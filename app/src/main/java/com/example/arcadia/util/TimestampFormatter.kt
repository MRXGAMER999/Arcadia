package com.example.arcadia.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utility object for formatting timestamps in friend requests and related features.
 * 
 * Formatting rules:
 * - Less than 24 hours: Relative time (e.g., "Just now", "5 minutes ago", "2 hours ago")
 * - 24 hours or more: Absolute date (e.g., "Dec 8" for current year, "Nov 15, 2024" for past years)
 * 
 * Requirements: 6.5, 6.6
 */
object TimestampFormatter {
    
    private const val MILLIS_PER_MINUTE = 1000L * 60
    private const val MILLIS_PER_HOUR = MILLIS_PER_MINUTE * 60
    private const val MILLIS_PER_DAY = MILLIS_PER_HOUR * 24
    
    /**
     * Formats a timestamp to a human-readable string.
     * 
     * @param timestamp The timestamp in milliseconds since epoch
     * @return Formatted string representing the time
     */
    fun format(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        // Handle future timestamps or very recent (within 1 minute)
        if (diff < MILLIS_PER_MINUTE) {
            return "Just now"
        }
        
        val hours = diff / MILLIS_PER_HOUR
        
        return when {
            hours < 1 -> {
                val minutes = (diff / MILLIS_PER_MINUTE).toInt()
                if (minutes == 1) "1 minute ago" else "$minutes minutes ago"
            }
            hours < 24 -> {
                val hourCount = hours.toInt()
                if (hourCount == 1) "1 hour ago" else "$hourCount hours ago"
            }
            else -> formatAbsoluteDate(timestamp)
        }
    }
    
    /**
     * Formats a timestamp to an absolute date string.
     * Uses "MMM d" for current year (e.g., "Dec 8")
     * Uses "MMM d, yyyy" for past years (e.g., "Nov 15, 2024")
     */
    private fun formatAbsoluteDate(timestamp: Long): String {
        val date = Date(timestamp)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val timestampCalendar = Calendar.getInstance().apply { time = date }
        val timestampYear = timestampCalendar.get(Calendar.YEAR)
        
        val pattern = if (timestampYear == currentYear) {
            "MMM d"
        } else {
            "MMM d, yyyy"
        }
        
        return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
    }
}
