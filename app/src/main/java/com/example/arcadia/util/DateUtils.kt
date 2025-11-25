package com.example.arcadia.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Utility object for date-related operations.
 * Centralizes date formatting and range calculations to avoid code duplication.
 */
object DateUtils {
    
    private val API_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    /**
     * Formats a LocalDate to the API-expected format (yyyy-MM-dd).
     */
    fun formatForApi(date: LocalDate): String = date.format(API_DATE_FORMATTER)
    
    /**
     * Creates a date range string for API calls from two LocalDate objects.
     * @param start Start date
     * @param end End date
     * @return Formatted string like "2024-01-01,2024-12-31"
     */
    fun formatDateRange(start: LocalDate, end: LocalDate): String {
        return "${formatForApi(start)},${formatForApi(end)}"
    }
    
    /**
     * Creates a date range string for API calls from two string dates.
     * @param start Start date string
     * @param end End date string
     * @return Formatted string like "2024-01-01,2024-12-31"
     */
    fun formatDateRange(start: String, end: String): String {
        return "$start,$end"
    }
    
    /**
     * Gets the current year date range as an API-formatted string.
     * @return Formatted string like "2024-01-01,2024-12-31"
     */
    fun getCurrentYearRange(): String {
        val now = LocalDate.now()
        val startOfYear = now.withDayOfYear(1)
        val endOfYear = now.withMonth(12).withDayOfMonth(31)
        return formatDateRange(startOfYear, endOfYear)
    }
    
    /**
     * Gets a date range for upcoming releases as an API-formatted string.
     * @param monthsAhead Number of months to look ahead (default: 12)
     * @return Formatted string like "2024-01-01,2025-01-01"
     */
    fun getUpcomingRange(monthsAhead: Long = 12): String {
        val today = LocalDate.now()
        val futureDate = today.plusMonths(monthsAhead)
        return formatDateRange(today, futureDate)
    }
    
    /**
     * Gets a date range for recent releases as an API-formatted string.
     * @param daysBack Number of days to look back (default: 60)
     * @return Formatted string like "2023-11-01,2024-01-01"
     */
    fun getRecentRange(daysBack: Long = 60): String {
        val today = LocalDate.now()
        val pastDate = today.minusDays(daysBack)
        return formatDateRange(pastDate, today)
    }
    
    /**
     * Gets a date range spanning years back from today as an API-formatted string.
     * @param yearsBack Number of years to look back (default: 5)
     * @return Formatted string like "2019-01-01,2024-01-01"
     */
    fun getYearsBackRange(yearsBack: Long = 5): String {
        val today = LocalDate.now()
        val pastDate = today.minusYears(yearsBack)
        return formatDateRange(pastDate, today)
    }
    
    /**
     * Gets the raw date pair for current year range.
     * @return Pair of (start of year, end of year)
     */
    fun getCurrentYearRangeDates(): Pair<LocalDate, LocalDate> {
        val now = LocalDate.now()
        return now.withDayOfYear(1) to now.withMonth(12).withDayOfMonth(31)
    }
    
    /**
     * Gets the raw date pair for upcoming range.
     * @return Pair of (today, future date)
     */
    fun getUpcomingRangeDates(monthsAhead: Long = 12): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        return today to today.plusMonths(monthsAhead)
    }
}
