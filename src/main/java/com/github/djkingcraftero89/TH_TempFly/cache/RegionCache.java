package com.github.djkingcraftero89.TH_TempFly.cache;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Per-chunk cache of the blocked region names relevant to a location.
 *
 * <p>Region data barely changes within a chunk, so results are keyed by world name plus
 * chunk coordinates and reused for a configurable time-to-live. This avoids repeating the
 * expensive WorldGuard query for every movement inside the same chunk.
 *
 * <p>The backing map is an access-ordered {@link LinkedHashMap} guarded by its own
 * monitor, giving LRU eviction and a hard upper bound on the number of cached chunks.
 */
public final class RegionCache {

	private final int maxSize;
	private final long ttlMillis;
	private final Map<String, Entry> cache;

	/**
	 * @param maxSize    maximum number of chunks to keep cached; values below one are
	 *                   clamped to one
	 * @param ttlSeconds how long a cached result stays valid, in seconds; values below
	 *                   zero are clamped to zero (always reload)
	 */
	public RegionCache(int maxSize, int ttlSeconds) {
		this.maxSize = Math.max(1, maxSize);
		this.ttlMillis = Math.max(0, ttlSeconds) * 1000L;

		final int limit = this.maxSize;
		this.cache = new LinkedHashMap<>(Math.min(limit, 64), 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
				return size() > limit;
			}
		};
	}

	/**
	 * Returns the blocked region names cached for the chunk containing the location,
	 * loading and storing them only when there is no fresh cached value.
	 *
	 * @param location the location whose chunk is looked up
	 * @param loader   supplier invoked only on a cache miss or expiry; an empty result is
	 *                 cached as well, since that is a valid answer
	 * @return the cached or freshly loaded set of blocked region names, never {@code null}
	 */
	public Set<String> getBlockedRegions(Location location, Supplier<Set<String>> loader) {
		if (location == null) {
			return (loader != null) ? nullSafe(loader.get()) : Collections.emptySet();
		}

		final String key = buildKey(location);
		final long now = System.currentTimeMillis();

		synchronized (cache) {
			Entry cached = cache.get(key);
			if (cached != null && now < cached.expiresAt) {
				return cached.regions;
			}
		}

		Set<String> loaded = (loader != null) ? nullSafe(loader.get()) : Collections.emptySet();

		synchronized (cache) {
			cache.put(key, new Entry(loaded, System.currentTimeMillis() + ttlMillis));
		}
		return loaded;
	}

	/** Drops every cached chunk, for example after a configuration reload. */
	public void invalidateAll() {
		synchronized (cache) {
			cache.clear();
		}
	}

	/** Builds the cache key from the world name and the chunk coordinates. */
	private static String buildKey(Location location) {
		World world = location.getWorld();
		String worldName = (world != null) ? world.getName() : "unknown";
		int chunkX = location.getBlockX() >> 4;
		int chunkZ = location.getBlockZ() >> 4;
		return worldName + ':' + chunkX + ':' + chunkZ;
	}

	private static Set<String> nullSafe(Set<String> value) {
		return (value != null) ? value : Collections.emptySet();
	}

	/** Cached region set with its expiry instant. */
	private static final class Entry {
		private final Set<String> regions;
		private final long expiresAt;

		private Entry(Set<String> regions, long expiresAt) {
			this.regions = regions;
			this.expiresAt = expiresAt;
		}
	}
}
