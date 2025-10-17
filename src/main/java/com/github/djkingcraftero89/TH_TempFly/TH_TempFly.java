package com.github.djkingcraftero89.TH_TempFly;

import com.github.djkingcraftero89.TH_TempFly.command.FlyCommand;
import com.github.djkingcraftero89.TH_TempFly.command.TempFlyCommand;
import com.github.djkingcraftero89.TH_TempFly.fly.FlyManager;
import com.github.djkingcraftero89.TH_TempFly.listener.PlayerListener;
import com.github.djkingcraftero89.TH_TempFly.placeholder.TempFlyPlaceholder;
import com.github.djkingcraftero89.TH_TempFly.redis.RedisService;
import com.github.djkingcraftero89.TH_TempFly.storage.DataStore;
import com.github.djkingcraftero89.TH_TempFly.storage.SQLDataStore;
import com.github.djkingcraftero89.TH_TempFly.util.MessageManager;
import com.zaxxer.hikari.HikariDataSource;
import org.bstats.bukkit.Metrics;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.UUID;

public class TH_TempFly extends JavaPlugin {
	private DataStore dataStore;
	private FlyManager flyManager;
	private RedisService redisService;
	private MessageManager messageManager;

	@Override
	public void onEnable() {
		getLogger().info("Starting TH_TempFly plugin...");
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
		HikariDataSource ds = createDataSourceFromConfig(isMysql);
		this.dataStore = new SQLDataStore(ds, isMysql);
		
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
				this.redisService = new RedisService(true, host, port, username, password, database, clientName, channel);
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
										getLogger().info("[DEBUG] Current time for " + playerId + ": " + current + ", received: " + seconds);
									}
									
									if (current != seconds) {
										this.flyManager.setRemainingNoSync(playerId, seconds, false);
										
										if (TempFlyCommand.isDebugMode()) {
											getLogger().info("[DEBUG] Applied Redis sync for " + playerId + ": " + current + " -> " + seconds);
										}
									} else if (TempFlyCommand.isDebugMode()) {
										getLogger().info("[DEBUG] Skipped Redis sync for " + playerId + " (no change)");
									}
									
									if (cfg.getBoolean("redis.sync.log-received", false)) {
										getLogger().info("Received Redis sync for player " + playerId + " with " + seconds + " seconds from server: " + senderServer);
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
				getLogger().warning("Could not connect to Redis at " + host + ":" + port + ". Continuing without Redis. Reason: " + ex.getMessage());
			}
		}

		// Initialize FlyManager
		this.flyManager = new FlyManager(this, dataStore, messageManager, redisService, tickInterval, saveInterval, freezeTimeWhenOffline, titlesEnabled, warningThreshold, titleUpdateInterval);
		this.flyManager.start();

		// Register commands
		getLogger().info("Registering commands...");
		TempFlyCommand tempFlyCommand = new TempFlyCommand(this, flyManager, dataStore, messageManager);
		getCommand("tempfly").setExecutor(tempFlyCommand);
		getCommand("tempfly").setTabCompleter(tempFlyCommand);
		getCommand("atempfly").setExecutor(tempFlyCommand);
		getCommand("atempfly").setTabCompleter(tempFlyCommand);
		getCommand("fly").setExecutor(new FlyCommand(flyManager, messageManager));
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
		getServer().getPluginManager().registerEvents(new PlayerListener(flyManager, enableOnJoin, disableOnQuit), this);
		getLogger().info("Listeners registered successfully");
		
		getLogger().info("TH_TempFly plugin enabled successfully!");
	}

	@Override
	public void onDisable() {
		if (flyManager != null) flyManager.stop();
		if (redisService != null) redisService.close();
		if (dataStore != null) dataStore.close();
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
			String params = cfg.getString("storage.mysql.params", "useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
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
}
