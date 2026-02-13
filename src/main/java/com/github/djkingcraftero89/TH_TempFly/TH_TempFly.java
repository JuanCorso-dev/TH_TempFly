package com.github.djkingcraftero89.TH_TempFly;

import com.github.djkingcraftero89.TH_TempFly.command.FlyCommand;
import com.github.djkingcraftero89.TH_TempFly.command.TempFlyCommand;
import com.github.djkingcraftero89.TH_TempFly.fly.FlyManager;
import com.github.djkingcraftero89.TH_TempFly.integration.VulcanIntegration;
import com.github.djkingcraftero89.TH_TempFly.listener.PlayerListener;
import com.github.djkingcraftero89.TH_TempFly.placeholder.TempFlyPlaceholder;
import com.github.djkingcraftero89.TH_TempFly.redis.RedisService;
import com.github.djkingcraftero89.TH_TempFly.restriction.FlightRestrictionManager;
import com.github.djkingcraftero89.TH_TempFly.storage.DataStore;
import com.github.djkingcraftero89.TH_TempFly.storage.SQLDataStore;
import com.github.djkingcraftero89.TH_TempFly.util.MessageManager;
import com.github.djkingcraftero89.TH_TempFly.util.UpdateChecker;
import com.zaxxer.hikari.HikariDataSource;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.UUID;

public class TH_TempFly extends JavaPlugin {
	private DataStore dataStore;
	private HikariDataSource dataSource;
	private FlyManager flyManager;
	private RedisService redisService;
	private VulcanIntegration vulcanIntegration;
	private MessageManager messageManager;
	private FlightRestrictionManager restrictionManager;
	private UpdateChecker updateChecker;

