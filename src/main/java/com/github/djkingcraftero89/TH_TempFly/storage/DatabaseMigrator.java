package com.github.djkingcraftero89.TH_TempFly.storage;

import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class DatabaseMigrator {
	private final JavaPlugin plugin;
	private final Logger logger;
	
	public DatabaseMigrator(JavaPlugin plugin) {
		this.plugin = plugin;
		this.logger = plugin.getLogger();
	}
	
	/**
	 * Migrates data from SQLite to MySQL
	 * @param mysqlDataSource The MySQL data source to write to
	 * @return MigrationResult with details about the migration
	 * @throws Exception if migration fails
	 */
	public MigrationResult migrateFromSQLiteToMySQL(HikariDataSource mysqlDataSource) throws Exception {
		FileConfiguration cfg = plugin.getConfig();
		String sqliteFile = cfg.getString("storage.sqlite.file", "data.db");
		File dbFile = new File(plugin.getDataFolder(), sqliteFile);
		
		if (!dbFile.exists()) {
			throw new Exception("SQLite database file not found: " + dbFile.getAbsolutePath());
		}
		
		String sqliteUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
		HikariDataSource sqliteDataSource = SQLDataStore.createDataSource(
			sqliteUrl, null, null, "TH-TempFly-Migration-SQLite", 1, 1, 10000L
		);
		
		try {
			// Read all data from SQLite
			Map<UUID, Long> dataToMigrate = new HashMap<>();
			try (Connection sqliteConn = sqliteDataSource.getConnection();
				 Statement stmt = sqliteConn.createStatement();
				 ResultSet rs = stmt.executeQuery("SELECT player_uuid, remaining_seconds FROM tempfly")) {
				
				while (rs.next()) {
					String uuidStr = rs.getString("player_uuid");
					long seconds = rs.getLong("remaining_seconds");
					
					try {
						UUID uuid = UUID.fromString(uuidStr);
						dataToMigrate.put(uuid, seconds);
					} catch (IllegalArgumentException e) {
						logger.warning("Invalid UUID in SQLite database: " + uuidStr + " - skipping");
					}
				}
			}
			
			if (dataToMigrate.isEmpty()) {
				return new MigrationResult(0, 0, 0, "No data found in SQLite database");
			}
			
			// Write to MySQL
			int migrated = 0;
			int skipped = 0;
			int batchSize = cfg.getInt("storage.hikari.batch-size", 50);
			
			String upsert = "INSERT INTO tempfly(player_uuid, remaining_seconds) VALUES(?, ?) " +
					"ON DUPLICATE KEY UPDATE remaining_seconds = VALUES(remaining_seconds)";
			
			try (Connection mysqlConn = mysqlDataSource.getConnection();
				 PreparedStatement ps = mysqlConn.prepareStatement(upsert)) {
				
				int count = 0;
				for (Map.Entry<UUID, Long> entry : dataToMigrate.entrySet()) {
					ps.setString(1, entry.getKey().toString());
					ps.setLong(2, Math.max(0L, entry.getValue()));
					ps.addBatch();
					count++;
					
					if (count % batchSize == 0) {
						int[] results = ps.executeBatch();
						for (int result : results) {
							if (result > 0) {
								migrated++;
							} else {
								skipped++;
							}
						}
						ps.clearBatch();
					}
				}
				
				// Execute remaining records
				if (count % batchSize != 0) {
					int[] results = ps.executeBatch();
					for (int result : results) {
						if (result > 0) {
							migrated++;
						} else {
							skipped++;
						}
					}
				}
			}
			
			return new MigrationResult(dataToMigrate.size(), migrated, skipped, null);
			
		} finally {
			sqliteDataSource.close();
		}
	}
	
	public static class MigrationResult {
		private final int totalRecords;
		private final int migrated;
		private final int skipped;
		private final String error;
		
		public MigrationResult(int totalRecords, int migrated, int skipped, String error) {
			this.totalRecords = totalRecords;
			this.migrated = migrated;
			this.skipped = skipped;
			this.error = error;
		}
		
		public int getTotalRecords() {
			return totalRecords;
		}
		
		public int getMigrated() {
			return migrated;
		}
		
		public int getSkipped() {
			return skipped;
		}
		
		public String getError() {
			return error;
		}
		
		public boolean hasError() {
			return error != null;
		}
	}
}

