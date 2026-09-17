package com.github.djkingcraftero89.TH_TempFly.cache;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Throttles expensive region checks by remembering the last location at which a
 * check was performed for each player.
 *
 * <p>A check is only requested again once the player has travelled at least the
 * configured number of blocks horizontally from that reference point, or has changed
 * world. Distances are compared squared so no square root is needed on the hot path.
 */
public final class LocationCache {

	private final int checkIntervalBlocks;
	private final double thresholdSquared;
	private final ConcurrentHashMap<UUID, Reference> references = new ConcurrentHashMap<>();

	/**
	 * @param checkIntervalBlocks minimum horizontal distance, in blocks, between two
	 *                            expensive checks; values below one are clamped to one
	 */
	public LocationCache(int checkIntervalBlocks) {
		this.checkIntervalBlocks = Math.max(1, checkIntervalBlocks);
		this.thresholdSquared = (double) this.checkIntervalBlocks * this.checkIntervalBlocks;
	}

	/**
	 * Decides whether the expensive restriction check should run now, and records the
	 * given location as the new reference point when it should.
	 *
	 * @param p  the moving player
	 * @param to the location the player moved to
	 * @return {@code true} on the first call for a player, after a world change, or once
	 *         the player has moved at least the configured distance
	 */
	public boolean shouldCheck(Player p, Location to) {
		if (p == null || to == null) {
			return false;
		}

		World world = to.getWorld();
		String worldName = (world != null) ? world.getName() : null;
		final UUID uuid = p.getUniqueId();

		Reference previous = references.get(uuid);
		if (previous != null
				&& previous.matchesWorld(worldName)
				&& previous.distanceSquaredTo(to.getX(), to.getZ()) < thresholdSquared) {
			return false;
		}

		references.put(uuid, new Reference(worldName, to.getX(), to.getZ()));
		return true;
	}

	/**
	 * Removes the stored reference point for a player. Called on quit so the cache does
	 * not retain entries for offline players.
	 *
	 * @param uuid the player's unique id
	 */
	public void clearCache(UUID uuid) {
		if (uuid != null) {
			references.remove(uuid);
		}
	}

	/** Last location at which an expensive check was performed. */
	private static final class Reference {
		private final String worldName;
		private final double x;
		private final double z;

		private Reference(String worldName, double x, double z) {
			this.worldName = worldName;
			this.x = x;
			this.z = z;
		}

		private boolean matchesWorld(String other) {
			return (worldName == null) ? (other == null) : worldName.equals(other);
		}

		private double distanceSquaredTo(double otherX, double otherZ) {
			double dx = otherX - x;
			double dz = otherZ - z;
			return (dx * dx) + (dz * dz);
		}
	}
}
