package com.github.djkingcraftero89.TH_TempFly.fly;

import java.util.Locale;
import java.util.Optional;

/**
 * Defines when a player's remaining flight time counts down.
 */
public enum TimeConsumptionMode {
	/** Time counts down only while the player is in the air. */
	FLYING,
	/** Time counts down the whole time the player is online, whether they fly or not. */
	ONLINE;

	/**
	 * Parses a configuration value, ignoring case and surrounding whitespace.
	 *
	 * @param value the raw configuration value, may be {@code null}
	 * @return the matching mode, or empty when the value is missing or unknown
	 */
	public static Optional<TimeConsumptionMode> parse(String value) {
		if (value == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(valueOf(value.trim().toUpperCase(Locale.ROOT)));
		} catch (IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}
