package com.github.djkingcraftero89.TH_TempFly.listener;

import com.github.djkingcraftero89.TH_TempFly.TH_TempFly;
import com.github.djkingcraftero89.TH_TempFly.cache.LocationCache;
import com.github.djkingcraftero89.TH_TempFly.fly.FlyManager;
import com.github.djkingcraftero89.TH_TempFly.restriction.FlightRestrictionManager;
import com.github.djkingcraftero89.TH_TempFly.util.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
	private final TH_TempFly plugin;
	private final FlyManager flyManager;
	private final FlightRestrictionManager restrictionManager;
	private final MessageManager messageManager;
	private final LocationCache locationCache;
	private final boolean enableOnJoin;
	private final boolean disableOnQuit;
	private final boolean notifyAdmins;

	public PlayerListener(TH_TempFly plugin, FlyManager flyManager, FlightRestrictionManager restrictionManager, MessageManager messageManager, boolean enableOnJoin, boolean disableOnQuit, boolean notifyAdmins) {
		this.plugin = plugin;
		this.flyManager = flyManager;
		this.restrictionManager = restrictionManager;
		this.messageManager = messageManager;
		this.enableOnJoin = enableOnJoin;
		this.disableOnQuit = disableOnQuit;
		this.notifyAdmins = notifyAdmins;

		// Initialize LocationCache with configurable interval
		int checkInterval = plugin.getConfig().getInt("fly.restrictions.check-interval-blocks", 10);
		this.locationCache = new LocationCache(checkInterval);
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		flyManager.loadPlayer(p, enableOnJoin);
		
		// Notify admin about updates if available
		if (notifyAdmins && p.hasPermission("thtempfly.admin") && plugin.getUpdateChecker() != null) {
			plugin.getUpdateChecker().notifyPlayer(p, messageManager);
		}
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		flyManager.unloadPlayer(p, disableOnQuit);

		// Clear location cache to prevent memory leaks
		locationCache.clearCache(p.getUniqueId());
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent e) {
		Player p = e.getPlayer();
		Location from = e.getFrom();
		Location to = e.getTo();

		// Check if player actually moved to a new block (ignore head movement)
		if (to == null || (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ())) {
			return;
		}

		// Only check if player is flying
		if (!p.isFlying()) {
			return;
		}

		// Use LocationCache to determine if we should perform expensive region checks
		// This dramatically reduces the number of WorldGuard API calls
		if (!locationCache.shouldCheck(p, to)) {
			return; // Player hasn't moved far enough, skip check
		}

		// Check if flight is restricted at the new location
		FlightRestrictionManager.RestrictionResult restriction = restrictionManager.checkFlightAllowed(p);
		if (restriction.isBlocked()) {
			// Disable flight
			p.setFlying(false);
			p.setAllowFlight(false);

			// Send appropriate message
			if (restriction.getType() == FlightRestrictionManager.RestrictionType.WORLD) {
				p.sendMessage(messageManager.getMessage("fly.restrictions.world-blocked", "world", restriction.getRestrictionName()));
			} else if (restriction.getType() == FlightRestrictionManager.RestrictionType.REGION) {
				p.sendMessage(messageManager.getMessage("fly.restrictions.region-blocked", "region", restriction.getRestrictionName()));
			}
			p.sendMessage(messageManager.getMessage("fly.restrictions.entering-blocked-zone"));
		}
	}
}
