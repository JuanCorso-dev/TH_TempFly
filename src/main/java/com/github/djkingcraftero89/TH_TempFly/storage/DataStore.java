package com.github.djkingcraftero89.TH_TempFly.storage;

import java.util.Optional;
import java.util.UUID;

public interface DataStore {
	void initialize() throws Exception;

	Optional<Long> getRemainingSeconds(UUID playerId) throws Exception;

	void setRemainingSeconds(UUID playerId, long seconds) throws Exception;

	void close();
}