	@Override
	public void onEnable() {
		getLogger().info("Starting TH_TempFly plugin...");
		getLogger().info("Dependencies are loaded automatically by Paper's library loader");

		saveDefaultConfig();
		FileConfiguration cfg = getConfig();
		getLogger().info("Configuration loaded successfully");

		// Initialize bStats metrics
		int pluginId = 27511;
		Metrics metrics = new Metrics(this, pluginId);
		getLogger().info("bStats metrics initialized");

		// Initialize MessageManager
		this.messageManager = new MessageManager(this);

		// Initialize database
		boolean isMysql = "MYSQL".equalsIgnoreCase(cfg.getString("storage.type", "SQLITE"));
		getLogger().info("Database type: " + (isMysql ? "MySQL" : "SQLite"));
		this.dataSource = createDataSourceFromConfig(isMysql);

		// Load retry configuration
		int maxRetries = cfg.getInt("storage.retry.max-retries", 3);
		long initialRetryDelay = cfg.getLong("storage.retry.initial-delay-ms", 100L);
		double retryBackoffMultiplier = cfg.getDouble("storage.retry.backoff-multiplier", 2.0);

		this.dataStore = new SQLDataStore(this.dataSource, isMysql, maxRetries, initialRetryDelay,
				retryBackoffMultiplier, getLogger());

		try {
			getLogger().info("Initializing database...");
			this.dataStore.initialize();
			getLogger().info("Database initialized successfully");
		} catch (Exception e) {
			getLogger().severe("Error initializing database: " + e.getMessage());
			e.printStackTrace();
			getServer().getPluginManager().disablePlugin(this);
			return;
		}

		// Load fly system configuration
		int tickInterval = cfg.getInt("fly.tick-interval-ticks", 20);
		long saveInterval = cfg.getLong("fly.save-interval-seconds", 60L);
		boolean freezeTimeWhenOffline = cfg.getBoolean("fly.freeze-time-when-offline", true);
		boolean titlesEnabled = cfg.getBoolean("fly.titles.enabled", true);
		long warningThreshold = cfg.getLong("fly.titles.warning-threshold", 5L);
		int titleUpdateInterval = cfg.getInt("fly.titles.update-interval", 20);

		getLogger().info("Freeze time when offline: " + freezeTimeWhenOffline);
		getLogger().info("Title system enabled: " + titlesEnabled + " (threshold: " + warningThreshold + "s)");

		// Initialize flight restrictions
		boolean restrictionsEnabled = cfg.getBoolean("fly.restrictions.enabled", true);
		List<String> blockedWorlds = cfg.getStringList("fly.restrictions.blocked-worlds");
		List<String> blockedRegions = cfg.getStringList("fly.restrictions.blocked-regions");
		this.restrictionManager = new FlightRestrictionManager(this, blockedWorlds, blockedRegions,
				restrictionsEnabled);

		// Initialize Vulcan Anti-Cheat integration
		this.vulcanIntegration = new VulcanIntegration(this);

		// Initialize Redis if enabled
		boolean redisEnabled = cfg.getBoolean("redis.enabled", false);
		if (redisEnabled) {
			String host = cfg.getString("redis.credentials.host", "127.0.0.1");
			int port = cfg.getInt("redis.credentials.port", 6379);
			String username = cfg.getString("redis.credentials.username", "");
			String password = cfg.getString("redis.credentials.password", "");
			int database = cfg.getInt("redis.credentials.database", 0);
			String clientName = cfg.getString("redis.server-name", "TH-TempFly");
			String channel = cfg.getString("redis.sync.channel", "TH-TempFly:updates");

			try {
				this.redisService = new RedisService(true, host, port, username, password, database, clientName,
						channel);
				getLogger().info("Connected to Redis successfully (" + host + ":" + port + ")");

				this.redisService.subscribe((redisChannel, message) -> {
					try {
						if (message.startsWith("PLAYER_SYNC:")) {
							String[] parts = message.split(":");
							if (parts.length >= 3) {
								UUID playerId = UUID.fromString(parts[1]);
								long seconds = Long.parseLong(parts[2]);
								String senderServer = parts.length > 3 ? parts[3] : "";
								String currentServer = cfg.getString("redis.server-name", "TH-TempFly");

								if (!senderServer.equals(currentServer)) {
									long current = this.flyManager.getRemaining(playerId);

									if (TempFlyCommand.isDebugMode()) {
										getLogger().info("[DEBUG] Received Redis message: " + message);
										getLogger().info("[DEBUG] Current time for " + playerId + ": " + current
												+ ", received: " + seconds);
									}

									if (current != seconds) {
										this.flyManager.setRemainingNoSync(playerId, seconds, false);

										if (TempFlyCommand.isDebugMode()) {
											getLogger().info("[DEBUG] Applied Redis sync for " + playerId + ": "
													+ current + " -> " + seconds);
										}
									} else if (TempFlyCommand.isDebugMode()) {
										getLogger().info("[DEBUG] Skipped Redis sync for " + playerId + " (no change)");
									}

									if (cfg.getBoolean("redis.sync.log-received", false)) {
										getLogger().info("Received Redis sync for player " + playerId + " with "
												+ seconds + " seconds from server: " + senderServer);
									}
								} else if (TempFlyCommand.isDebugMode()) {
									getLogger().info("[DEBUG] Ignored Redis message from self (" + senderServer + ")");
								}
							}
						}
					} catch (Exception e) {
						getLogger().warning("Error processing Redis message: " + e.getMessage());
					}
				});
				getLogger().info("Redis synchronization setup complete");
			} catch (Throwable ex) {
				this.redisService = null;
				getLogger().warning("Could not connect to Redis at " + host + ":" + port
						+ ". Continuing without Redis. Reason: " + ex.getMessage());
			}
		}

		// Initialize FlyManager
		this.flyManager = new FlyManager(this, dataStore, messageManager, redisService, vulcanIntegration, tickInterval,
				saveInterval, freezeTimeWhenOffline, titlesEnabled, warningThreshold, titleUpdateInterval);
		this.flyManager.start();

		// Register commands
		getLogger().info("Registering commands...");
		TempFlyCommand tempFlyCommand = new TempFlyCommand(this, flyManager, dataStore, messageManager);
		getCommand("tempfly").setExecutor(tempFlyCommand);
		getCommand("tempfly").setTabCompleter(tempFlyCommand);
		getCommand("atempfly").setExecutor(tempFlyCommand);
		getCommand("atempfly").setTabCompleter(tempFlyCommand);
		FlyCommand flyCommand = new FlyCommand(flyManager, messageManager, restrictionManager, vulcanIntegration);
		PluginCommand flyCmd = getCommand("fly");
		if (flyCmd == null) {
			getLogger().severe("Command 'fly' is missing from plugin.yml");
		} else {
			flyCmd.setExecutor(flyCommand);
			flyCmd.setTabCompleter(flyCommand);

			List<String> configuredAliases = cfg.getStringList("fly.aliases");
			if (configuredAliases != null && !configuredAliases.isEmpty()) {
				LinkedHashSet<String> uniqueAliases = new LinkedHashSet<>();
				List<String> invalidAliases = new ArrayList<>();

				for (String a : configuredAliases) {
					if (a == null) continue;
					String alias = a.trim();
					if (alias.isEmpty()) continue;
					if (alias.contains(" ") || alias.contains("\t")) {
						invalidAliases.add(a);
						continue;
					}
					if ("fly".equalsIgnoreCase(alias)) continue;
					uniqueAliases.add(alias);
				}

				if (!invalidAliases.isEmpty()) {
					getLogger().warning("Ignored invalid fly aliases (spaces are not allowed): " + invalidAliases);
				}

				List<String> finalAliases = new ArrayList<>(uniqueAliases);
				flyCmd.setAliases(finalAliases);
				if (!finalAliases.isEmpty()) {
					getLogger().info("Registered /fly aliases from config: " + finalAliases);
				}
			}
		}
		getLogger().info("Commands registered successfully");

		// Register PlaceholderAPI expansion
		if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			new TempFlyPlaceholder(flyManager).register();
			getLogger().info("PlaceholderAPI expansion registered successfully!");
		} else {
			getLogger().info("PlaceholderAPI not found, placeholders will not be available.");
		}

