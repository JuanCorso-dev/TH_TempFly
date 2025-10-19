package com.github.djkingcraftero89.TH_TempFly.restriction;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FlightRestrictionManager {
    private final Plugin plugin;
    private final Set<String> blockedWorlds = new HashSet<>();
    private final Set<String> blockedRegions = new HashSet<>();
    private final boolean worldGuardEnabled;
    private final boolean restrictionsEnabled;

    public FlightRestrictionManager(Plugin plugin, List<String> blockedWorldsList, List<String> blockedRegionsList, boolean enabled) {
        this.plugin = plugin;
        this.restrictionsEnabled = enabled;
        
        if (blockedWorldsList != null) {
            for (String world : blockedWorldsList) {
                blockedWorlds.add(world.toLowerCase());
            }
        }
        
        if (blockedRegionsList != null) {
            for (String region : blockedRegionsList) {
                blockedRegions.add(region.toLowerCase());
            }
        }
        
        this.worldGuardEnabled = plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard");
        
        if (worldGuardEnabled && !blockedRegions.isEmpty()) {
            plugin.getLogger().info("WorldGuard integration enabled with " + blockedRegions.size() + " blocked regions");
        } else if (!worldGuardEnabled && !blockedRegions.isEmpty()) {
            plugin.getLogger().warning("WorldGuard is not installed but regions are configured. Region restrictions will not work.");
        }
        
        if (!blockedWorlds.isEmpty()) {
            plugin.getLogger().info("Flight restrictions enabled for " + blockedWorlds.size() + " worlds");
        }
    }

    /**
     * Check if flight is allowed at the player's current location
     * @param player The player to check
     * @return RestrictionResult with details about the restriction
     */
    public RestrictionResult checkFlightAllowed(Player player) {
        if (!restrictionsEnabled) {
            return RestrictionResult.allowed();
        }
        
        Location location = player.getLocation();
        String worldName = location.getWorld().getName();
        
        // Check if world is blocked
        if (isWorldBlocked(worldName)) {
            return RestrictionResult.blockedByWorld(worldName);
        }
        
        // Check if in blocked region (only if WorldGuard is enabled)
        if (worldGuardEnabled && !blockedRegions.isEmpty()) {
            String blockedRegion = getBlockedRegionAt(location);
            if (blockedRegion != null) {
                return RestrictionResult.blockedByRegion(blockedRegion);
            }
        }
        
        return RestrictionResult.allowed();
    }

    /**
     * Check if a world is blocked for flight
     * @param worldName The world name
     * @return true if flight is blocked in this world
     */
    public boolean isWorldBlocked(String worldName) {
        return blockedWorlds.contains(worldName.toLowerCase());
    }

    /**
     * Get the name of the blocked region at the given location
     * @param location The location to check
     * @return The name of the blocked region, or null if not in a blocked region
     */
    private String getBlockedRegionAt(Location location) {
        if (!worldGuardEnabled) {
            return null;
        }
        
        try {
            RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
            RegionQuery query = container.createQuery();
            ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(location));
            
            for (ProtectedRegion region : set) {
                if (blockedRegions.contains(region.getId().toLowerCase())) {
                    return region.getId();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error checking WorldGuard regions: " + e.getMessage());
        }
        
        return null;
    }

    /**
     * Reload restrictions from config
     * @param blockedWorldsList Updated list of blocked worlds
     * @param blockedRegionsList Updated list of blocked regions
     */
    public void reload(List<String> blockedWorldsList, List<String> blockedRegionsList) {
        blockedWorlds.clear();
        blockedRegions.clear();
        
        if (blockedWorldsList != null) {
            for (String world : blockedWorldsList) {
                blockedWorlds.add(world.toLowerCase());
            }
        }
        
        if (blockedRegionsList != null) {
            for (String region : blockedRegionsList) {
                blockedRegions.add(region.toLowerCase());
            }
        }
        
        plugin.getLogger().info("Flight restrictions reloaded: " + blockedWorlds.size() + " worlds, " + blockedRegions.size() + " regions");
    }

    /**
     * Result of a flight restriction check
     */
    public static class RestrictionResult {
        private final boolean allowed;
        private final RestrictionType type;
        private final String restrictionName;

        private RestrictionResult(boolean allowed, RestrictionType type, String restrictionName) {
            this.allowed = allowed;
            this.type = type;
            this.restrictionName = restrictionName;
        }

        public static RestrictionResult allowed() {
            return new RestrictionResult(true, RestrictionType.NONE, null);
        }

        public static RestrictionResult blockedByWorld(String worldName) {
            return new RestrictionResult(false, RestrictionType.WORLD, worldName);
        }

        public static RestrictionResult blockedByRegion(String regionName) {
            return new RestrictionResult(false, RestrictionType.REGION, regionName);
        }

        public boolean isAllowed() {
            return allowed;
        }

        public boolean isBlocked() {
            return !allowed;
        }

        public RestrictionType getType() {
            return type;
        }

        public String getRestrictionName() {
            return restrictionName;
        }
    }

    public enum RestrictionType {
        NONE,
        WORLD,
        REGION
    }
}
