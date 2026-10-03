package com.ishara.app.core.utils

import java.util.Locale

object TransitFormatters {

    fun formatRelativeTime(epochMillis: Long?, nowMillis: Long = System.currentTimeMillis()): String {
        if (epochMillis == null || epochMillis <= 0L) return "Saved offline"
        val diff = nowMillis - epochMillis
        if (diff < 60_000L) {
            return "Updated just now"
        }
        val minutes = diff / 60_000L
        if (minutes < 60) {
            return "Updated $minutes min ago"
        }
        val hours = minutes / 60
        if (hours < 24) {
            return "Updated $hours hours ago"
        }
        val days = hours / 24
        return if (days == 1L) "Updated yesterday" else "Updated $days days ago"
    }

    fun formatDistance(distanceMeters: Double?): String? {
        if (distanceMeters == null || distanceMeters <= 0.0) return null
        return if (distanceMeters < 1000.0) {
            "${distanceMeters.toInt()} m"
        } else {
            String.format(Locale.US, "%.1f km", distanceMeters / 1000.0)
        }
    }

    fun formatDuration(durationSeconds: Long?): String? {
        if (durationSeconds == null || durationSeconds <= 0L) return null
        val minutes = (durationSeconds + 59) / 60
        if (minutes < 60) {
            return "$minutes min"
        }
        val hours = minutes / 60
        val remMin = minutes % 60
        return if (remMin == 0L) "${hours}h" else "${hours}h ${remMin}m"
    }
}
