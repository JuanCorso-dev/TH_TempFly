package com.github.djkingcraftero89.TH_TempFly.integration;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.Method;

/**
 * Vulcan Anti-Cheat integration to prevent false-positive fly detections
 * Uses reflection to avoid compile-time dependencies
 */
public class VulcanIntegration {
    private final Plugin plugin;
    private Object vulcanAPI;
    private Method addExemptionMethod;
    private Method removeExemptionMethod;
    private final boolean enabled;

    public VulcanIntegration(Plugin plugin) {
        this.plugin = plugin;
        this.enabled = initializeVulcan();
    }

    /**
     * Initializes the Vulcan API using reflection
     * @return true if it was initialized successfully
     */
    private boolean initializeVulcan() {
        try {
            if (!plugin.getServer().getPluginManager().isPluginEnabled("Vulcan")) {
                plugin.getLogger().info("Vulcan Anti-Cheat not found, integration disabled.");
                return false;
            }

            // Get the VulcanAPI class using reflection
            Class<?> vulcanAPIClass = Class.forName("me.frep.vulcan.api.VulcanAPI");
            Class<?> factoryClass = Class.forName("me.frep.vulcan.api.VulcanAPI$Factory");

            // Get the API instance
            Method getApiMethod = factoryClass.getDeclaredMethod("getApi");
            this.vulcanAPI = getApiMethod.invoke(null);

            // Get the methods we need
            this.addExemptionMethod = vulcanAPIClass.getDeclaredMethod("addExemption", Player.class, String.class);
            this.removeExemptionMethod = vulcanAPIClass.getDeclaredMethod("removeExemption", Player.class, String.class);

            plugin.getLogger().info("Vulcan Anti-Cheat integration enabled successfully!");
            return true;
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("Vulcan Anti-Cheat not found, integration disabled.");
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("Error initializing Vulcan API: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * Enables fly exemptions for a player
     * This prevents Vulcan from detecting fly as a hack
     * @param player The player to exempt
     */
    public void enableFlyExemption(Player player) {
        if (!enabled || vulcanAPI == null || addExemptionMethod == null) {
            return;
        }

        try {
            // Exempt flight-related checks
            // Flight - primary flight detection
            // Elytra - may detect flight via elytra
            // Speed - may detect flight speed
            // Motion - may detect abnormal air movement
            addExemptionMethod.invoke(vulcanAPI, player, "Flight");
            addExemptionMethod.invoke(vulcanAPI, player, "Elytra");
            addExemptionMethod.invoke(vulcanAPI, player, "Speed");
            addExemptionMethod.invoke(vulcanAPI, player, "Motion");

            plugin.getLogger().info("Fly exemptions enabled in Vulcan for " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("Error setting Vulcan exemptions for " + player.getName() + ": " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Disables fly exemptions for a player
     * Vulcan will resume detecting hacks normally
     * @param player The player to remove exemptions from
     */
    public void disableFlyExemption(Player player) {
        if (!enabled || vulcanAPI == null || removeExemptionMethod == null) {
            return;
        }

        try {
            // Remove the flight check exemptions
            removeExemptionMethod.invoke(vulcanAPI, player, "Flight");
            removeExemptionMethod.invoke(vulcanAPI, player, "Elytra");
            removeExemptionMethod.invoke(vulcanAPI, player, "Speed");
            removeExemptionMethod.invoke(vulcanAPI, player, "Motion");

            plugin.getLogger().info("Fly exemptions disabled in Vulcan for " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("Error removing Vulcan exemptions for " + player.getName() + ": " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Checks whether the Vulcan integration is active
     * @return true if Vulcan is available and the integration is working
     */
    public boolean isEnabled() {
        return enabled;
    }
}

