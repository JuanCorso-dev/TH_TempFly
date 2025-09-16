package com.github.djkingcraftero89.TH_TempFly.listener;

import com.github.djkingcraftero89.TH_TempFly.fly.FlyManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
	private final FlyManager flyManager;
	private final boolean enableOnJoin;
	private final boolean disableOnQuit;

	public PlayerListener(FlyManager flyManager, boolean enableOnJoin, boolean disableOnQuit) {
		this.flyManager = flyManager;
		this.enableOnJoin = enableOnJoin;
		this.disableOnQuit = disableOnQuit;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		flyManager.loadPlayer(p, enableOnJoin);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		flyManager.unloadPlayer(p, disableOnQuit);
	}
}
