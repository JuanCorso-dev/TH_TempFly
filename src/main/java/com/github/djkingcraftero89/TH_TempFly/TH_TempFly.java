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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

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
		
		// Initialize MessageManager
		this.messageManager = new MessageManager(this);

		// Setup SQL
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

		int tickInterval = cfg.getInt("fly.tick-interval-ticks", 20);
		long saveInterval = cfg.getLong("fly.save-interval-seconds", 60L);
		this.flyManager = new FlyManager(this, dataStore, tickInterval, saveInterval);
		this.flyManager.start();

		boolean redisEnabled = cfg.getBoolean("redis.enabled", false);
		if (redisEnabled) {
			this.redisService = new RedisService(
				true,
				cfg.getString("redis.host", "127.0.0.1"),
				cfg.getInt("redis.port", 6379),
				cfg.getString("redis.username", ""),
				cfg.getString("redis.password", ""),
				cfg.getInt("redis.database", 0),
				cfg.getString("redis.clientName", "TH-TempFly"),
				cfg.getString("redis.channel", "TH-TempFly:updates")
			);
		}

		// Register commands
		getLogger().info("Registering commands...");
		getCommand("tempfly").setExecutor(new TempFlyCommand(this, flyManager, dataStore, messageManager));
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
			// ensure data folder exists
			getDataFolder().mkdirs();
		}
		return SQLDataStore.createDataSource(jdbcUrl, user, pass, poolName, maxPool, minIdle, connTimeout);
	}
}
