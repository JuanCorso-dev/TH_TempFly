package com.github.djkingcraftero89.TH_TempFly.fly;

import com.github.djkingcraftero89.TH_TempFly.command.TempFlyCommand;
import com.github.djkingcraftero89.TH_TempFly.redis.RedisService;
import com.github.djkingcraftero89.TH_TempFly.storage.DataStore;
import com.github.djkingcraftero89.TH_TempFly.util.MessageManager;
import com.github.djkingcraftero89.TH_TempFly.util.TimeParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FlyManager {
	private final Plugin plugin;
	private final DataStore dataStore;
	private final MessageManager messageManager;
	private final RedisService redisService;
	private final Map<UUID, Long> playerToRemainingSeconds = new HashMap<>();
	private final int tickIntervalTicks;
	private final long saveIntervalSeconds;
	private final boolean freezeTimeWhenOffline;
	
	// Title system variables
	private final boolean titlesEnabled;
	private final long warningThreshold;
	private final int titleUpdateInterval;
	private final Map<UUID, Long> playerLastTitleTime = new HashMap<>();
	private final Map<UUID, Long> lastSyncedSeconds = new HashMap<>();
	private final boolean fullBroadcastEnabled;

	private int taskId = -1;
	private long lastSaveEpochSecond = 0L;

	public FlyManager(Plugin plugin, DataStore dataStore, MessageManager messageManager, RedisService redisService, int tickIntervalTicks, long saveIntervalSeconds, boolean freezeTimeWhenOffline, boolean titlesEnabled, long warningThreshold, int titleUpdateInterval) {
		this.plugin = plugin;
		this.dataStore = dataStore;
		this.messageManager = messageManager;
		this.redisService = redisService;
		this.tickIntervalTicks = tickIntervalTicks;
		this.saveIntervalSeconds = saveIntervalSeconds;
		this.freezeTimeWhenOffline = freezeTimeWhenOffline;
		this.titlesEnabled = titlesEnabled;
		this.warningThreshold = warningThreshold;
		this.titleUpdateInterval = titleUpdateInterval;
		this.fullBroadcastEnabled = plugin.getConfig().getBoolean("redis.sync.full-broadcast-enabled", false);
	}

	public void start() {
		if (taskId != -1) return;
		taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::tick, tickIntervalTicks, tickIntervalTicks);
	}

	public void stop() {
		if (taskId != -1) {
			Bukkit.getScheduler().cancelTask(taskId);
			taskId = -1;
		}
		saveAll();
	}

	private void tick() {
		if (freezeTimeWhenOffline) {
			// FROZEN MODE: Only deduct fly time from ONLINE players
			// Offline player time is FROZEN and preserved until they reconnect
			tickOnlinePlayersOnly();
		} else {
			// CONTINUOUS MODE: Deduct time from ALL players (online and offline)
			tickAllPlayers();
		}

		// Save data periodically
		long nowSec = System.currentTimeMillis() / 1000L;
		if (nowSec - lastSaveEpochSecond >= saveIntervalSeconds) {
			lastSaveEpochSecond = nowSec;
			saveAllAsync();
			
			// Optional: full broadcast (disabled by default)
			if (fullBroadcastEnabled) {
				syncAllPlayersToRedis();
			}
		}
	}

	private void tickOnlinePlayersOnly() {
		long secondsToDeduct = tickIntervalTicks / 20L;
		
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (p.hasPermission("thtempfly.fly.infinite") || !p.isFlying()) {
				continue;
			}
			
			UUID id = p.getUniqueId();
			long remaining = playerToRemainingSeconds.getOrDefault(id, 0L);
			
			if (remaining <= 0) {
				continue;
			}
			
			remaining -= secondsToDeduct;
			
			if (remaining <= 0) {
				remaining = 0;
				disableFlight(p);
			} else {
				showTitleWarningIfNeeded(p, remaining);
			}
			
			playerToRemainingSeconds.put(id, remaining);
		}
	}

	private void tickAllPlayers() {
		long secondsToDeduct = tickIntervalTicks / 20L;
		
		for (Map.Entry<UUID, Long> entry : playerToRemainingSeconds.entrySet()) {
			UUID id = entry.getKey();
			long remaining = entry.getValue();
			
			if (remaining <= 0) {
				continue;
			}
			
			Player p = Bukkit.getPlayer(id);
			
			if (p == null || !p.isFlying()) {
				continue;
			}
			
			if (p.hasPermission("thtempfly.fly.infinite")) {
				continue;
			}
			
			remaining -= secondsToDeduct;
			
			if (remaining <= 0) {
				remaining = 0;
				disableFlight(p);
			} else {
				showTitleWarningIfNeeded(p, remaining);
			}
			
			playerToRemainingSeconds.put(id, remaining);
		}
	}

	public void loadPlayer(Player p, boolean enableOnJoin) {
		UUID id = p.getUniqueId();
		if (TempFlyCommand.isDebugMode()) {
			plugin.getLogger().info("[DEBUG] Loading player " + p.getName() + " (" + id + ")");
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				Optional<Long> opt = dataStore.getRemainingSeconds(id);
				long sec = opt.orElse(0L);
				if (TempFlyCommand.isDebugMode()) {
					plugin.getLogger().info("[DEBUG] Database loaded for " + p.getName() + ": " + sec + " seconds");
				}
				Bukkit.getScheduler().runTask(plugin, () -> {
					playerToRemainingSeconds.put(id, sec);
					
					// Sync current data to Redis for other servers
					syncToRedis(id, sec);
					
					if (enableOnJoin && sec > 0) {
						p.setAllowFlight(true);
						p.setFlying(true);
						String formattedTime = TimeParser.formatSeconds(sec);
						
						// Use specific message based on time mode
						String messageKey = freezeTimeWhenOffline ? "events.join.welcome-back-frozen" : "events.join.welcome-back";
						p.sendMessage(messageManager.getMessage(messageKey, "time", formattedTime));
						
						// Show information about the time system
						String timeSystemKey = freezeTimeWhenOffline ? "events.time-system.frozen-mode" : "events.time-system.continuous-mode";
						p.sendMessage(messageManager.getMessage(timeSystemKey));
					}
				});
			} catch (Exception e) {
				if (TempFlyCommand.isDebugMode()) {
					plugin.getLogger().severe("[DEBUG] Error loading player " + p.getName() + ": " + e.getMessage());
				}
				e.printStackTrace();
			}
		});
	}

	public void unloadPlayer(Player p, boolean disableOnQuit) {
		UUID id = p.getUniqueId();
		long remainingTime = getRemaining(id);
		
		if (TempFlyCommand.isDebugMode()) {
			plugin.getLogger().info("[DEBUG] Unloading player " + p.getName() + " (" + id + ") with " + remainingTime + " seconds");
		}
		
		if (disableOnQuit) {
			p.setAllowFlight(false);
			p.setFlying(false);
			p.sendMessage(messageManager.getMessage("events.quit.fly-disabled"));
		}
		
		// Inform about time system if they have remaining time
		if (remainingTime > 0 && freezeTimeWhenOffline) {
			p.sendMessage(messageManager.getMessage("events.quit.time-frozen"));
		}
		
		// Sync final data to Redis before player leaves
		syncToRedis(id, remainingTime);
		
		// Save with the actual remaining time before removing from memory
		savePlayerAsync(id, remainingTime);
		playerToRemainingSeconds.remove(id);
		playerLastTitleTime.remove(id); // Clean up title data
		lastSyncedSeconds.remove(id);
	}

	public long getRemaining(UUID playerId) {
		Player player = Bukkit.getPlayer(playerId);
		if (player != null && player.hasPermission("thtempfly.fly.infinite")) {
			return -1L; // -1 indicates infinite time
		}
		return playerToRemainingSeconds.getOrDefault(playerId, 0L);
	}

	public void setRemaining(UUID playerId, long seconds, boolean applyIfOnline) {
		long valueToStore = (seconds < 0L) ? -1L : Math.max(0L, seconds);
		playerToRemainingSeconds.put(playerId, valueToStore);
		
		if (applyIfOnline) {
			Player p = Bukkit.getPlayer(playerId);
			if (p != null) {
				boolean shouldFly = valueToStore != 0L;
				p.setAllowFlight(shouldFly);
				p.setFlying(shouldFly);
			}
		}
		
		syncToRedis(playerId, valueToStore);
	}

	/**
	 * Sets remaining time without syncing to Redis (for received messages).
	 * @param playerId The player's UUID
	 * @param seconds The remaining seconds
	 * @param applyIfOnline Whether to apply flight status if player is online
	 */
	public void setRemainingNoSync(UUID playerId, long seconds, boolean applyIfOnline) {
		long valueToStore = (seconds < 0L) ? -1L : Math.max(0L, seconds);
		playerToRemainingSeconds.put(playerId, valueToStore);
		
		if (applyIfOnline) {
			Player p = Bukkit.getPlayer(playerId);
			if (p != null) {
				boolean shouldFly = valueToStore != 0L;
				p.setAllowFlight(shouldFly);
				p.setFlying(shouldFly);
			}
		}
	}

	public void addSeconds(UUID playerId, long seconds, boolean applyIfOnline) {
		long current = getRemaining(playerId);
		
		if (current < 0L) {
			return;
		}
		
		setRemaining(playerId, current + seconds, applyIfOnline);
	}

	public void saveAll() {
		for (Map.Entry<UUID, Long> e : playerToRemainingSeconds.entrySet()) {
			save(e.getKey(), e.getValue());
		}
	}

	public void saveAllAsync() {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, this::saveAll);
	}

	private void save(UUID id, long seconds) {
		try {
			if (TempFlyCommand.isDebugMode()) {
				plugin.getLogger().info("[DEBUG] Saving to database: " + id + " = " + seconds + " seconds");
			}
			dataStore.setRemainingSeconds(id, seconds);
			if (TempFlyCommand.isDebugMode()) {
				plugin.getLogger().info("[DEBUG] Database save successful for " + id);
			}
		} catch (Exception e) {
			if (TempFlyCommand.isDebugMode()) {
				plugin.getLogger().severe("[DEBUG] Database save failed for " + id + ": " + e.getMessage());
			}
			e.printStackTrace();
		}
	}

	private void savePlayerAsync(UUID id, long seconds) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> save(id, seconds));
	}
	
	/**
	 * Disables flight for a player and sends appropriate messages
	 * @param player The player to disable flight for
	 */
	private void disableFlight(Player player) {
		player.setAllowFlight(false);
		player.setFlying(false);
		player.sendMessage(messageManager.getMessage("events.time-expired"));
		
		if (titlesEnabled) {
			messageManager.sendTitle(player, "events.titles.expired-title", "events.titles.expired-subtitle", 10, 40, 10);
		}
	}
	
	/**
	 * Shows title warning to player if needed based on remaining time and update interval
	 * @param player The player to show the warning to
	 * @param remainingSeconds The remaining fly time in seconds
	 */
	private void showTitleWarningIfNeeded(Player player, long remainingSeconds) {
		if (!titlesEnabled || remainingSeconds > warningThreshold) {
			return;
		}
		
		UUID playerId = player.getUniqueId();
		long currentTime = System.currentTimeMillis();
		Long lastTitleTime = playerLastTitleTime.get(playerId);
		
		// Check if enough time has passed since last title (convert titleUpdateInterval from ticks to milliseconds)
		long intervalMs = titleUpdateInterval * 50L; // 20 ticks = 1000ms, so 1 tick = 50ms
		
		if (lastTitleTime == null || (currentTime - lastTitleTime) >= intervalMs) {
			// Send warning title with countdown
			messageManager.sendTitle(
				player, 
				"events.titles.warning-title", 
				"events.titles.warning-subtitle", 
				"seconds", 
				String.valueOf(remainingSeconds),
				5, // fadeIn
				20, // stay (1 second)
				5  // fadeOut
			);
			
			// Update last title time
			playerLastTitleTime.put(playerId, currentTime);
		}
	}
	
	/**
	 * Synchronizes player data to Redis for cross-server communication
	 * @param playerId The player's UUID
	 * @param seconds The remaining seconds to sync
	 */
	private void syncToRedis(UUID playerId, long seconds) {
		if (redisService != null) {
			try {
				Long last = lastSyncedSeconds.get(playerId);
				if (last != null && last == seconds) {
					if (TempFlyCommand.isDebugMode()) {
						plugin.getLogger().info("[DEBUG] Skipping Redis sync for " + playerId + " (no change: " + seconds + ")");
					}
					return; // Don't send duplicates
				}
				String serverName = plugin.getConfig().getString("redis.server-name", "TH-TempFly");
				String message = "PLAYER_SYNC:" + playerId.toString() + ":" + seconds + ":" + serverName;
				if (TempFlyCommand.isDebugMode()) {
					plugin.getLogger().info("[DEBUG] Publishing to Redis: " + message);
				}
				redisService.publish(message);
				lastSyncedSeconds.put(playerId, seconds);
				if (TempFlyCommand.isDebugMode()) {
					plugin.getLogger().info("[DEBUG] Redis publish successful for " + playerId);
				}
			} catch (Exception e) {
				if (TempFlyCommand.isDebugMode()) {
					plugin.getLogger().severe("[DEBUG] Redis publish failed for " + playerId + ": " + e.getMessage());
				}
				plugin.getLogger().warning("Failed to sync player data to Redis: " + e.getMessage());
			}
		} else if (TempFlyCommand.isDebugMode()) {
			plugin.getLogger().info("[DEBUG] Redis service is null, cannot sync " + playerId);
		}
	}
	
	/**
	 * Synchronizes all player data to Redis for multiserver consistency
	 */
	private void syncAllPlayersToRedis() {
		if (redisService != null) {
			try {
				String serverName = plugin.getConfig().getString("redis.server-name", "TH-TempFly");
				for (Map.Entry<UUID, Long> entry : playerToRemainingSeconds.entrySet()) {
					UUID playerId = entry.getKey();
					long seconds = entry.getValue();
					Long last = lastSyncedSeconds.get(playerId);
					if (last == null || last != seconds) {
						String message = "PLAYER_SYNC:" + playerId.toString() + ":" + seconds + ":" + serverName;
						redisService.publish(message);
						lastSyncedSeconds.put(playerId, seconds);
					}
				}
			} catch (Exception e) {
				plugin.getLogger().warning("Failed to sync all player data to Redis: " + e.getMessage());
			}
		}
	}
}
