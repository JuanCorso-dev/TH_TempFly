package com.github.djkingcraftero89.TH_TempFly.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Brings a YAML file in the plugin's data folder up to date with the copy bundled in the jar.
 *
 * <p>Both copies carry an integer {@value #VERSION_KEY} key. The bundled copy's value is the
 * current version; a file on disk without the key counts as version 0. The file on disk is
 * rewritten when its version is lower than the current one, or when any key of the bundled
 * copy is missing from it. The rewrite:
 * <ol>
 *   <li>copies the original to {@code <name>.v<oldVersion>-<yyyyMMdd-HHmmss>.bak};</li>
 *   <li>runs every registered {@link ConfigMigration} above the stored version, in order;</li>
 *   <li>rebuilds the file from the bundled copy, so key order and comments follow the shipped
 *       file, then applies every user value on top of it. Keys that only exist in the user's
 *       file are kept together with their comments;</li>
 *   <li>sets {@value #VERSION_KEY} to the current version and replaces the file atomically.</li>
 * </ol>
 *
 * <p>Comments the user wrote above keys that also exist in the bundled copy are replaced by the
 * bundled comments; the backup keeps the original text. A file with a version newer than the
 * bundled one, or one that is not valid YAML, is never touched.
 *
 * <p>See {@link ConfigMigration} for how to handle renamed or moved keys.
 */
public final class ConfigUpdater {
	/** Key holding the file version, the first key of every versioned resource. */
	public static final String VERSION_KEY = "config-version";

	private static final DateTimeFormatter BACKUP_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

	/** Outcome of comparing a file on disk with the bundled copy. */
	enum Decision {
		UP_TO_DATE, UPDATE, NEWER_VERSION
	}

	private final File dataFolder;
	private final String resourceName;
	private final Supplier<InputStream> bundledResource;
	private final Logger logger;
	private final NavigableMap<Integer, ConfigMigration> migrations = new TreeMap<>();

	/**
	 * @param plugin the plugin owning the file
	 * @param resourceName the file name, identical in the jar and in the data folder (e.g. {@code config.yml})
	 */
	public ConfigUpdater(Plugin plugin, String resourceName) {
		this(plugin.getDataFolder(), resourceName, () -> plugin.getResource(resourceName), plugin.getLogger());
	}

	ConfigUpdater(File dataFolder, String resourceName, Supplier<InputStream> bundledResource, Logger logger) {
		this.dataFolder = dataFolder;
		this.resourceName = resourceName;
		this.bundledResource = bundledResource;
		this.logger = logger;
	}

	/**
	 * Registers the step that upgrades a file from {@code targetVersion - 1} to {@code targetVersion}.
	 *
	 * @param targetVersion the version the step produces, at least 1
	 * @param migration the step
	 * @return this updater, for chaining
	 * @throws IllegalArgumentException if the version is below 1 or already has a step
	 */
	public ConfigUpdater addMigration(int targetVersion, ConfigMigration migration) {
		Objects.requireNonNull(migration, "migration");
		if (targetVersion < 1) {
			throw new IllegalArgumentException("Migration version must be at least 1: " + targetVersion);
		}
		if (migrations.putIfAbsent(targetVersion, migration) != null) {
			throw new IllegalArgumentException("A migration for version " + targetVersion + " is already registered");
		}
		return this;
	}

	/**
	 * Updates the file on disk if it is outdated. Does nothing when the file does not exist yet.
	 * Problems are logged and leave the file unchanged.
	 *
	 * @return {@code true} if the file was rewritten
	 */
	public boolean update() {
		File file = new File(dataFolder, resourceName);
		if (!file.isFile()) {
			return false;
		}

		YamlConfiguration template;
		try {
			template = loadBundled();
		} catch (IOException | InvalidConfigurationException e) {
			logger.log(Level.WARNING, "Could not read the bundled " + resourceName + ", skipping its automatic update", e);
			return false;
		}

		// loadConfiguration(File) would swallow a parse error and return an empty config, which
		// looks like a file missing every key and would overwrite the user's work.
		YamlConfiguration user = new YamlConfiguration();
		try {
			user.load(file);
		} catch (IOException | InvalidConfigurationException e) {
			logger.warning(resourceName + " could not be read as YAML, so it was not updated automatically."
					+ " Fix it or delete it to regenerate it. Reason: " + e.getMessage());
			return false;
		}

		int currentVersion = readVersion(template);
		int storedVersion = readVersion(user);
		Decision decision = decide(template, user);
		if (decision == Decision.NEWER_VERSION) {
			logger.warning(resourceName + " has " + VERSION_KEY + " " + storedVersion + ", newer than the "
					+ currentVersion + " this plugin version ships. It was left untouched.");
			return false;
		}
		if (decision == Decision.UP_TO_DATE) {
			return false;
		}

		Path path = file.toPath();
		try {
			Path backup = backup(path, storedVersion);
			String content = upgrade(template, user, migrations).saveToString();
			writeAtomically(path, content);
			logger.info("Updated " + resourceName + " from version " + storedVersion + " to " + currentVersion
					+ " (backup: " + backup.getFileName() + ")");
			return true;
		} catch (IOException | RuntimeException e) {
			logger.log(Level.WARNING, "Could not update " + resourceName + ", the file was left unchanged", e);
			return false;
		}
	}

	/** The file version stored in {@code config}, or 0 when the key is missing or not a number. */
	static int readVersion(ConfigurationSection config) {
		return config.getInt(VERSION_KEY, 0);
	}

	/** Compares the user's file with the bundled defaults. */
	static Decision decide(ConfigurationSection defaults, ConfigurationSection user) {
		int currentVersion = readVersion(defaults);
		int storedVersion = readVersion(user);
		if (storedVersion > currentVersion) {
			return Decision.NEWER_VERSION;
		}
		if (storedVersion < currentVersion || hasMissingKeys(defaults, user)) {
			return Decision.UPDATE;
		}
		return Decision.UP_TO_DATE;
	}

	/** Runs, in ascending order, the migrations for every version in {@code (fromVersion, toVersion]}. */
	static void applyMigrations(ConfigurationSection config, int fromVersion, int toVersion,
			NavigableMap<Integer, ConfigMigration> migrations) {
		if (fromVersion >= toVersion) {
			return;
		}
		for (ConfigMigration migration : migrations.subMap(fromVersion, false, toVersion, true).values()) {
			migration.migrate(config);
		}
	}

	/**
	 * Migrates {@code user} and merges it into {@code template}, which must be a fresh load of the
	 * bundled file. Both arguments are modified; the returned template is ready to be saved.
	 */
	static YamlConfiguration upgrade(YamlConfiguration template, YamlConfiguration user,
			NavigableMap<Integer, ConfigMigration> migrations) {
		int currentVersion = readVersion(template);
		applyMigrations(user, readVersion(user), currentVersion, migrations);
		overlay(template, user);
		template.set(VERSION_KEY, currentVersion);
		// The default width of 80 folds long strings over several lines.
		template.options().width(Integer.MAX_VALUE);
		return template;
	}

	private static boolean hasMissingKeys(ConfigurationSection defaults, ConfigurationSection user) {
		for (String path : defaults.getKeys(true)) {
			if (!defaults.isConfigurationSection(path) && !user.contains(path, true)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Copies every user value into the template. Keys only present in the user's file are added
	 * with their comments; keys present in both keep the template's comments.
	 */
	private static void overlay(YamlConfiguration template, YamlConfiguration user) {
		// getKeys(true) lists a section before its children, so parents exist before values land in them.
		for (String path : user.getKeys(true)) {
			if (changesTemplateShape(template, user, path)) {
				continue;
			}
			boolean userOnly = !template.contains(path, true);
			if (user.isConfigurationSection(path)) {
				if (userOnly) {
					template.createSection(path);
				}
			} else {
				template.set(path, user.get(path));
			}
			if (userOnly) {
				template.setComments(path, user.getComments(path));
				template.setInlineComments(path, user.getInlineComments(path));
			}
		}
	}

	/**
	 * Whether applying the user's entry at {@code path} would turn a template section into a plain
	 * value or the other way round. The template wins such conflicts; otherwise every later start
	 * would find the template's keys missing again and rewrite the file. A {@link ConfigMigration}
	 * is the way to carry such a value over.
	 */
	private static boolean changesTemplateShape(ConfigurationSection template, ConfigurationSection user, String path) {
		if (template.contains(path, true) && template.isConfigurationSection(path) != user.isConfigurationSection(path)) {
			return true;
		}
		for (int dot = path.indexOf('.'); dot != -1; dot = path.indexOf('.', dot + 1)) {
			String ancestor = path.substring(0, dot);
			if (template.contains(ancestor, true) && !template.isConfigurationSection(ancestor)) {
				return true;
			}
		}
		return false;
	}

	private YamlConfiguration loadBundled() throws IOException, InvalidConfigurationException {
		try (InputStream in = bundledResource.get()) {
			if (in == null) {
				throw new FileNotFoundException(resourceName + " is missing from the plugin jar");
			}
			YamlConfiguration config = new YamlConfiguration();
			config.load(new InputStreamReader(in, StandardCharsets.UTF_8));
			return config;
		}
	}

	private static Path backup(Path file, int version) throws IOException {
		String name = file.getFileName() + ".v" + version + "-" + LocalDateTime.now().format(BACKUP_TIMESTAMP) + ".bak";
		return Files.copy(file, file.resolveSibling(name));
	}

	private static void writeAtomically(Path file, String content) throws IOException {
		Path temp = file.resolveSibling(file.getFileName() + ".tmp");
		try {
			Files.writeString(temp, content, StandardCharsets.UTF_8);
			try {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temp);
		}
	}
}
