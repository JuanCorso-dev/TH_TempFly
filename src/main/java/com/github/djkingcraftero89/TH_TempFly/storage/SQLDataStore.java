package com.github.djkingcraftero89.TH_TempFly.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

public class SQLDataStore implements DataStore {
	private final HikariDataSource dataSource;
	private final boolean mysql;

	public SQLDataStore(HikariDataSource dataSource, boolean mysql) {
		this.dataSource = dataSource;
		this.mysql = mysql;
	}

	@Override
	public void initialize() throws Exception {
		try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
			String create = "CREATE TABLE IF NOT EXISTS tempfly (" +
					"player_uuid VARCHAR(36) PRIMARY KEY," +
					"remaining_seconds BIGINT NOT NULL" +
					")" + (mysql ? " ENGINE=InnoDB DEFAULT CHARSET=utf8mb4" : "");
			st.executeUpdate(create);
		}
	}

	@Override
	public Optional<Long> getRemainingSeconds(UUID playerId) throws Exception {
		String sql = "SELECT remaining_seconds FROM tempfly WHERE player_uuid = ?";
		try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
			ps.setString(1, playerId.toString());
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return Optional.of(rs.getLong(1));
				}
			}
		}
		return Optional.empty();
	}

	@Override
	public void setRemainingSeconds(UUID playerId, long seconds) throws Exception {
		String upsert;
		if (mysql) {
			upsert = "INSERT INTO tempfly(player_uuid, remaining_seconds) VALUES(?, ?) " +
					"ON DUPLICATE KEY UPDATE remaining_seconds = VALUES(remaining_seconds)";
		} else {
			upsert = "INSERT INTO tempfly(player_uuid, remaining_seconds) VALUES(?, ?) " +
					"ON CONFLICT(player_uuid) DO UPDATE SET remaining_seconds = excluded.remaining_seconds";
		}
		try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(upsert)) {
			ps.setString(1, playerId.toString());
			ps.setLong(2, Math.max(0L, seconds));
			ps.executeUpdate();
		}
	}

	@Override
	public void close() {
		dataSource.close();
	}

	public static HikariDataSource createDataSource(String jdbcUrl, String username, String password, String poolName, int maxPoolSize, int minIdle, long connTimeoutMs) {
		HikariConfig config = new HikariConfig();
		config.setJdbcUrl(jdbcUrl);
		if (username != null && !username.isEmpty()) config.setUsername(username);
		if (password != null && !password.isEmpty()) config.setPassword(password);
		config.setPoolName(poolName);
		config.setMaximumPoolSize(maxPoolSize);
		config.setMinimumIdle(minIdle);
		config.setConnectionTimeout(connTimeoutMs);
		return new HikariDataSource(config);
	}
}
