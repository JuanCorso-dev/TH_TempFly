package com.github.djkingcraftero89.TH_TempFly.placeholder;

import com.github.djkingcraftero89.TH_TempFly.fly.FlyManager;
import com.github.djkingcraftero89.TH_TempFly.util.TimeParser;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TempFlyPlaceholder extends PlaceholderExpansion {
	private final FlyManager flyManager;

	public TempFlyPlaceholder(FlyManager flyManager) {
		this.flyManager = flyManager;
	}

	@Override
	public @NotNull String getIdentifier() {
		return "thtempfly";
	}

	@Override
	public @NotNull String getAuthor() {
		return "JuanCorso";
	}

	@Override
	public @NotNull String getVersion() {
		return "1.0.0";
	}

	@Override
	public boolean persist() {
		return true;
	}

	@Override
	public String onPlaceholderRequest(Player player, @NotNull String params) {
		if (player == null) {
			return "";
		}

		long remainingTime = flyManager.getRemaining(player.getUniqueId());
		boolean hasInfiniteFly = player.hasPermission("thtempfly.fly.infinite");

		switch (params.toLowerCase()) {
			case "time":
			case "remaining":
			case "left":
				if (hasInfiniteFly) {
					return "Infinite";
				}
				return TimeParser.formatSeconds(remainingTime);
			
			case "time_seconds":
			case "remaining_seconds":
			case "left_seconds":
				if (hasInfiniteFly) {
					return "∞";
				}
				return String.valueOf(remainingTime);
			
			case "time_minutes":
			case "remaining_minutes":
			case "left_minutes":
				if (hasInfiniteFly) {
					return "∞";
				}
				return String.valueOf(remainingTime / 60);
			
			case "time_hours":
			case "remaining_hours":
			case "left_hours":
				if (hasInfiniteFly) {
					return "∞";
				}
				return String.valueOf(remainingTime / 3600);
			
			case "has_time":
			case "can_fly":
				return (hasInfiniteFly || remainingTime > 0) ? "true" : "false";
			
			case "status":
				if (hasInfiniteFly) {
					return player.getAllowFlight() ? "flying" : "can_fly";
				}
				if (remainingTime > 0) {
					return player.getAllowFlight() ? "flying" : "can_fly";
				} else {
					return "no_time";
				}
			
			default:
				return null;
		}
	}
}
