package com.github.djkingcraftero89.TH_TempFly.listener;

import com.github.djkingcraftero89.TH_TempFly.TH_TempFly;
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
