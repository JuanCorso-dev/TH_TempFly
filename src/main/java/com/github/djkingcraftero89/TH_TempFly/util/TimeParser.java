package com.github.djkingcraftero89.TH_TempFly.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeParser {
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhd]?)", Pattern.CASE_INSENSITIVE);
    
    /**
     * Parse time string to seconds
     * Supports formats: 30, 30s, 5m, 2h, 1d
     * @param timeStr time string to parse
     * @return seconds, or 0 if invalid
     */
    public static long parseTimeToSeconds(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return 0;
        }
        
        timeStr = timeStr.trim().toLowerCase();
        Matcher matcher = TIME_PATTERN.matcher(timeStr);
        
        if (!matcher.matches()) {
            return 0;
        }
        
        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);
        
        if (unit.isEmpty() || unit.equals("s")) {
            return value;
        } else if (unit.equals("m")) {
            return value * 60;
        } else if (unit.equals("h")) {
            return value * 3600;
        } else if (unit.equals("d")) {
            return value * 86400;
        }
        
        return 0;
    }
    
    /**
     * Format seconds to human readable string
     * @param seconds seconds to format (-1 for infinite)
     * @return formatted string like "1h 30m 15s" or "Infinite"
     */
    public static String formatSeconds(long seconds) {
        if (seconds == -1) {
            return "Infinite";
        }
        if (seconds <= 0) {
            return "0s";
        }
        
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (secs > 0 || sb.length() == 0) {
            sb.append(secs).append("s");
        }
        
        return sb.toString().trim();
    }
}
