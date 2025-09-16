package com.github.djkingcraftero89.TH_TempFly.fly;

import com.github.djkingcraftero89.TH_TempFly.storage.DataStore;
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
	private final Map<UUID, Long> playerToRemainingSeconds = new HashMap<>();
	private final int tickIntervalTicks;
	private final long saveIntervalSeconds;

	private int taskId = -1;
	private long lastSaveEpochSecond = 0L;

	public FlyManager(Plugin plugin, DataStore dataStore, int tickIntervalTicks, long saveIntervalSeconds) {
		this.plugin = plugin;
		this.dataStore = dataStore;
		this.tickIntervalTicks = tickIntervalTicks;
		this.saveIntervalSeconds = saveIntervalSeconds;
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
		// Only tick down time for online players
		for (Player p : Bukkit.getOnlinePlayers()) {
			UUID id = p.getUniqueId();
			
			// Skip players with infinite fly permission
			if (p.hasPermission("thtempfly.fly.infinite")) {
				continue;
			}
			
			long remaining = playerToRemainingSeconds.getOrDefault(id, 0L);
			if (remaining > 0) {
				remaining -= tickIntervalTicks / 20L; // convert ticks to seconds step
				if (remaining <= 0) {
					remaining = 0;
					p.setAllowFlight(false);
					p.setFlying(false);
					p.sendMessage("&cYour fly time has expired!");
				}
				playerToRemainingSeconds.put(id, remaining);
			}
		}
		// Note: Offline players' time is preserved and will resume when they join

		long nowSec = System.currentTimeMillis() / 1000L;
		if (nowSec - lastSaveEpochSecond >= saveIntervalSeconds) {
			lastSaveEpochSecond = nowSec;
			saveAllAsync();
		}
	}

	public void loadPlayer(Player p, boolean enableOnJoin) {
		UUID id = p.getUniqueId();
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				Optional<Long> opt = dataStore.getRemainingSeconds(id);
				long sec = opt.orElse(0L);
				Bukkit.getScheduler().runTask(plugin, () -> {
					playerToRemainingSeconds.put(id, sec);
					if (enableOnJoin && sec > 0) {
						p.setAllowFlight(true);
						p.setFlying(true);
						String formattedTime = TimeParser.formatSeconds(sec);
						p.sendMessage("§aWelcome back! You have §e" + formattedTime + " §aof fly time remaining.");
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	public void unloadPlayer(Player p, boolean disableOnQuit) {
		UUID id = p.getUniqueId();
		if (disableOnQuit) {
			p.setAllowFlight(false);
			p.setFlying(false);
		}
		savePlayerAsync(id);
		playerToRemainingSeconds.remove(id);
	}

	public long getRemaining(UUID playerId) {
		Player player = Bukkit.getPlayer(playerId);
		if (player != null && player.hasPermission("thtempfly.fly.infinite")) {
			return -1L; // -1 indicates infinite time
		}
		return playerToRemainingSeconds.getOrDefault(playerId, 0L);
	}

	public void setRemaining(UUID playerId, long seconds, boolean applyIfOnline) {
		playerToRemainingSeconds.put(playerId, Math.max(0L, seconds));
		Player p = Bukkit.getPlayer(playerId);
		if (p != null && applyIfOnline) {
			if (seconds > 0) {
				p.setAllowFlight(true);
				p.setFlying(true);
			} else {
				p.setAllowFlight(false);
				p.setFlying(false);
			}
		}
	}

	public void addSeconds(UUID playerId, long seconds, boolean applyIfOnline) {
		setRemaining(playerId, getRemaining(playerId) + seconds, applyIfOnline);
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
			dataStore.setRemainingSeconds(id, seconds);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void savePlayerAsync(UUID id) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> save(id, getRemaining(id)));
	}
}