		// Register listeners
		getLogger().info("Registering listeners...");
		boolean enableOnJoin = cfg.getBoolean("fly.enable-on-join-if-leftover", true);
		boolean disableOnQuit = cfg.getBoolean("fly.disable-on-quit", true);
		boolean notifyAdmins = cfg.getBoolean("update-checker.notify-admins", true);
		getServer().getPluginManager().registerEvents(new PlayerListener(this, flyManager, restrictionManager,
				messageManager, enableOnJoin, disableOnQuit, notifyAdmins), this);
		getLogger().info("Listeners registered successfully");

		// Check for updates if enabled
		if (cfg.getBoolean("update-checker.enabled", true)) {
			this.updateChecker = new UpdateChecker(this, "JuanCorso-dev", "TH_TempFly");
			updateChecker.checkForUpdates().thenAccept(updateAvailable -> {
				if (updateAvailable) {
					updateChecker.notifyConsole(messageManager);
				}
			});
		}

		getLogger().info("TH_TempFly plugin enabled successfully!");
	}

	@Override
	public void onDisable() {
		if (flyManager != null)
			flyManager.stop();
		if (redisService != null)
			redisService.close();
		if (dataStore != null)
			dataStore.close();
		if (dataSource != null)
			dataSource.close();
	}

	private HikariDataSource createDataSourceFromConfig(boolean mysql) {
		FileConfiguration cfg = getConfig();
		String poolName = cfg.getString("storage.hikari.poolName", "TH-TempFly");
		int maxPool = cfg.getInt("storage.hikari.maximumPoolSize", 10);
		int minIdle = cfg.getInt("storage.hikari.minimumIdle", 2);
		long connTimeout = cfg.getLong("storage.hikari.connectionTimeoutMs", 10000L);

		String jdbcUrl;
		String user = null;
		String pass = null;

		if (mysql) {
			String host = cfg.getString("storage.mysql.host", "localhost");
			int port = cfg.getInt("storage.mysql.port", 3306);
			String db = cfg.getString("storage.mysql.database", "tempfly");
			String params = cfg.getString("storage.mysql.params",
					"useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
			jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + db + (params.isEmpty() ? "" : ("?" + params));
			user = cfg.getString("storage.mysql.username", "root");
			pass = cfg.getString("storage.mysql.password", "password");
		} else {
			String file = cfg.getString("storage.sqlite.file", "data.db");
			File dbFile = new File(getDataFolder(), file);
			jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
			getDataFolder().mkdirs();
		}

		return SQLDataStore.createDataSource(jdbcUrl, user, pass, poolName, maxPool, minIdle, connTimeout);
	}

	public FlightRestrictionManager getRestrictionManager() {
		return restrictionManager;
	}

	public UpdateChecker getUpdateChecker() {
		return updateChecker;
	}

	public HikariDataSource getMySQLDataSource() {
		FileConfiguration cfg = getConfig();
		boolean isMysql = "MYSQL".equalsIgnoreCase(cfg.getString("storage.type", "SQLITE"));
		return isMysql ? this.dataSource : null;
	}
}
