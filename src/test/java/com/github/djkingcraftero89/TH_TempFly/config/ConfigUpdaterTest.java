package com.github.djkingcraftero89.TH_TempFly.config;

import com.github.djkingcraftero89.TH_TempFly.config.ConfigUpdater.Decision;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigUpdaterTest {
	private static final String DEFAULTS = """
			# Internal file version used for automatic updates. Do not change it.
			config-version: 2

			# Fly settings
			fly:
			  # Seconds between checks
			  interval: 20
			  # Added in version 2
			  mode: FLYING
			  aliases: []
			""";

	private static final NavigableMap<Integer, ConfigMigration> NO_MIGRATIONS = new TreeMap<>();

	@Test
	void keepsUserValues() {
		YamlConfiguration result = upgrade(yaml("""
				config-version: 1
				fly:
				  interval: 40
				  aliases: [f, volar]
				"""));

		assertEquals(40, result.getInt("fly.interval"));
		assertEquals(List.of("f", "volar"), result.getStringList("fly.aliases"));
	}

	@Test
	void addsMissingDefaultKeysWithTheirComments() {
		YamlConfiguration result = upgrade(yaml("""
				config-version: 1
				fly:
				  interval: 40
				  aliases: []
				"""));

		assertEquals("FLYING", result.getString("fly.mode"));
		assertEquals(List.of("Added in version 2"), result.getComments("fly.mode"));
		assertTrue(result.saveToString().contains("  # Added in version 2\n  mode: FLYING\n"));
	}

	@Test
	void sharedKeysTakeTheBundledComments() {
		YamlConfiguration result = upgrade(yaml("""
				fly:
				  # A comment the user wrote
				  interval: 40
				"""));

		assertEquals(List.of("Seconds between checks"), result.getComments("fly.interval"));
	}

	@Test
	void keepsUserOnlyKeysWithTheirComments() {
		YamlConfiguration result = upgrade(yaml("""
				config-version: 1
				fly:
				  interval: 20
				  # My own toggle
				  custom-flag: true # inline note
				# Section the plugin does not know
				extra:
				  # Nested comment
				  value: 7
				"""));

		assertTrue(result.getBoolean("fly.custom-flag"));
		assertEquals(List.of("My own toggle"), result.getComments("fly.custom-flag"));
		assertEquals(List.of("inline note"), result.getInlineComments("fly.custom-flag"));
		assertEquals(7, result.getInt("extra.value"));
		assertEquals(List.of("Section the plugin does not know"), result.getComments("extra"));
		assertEquals(List.of("Nested comment"), result.getComments("extra.value"));
	}

	@Test
	void setsTheCurrentVersionAsTheFirstKey() {
		YamlConfiguration result = upgrade(yaml("""
				fly:
				  interval: 40
				"""));

		assertEquals(2, result.getInt(ConfigUpdater.VERSION_KEY));
		assertEquals(ConfigUpdater.VERSION_KEY, result.getKeys(false).iterator().next());
	}

	@Test
	void upgradedFileIsUpToDateAndStable() {
		String once = upgrade(yaml("fly:\n  interval: 40\n")).saveToString();
		String twice = upgrade(yaml(once)).saveToString();

		assertEquals(Decision.UP_TO_DATE, ConfigUpdater.decide(yaml(DEFAULTS), yaml(once)));
		assertEquals(once, twice);
	}

	@Test
	void bundledSectionWinsOverAUserPlainValue() {
		YamlConfiguration result = upgrade(yaml("""
				config-version: 1
				fly: true
				"""));

		assertTrue(result.isConfigurationSection("fly"));
		assertEquals(20, result.getInt("fly.interval"));
		assertEquals(Decision.UP_TO_DATE, ConfigUpdater.decide(yaml(DEFAULTS), yaml(result.saveToString())));
	}

	@Test
	void bundledPlainValueWinsOverAUserSection() {
		YamlConfiguration result = upgrade(yaml("""
				config-version: 1
				fly:
				  aliases:
				    nested:
				      value: 1
				"""));

		assertTrue(result.isList("fly.aliases"));
		assertFalse(result.contains("fly.aliases.nested.value"));
	}

	@Test
	void longStringsStayOnOneLine() {
		String longMessage = "&aThis message is long enough to be folded by the default width ".repeat(3).trim();
		YamlConfiguration result = upgrade(yaml("custom-message: \"" + longMessage + "\"\n"));

		assertTrue(result.saveToString().lines().anyMatch(line -> line.contains(longMessage)));
	}

	@Test
	void runsMigrationsInOrderOnlyAboveTheStoredVersion() {
		List<Integer> ran = new ArrayList<>();
		NavigableMap<Integer, ConfigMigration> migrations = new TreeMap<>();
		for (int version : new int[] {4, 2, 1, 3}) {
			migrations.put(version, config -> ran.add(version));
		}

		ConfigUpdater.applyMigrations(new YamlConfiguration(), 1, 3, migrations);

		assertEquals(List.of(2, 3), ran);
	}

	@Test
	void runsNoMigrationWhenAlreadyAtTheCurrentVersion() {
		List<Integer> ran = new ArrayList<>();
		NavigableMap<Integer, ConfigMigration> migrations = new TreeMap<>();
		migrations.put(1, config -> ran.add(1));

		ConfigUpdater.applyMigrations(new YamlConfiguration(), 1, 1, migrations);
		ConfigUpdater.applyMigrations(new YamlConfiguration(), 2, 1, migrations);

		assertTrue(ran.isEmpty());
	}

	@Test
	void migrationRunsBeforeTheMergeSoARenamedKeyKeepsTheUserValue() {
		NavigableMap<Integer, ConfigMigration> migrations = new TreeMap<>();
		migrations.put(2, config -> {
			config.set("fly.interval", config.get("fly.tick-interval"));
			config.set("fly.tick-interval", null);
		});

		YamlConfiguration result = ConfigUpdater.upgrade(yaml(DEFAULTS), yaml("""
				config-version: 1
				fly:
				  tick-interval: 40
				"""), migrations);

		assertEquals(40, result.getInt("fly.interval"));
		assertFalse(result.contains("fly.tick-interval"));
	}

	@Test
	void rejectsInvalidOrDuplicateMigrationVersions() {
		ConfigUpdater updater = new ConfigUpdater(new File("."), "config.yml", () -> null, Logger.getAnonymousLogger());
		updater.addMigration(2, config -> { });

		assertThrows(IllegalArgumentException.class, () -> updater.addMigration(2, config -> { }));
		assertThrows(IllegalArgumentException.class, () -> updater.addMigration(0, config -> { }));
	}

	@Test
	void upToDateFileNeedsNoUpdate() {
		YamlConfiguration user = yaml("""
				config-version: 2
				fly:
				  interval: 99
				  mode: ONLINE
				  aliases: [f]
				  custom-flag: true
				""");

		assertEquals(Decision.UP_TO_DATE, ConfigUpdater.decide(yaml(DEFAULTS), user));
	}

	@Test
	void missingKeyTriggersAnUpdateAtTheCurrentVersion() {
		YamlConfiguration user = yaml("""
				config-version: 2
				fly:
				  interval: 20
				  aliases: []
				""");

		assertEquals(Decision.UPDATE, ConfigUpdater.decide(yaml(DEFAULTS), user));
	}

	@Test
	void olderVersionTriggersAnUpdateEvenWithEveryKey() {
		YamlConfiguration user = yaml(DEFAULTS.replace("config-version: 2", "config-version: 1"));

		assertEquals(Decision.UPDATE, ConfigUpdater.decide(yaml(DEFAULTS), user));
	}

	@Test
	void newerStoredVersionIsLeftUntouched() {
		YamlConfiguration user = yaml("""
				config-version: 3
				fly:
				  interval: 20
				""");

		assertEquals(Decision.NEWER_VERSION, ConfigUpdater.decide(yaml(DEFAULTS), user));
	}

	@Test
	void fileWithoutTheVersionKeyCountsAsZero() {
		YamlConfiguration user = yaml(DEFAULTS.replace("config-version: 2\n", ""));

		assertEquals(0, ConfigUpdater.readVersion(user));
		assertEquals(Decision.UPDATE, ConfigUpdater.decide(yaml(DEFAULTS), user));
	}

	private static YamlConfiguration upgrade(YamlConfiguration user) {
		return ConfigUpdater.upgrade(yaml(DEFAULTS), user, NO_MIGRATIONS);
	}

	static YamlConfiguration yaml(String text) {
		YamlConfiguration config = new YamlConfiguration();
		try {
			config.loadFromString(text);
		} catch (InvalidConfigurationException e) {
			throw new IllegalArgumentException("Test YAML is invalid", e);
		}
		return config;
	}
}
