package com.github.djkingcraftero89.TH_TempFly.integration;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.Method;

/**
 * Integración con Vulcan Anti-Cheat para evitar detecciones de fly legales
 * Usa reflexión para evitar dependencias en tiempo de compilación
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
     * Inicializa la API de Vulcan usando reflexión
     * @return true si se inicializó correctamente
     */
    private boolean initializeVulcan() {
        try {
            if (!plugin.getServer().getPluginManager().isPluginEnabled("Vulcan")) {
                plugin.getLogger().info("Vulcan Anti-Cheat no encontrado, integración deshabilitada.");
                return false;
            }
            
            // Obtener la clase VulcanAPI usando reflexión
            Class<?> vulcanAPIClass = Class.forName("me.frep.vulcan.api.VulcanAPI");
            Class<?> factoryClass = Class.forName("me.frep.vulcan.api.VulcanAPI$Factory");
            
            // Obtener la instancia de API
            Method getApiMethod = factoryClass.getDeclaredMethod("getApi");
            this.vulcanAPI = getApiMethod.invoke(null);
            
            // Obtener los métodos que necesitamos
            this.addExemptionMethod = vulcanAPIClass.getDeclaredMethod("addExemption", Player.class, String.class);
            this.removeExemptionMethod = vulcanAPIClass.getDeclaredMethod("removeExemption", Player.class, String.class);
            
            plugin.getLogger().info("Integración con Vulcan Anti-Cheat activada correctamente!");
            return true;
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("Vulcan Anti-Cheat no encontrado, integración deshabilitada.");
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning("Error al inicializar Vulcan API: " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
            return false;
        }
    }
    
    /**
     * Habilita las excepciones de fly para un jugador
     * Esto evita que Vulcan detecte el fly como hack
     * @param player El jugador a exentar
     */
    public void enableFlyExemption(Player player) {
        if (!enabled || vulcanAPI == null || addExemptionMethod == null) {
            return;
        }
        
        try {
            // Exempt los checks relacionados con vuelo
            // Flight - detección principal de vuelo
            // Elytra - puede detectar vuelo con elytras
            // Speed - puede detectar velocidad de vuelo
            // Motion - puede detectar movimientos anormales en el aire
            addExemptionMethod.invoke(vulcanAPI, player, "Flight");
            addExemptionMethod.invoke(vulcanAPI, player, "Elytra");
            addExemptionMethod.invoke(vulcanAPI, player, "Speed");
            addExemptionMethod.invoke(vulcanAPI, player, "Motion");
            
            plugin.getLogger().info("Excepciones de fly habilitadas en Vulcan para " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("Error al establecer excepciones de Vulcan para " + player.getName() + ": " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Deshabilita las excepciones de fly para un jugador
     * Vulcan volverá a detectar hacks normalmente
     * @param player El jugador a remover de excepciones
     */
    public void disableFlyExemption(Player player) {
        if (!enabled || vulcanAPI == null || removeExemptionMethod == null) {
            return;
        }
        
        try {
            // Remover las excepciones de checks de vuelo
            removeExemptionMethod.invoke(vulcanAPI, player, "Flight");
            removeExemptionMethod.invoke(vulcanAPI, player, "Elytra");
            removeExemptionMethod.invoke(vulcanAPI, player, "Speed");
            removeExemptionMethod.invoke(vulcanAPI, player, "Motion");
            
            plugin.getLogger().info("Excepciones de fly deshabilitadas en Vulcan para " + player.getName());
        } catch (Exception e) {
            plugin.getLogger().warning("Error al remover excepciones de Vulcan para " + player.getName() + ": " + e.getMessage());
            if (plugin.getConfig().getBoolean("debug", false)) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Verifica si la integración con Vulcan está activa
     * @return true si Vulcan está disponible y la integración funciona
     */
    public boolean isEnabled() {
        return enabled;
    }
}

