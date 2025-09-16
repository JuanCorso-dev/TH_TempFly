package com.github.djkingcraftero89.TH_TempFly.storage;

import java.util.Optional;
import java.util.UUID;

public interface DataStore {
	void initialize() throws Exception;

	Optional<Long> getRemainingSeconds(UUID playerId) throws Exception;

	void setRemainingSeconds(UUID playerId, long seconds) throws Exception;

	default void addSeconds(UUID playerId, long deltaSeconds) throws Exception {
		long current = getRemainingSeconds(playerId).orElse(0L);
		setRemainingSeconds(playerId, Math.max(0L, current + deltaSeconds));
	}

	default void removeSeconds(UUID playerId, long deltaSeconds) throws Exception {
		addSeconds(playerId, -deltaSeconds);
	}

	void close();
}
