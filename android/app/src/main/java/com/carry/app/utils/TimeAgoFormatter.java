package com.carry.app.utils;

public class TimeAgoFormatter {

    public static String format(long timestampMs, long currentMs) {
        if (timestampMs <= 0) return "Unknown";
        long diffMs = currentMs - timestampMs;
        if (diffMs < 0) diffMs = 0;

        long minutes = diffMs / (60 * 1000);
        if (minutes < 1) {
            return "Just now";
        } else if (minutes == 1) {
            return "1 min ago";
        } else if (minutes < 60) {
            return minutes + " min ago";
        } else {
            long hours = minutes / 60;
            if (hours == 1) {
                return "1 hour ago";
            } else if (hours < 24) {
                return hours + " hours ago";
            } else {
                long days = hours / 24;
                return days == 1 ? "1 day ago" : days + " days ago";
            }
        }
    }
}
