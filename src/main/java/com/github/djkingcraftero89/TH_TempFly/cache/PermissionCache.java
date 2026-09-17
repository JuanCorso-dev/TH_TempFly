package com.github.djkingcraftero89.TH_TempFly.cache;

import org.bukkit.entity.Player;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Short-lived cache for the expensive {@code thtempfly.fly.infinite} permission lookup.
 *
 * <p>This is queried from the fly tick loop, so the hot path is a single
 * {@link ConcurrentHashMap} get plus a timestamp comparison.
 *
 * <p>No external cleanup hook exists, so the cache bounds itself: every access may
 * opportunistically purge entries whose window has elapsed (for example entries left
 * behind by players who logged off). No public cleanup method is exposed.
 */
public final class PermissionCache {

	/** Permission node whose result is cached. */
	private static final String INFINITE_FLY_PERMISSION = "thtempfly.fly.infinite";

	/** How often, at most, a full purge sweep runs (milliseconds). */
	private static final long PURGE_INTERVAL_MS = 5_000L;

	/** Entry count above which a full sweep is also forced on the next access. */
	private static final int PURGE_SIZE_THRESHOLD = 256;

	private final long durationMs;
	private final ConcurrentHashMap<UUID, Entry> entries = new ConcurrentHashMap<>();
	private final AtomicLong nextPurgeAt = new AtomicLong(0L);

	/**
	 * @param durationMs how long a cached permission result stays valid, in milliseconds;
	 *                   values below zero are clamped to zero (always re-check)
	 */
	public PermissionCache(long durationMs) {
		this.durationMs = Math.max(0L, durationMs);
	}

	/**
	 * Returns whether the player holds the infinite fly permission, using the cached
	 * value when it is still within the configured window.
	 *
	 * @param p the player to check; {@code null} yields {@code false}
	 * @return {@code true} if the player has {@code thtempfly.fly.infinite}
	 */
	public boolean hasInfiniteFly(Player p) {
		if (p == null) {
			return false;
		}

		final UUID uuid = p.getUniqueId();
		final long now = System.currentTimeMillis();

		Entry cached = entries.get(uuid);
		if (cached != null && now < cached.expiresAt) {
			return cached.value;
		}

		boolean value = p.hasPermission(INFINITE_FLY_PERMISSION);
		entries.put(uuid, new Entry(value, now + durationMs));

		maybePurge(now);
		return value;
	}

	/**
	 * Drops expired entries at most once per {@link #PURGE_INTERVAL_MS}, so the map can
	 * never grow without bound for players that never come back.
	 */
	private void maybePurge(long now) {
		long scheduled = nextPurgeAt.get();
		if (now < scheduled && entries.size() < PURGE_SIZE_THRESHOLD) {
			return;
		}
		if (!nextPurgeAt.compareAndSet(scheduled, now + PURGE_INTERVAL_MS)) {
			// Another thread is already sweeping.
			return;
		}

		Iterator<Map.Entry<UUID, Entry>> it = entries.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Entry> e = it.next();
			if (now >= e.getValue().expiresAt) {
				it.remove();
			}
		}
	}

	/** Immutable cached permission result with its expiry instant. */
	private static final class Entry {
		private final boolean value;
		private final long expiresAt;

		private Entry(boolean value, long expiresAt) {
			this.value = value;
			this.expiresAt = expiresAt;
		}
	}
}
