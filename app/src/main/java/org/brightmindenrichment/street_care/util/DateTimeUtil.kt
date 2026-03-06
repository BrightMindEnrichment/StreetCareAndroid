package org.brightmindenrichment.street_care.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Returns today's date in the device's local timezone. */
fun localDateNow(): LocalDate = LocalDate.now(ZoneId.systemDefault())

/** Returns the current time in the device's local timezone. */
fun localTimeNow(): LocalTime = LocalTime.now(ZoneId.systemDefault())

/**
 * Converts a [LocalDate] to the UTC-midnight millisecond value expected by [MaterialDatePicker].
 * MaterialDatePicker always works in UTC, so we pass midnight UTC for a given calendar date.
 */
fun LocalDate.toPickerMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/**
 * Converts the millisecond value returned by [MaterialDatePicker] back to a [LocalDate].
 * The picker always returns midnight UTC, so we interpret in UTC to recover the correct date.
 */
fun Long.toLocalDateFromPicker(): LocalDate =
    Instant.ofEpochMilli(this).atOffset(ZoneOffset.UTC).toLocalDate()


/**
 * Parses an ISO-8601 time string and formats it to (e.g., "6:30 PM")
 * in the device's local timezone.
 */
val String?.toFormattedTime: String?
    get() {
        if (this.isNullOrBlank()) return null

        return try {
            val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

            // Parse the string to an Instant, then format it locally
            Instant.parse(this)
                .atZone(ZoneId.systemDefault())
                .format(formatter)
        } catch (e: Exception) {
            // Pro-tip: If parsing fails (e.g. malformed data), fallback to showing
            // the original string rather than crashing or showing nothing.
            this
        }
    }

/**
 * Converts a [LocalTime] to a timezone-aware ISO-8601 string that includes timezone offset and ID.
 * Format: "HH:mm:ss+HH:MM[ZoneId]" (e.g., "14:30:00-05:00[America/New_York]")
 *
 * This is used to preserve timezone context when storing times that should be interpreted
 * in a specific timezone. When parsed back, this string can be correctly converted to UTC.
 *
 * @param time The LocalTime to convert
 * @param zoneId The timezone to apply (defaults to system timezone)
 * @return ISO-8601 string with timezone context
 */
fun LocalTime.toZonedString(zoneId: ZoneId = ZoneId.systemDefault()): String =
    this.atDate(LocalDate.now()).atZone(zoneId).toString()

/**
 * Parses a timezone-aware ISO-8601 string back to a [ZonedDateTime].
 * This correctly handles times that were stored with timezone context.
 *
 * @param timeString ISO-8601 string with timezone (e.g., "14:30:00-05:00[America/New_York]")
 * @return ZonedDateTime if parsing succeeds, null otherwise
 */
fun parseTimezoneAwareTime(timeString: String): ZonedDateTime? =
    try {
        ZonedDateTime.parse(timeString)
    } catch (e: Exception) {
        null
    }

/**
 * Extracts just the time portion (HH:MM) from a timezone-aware ISO-8601 string.
 * Useful for displaying times in a simple format while preserving timezone context in storage.
 *
 * @param timeString ISO-8601 string with timezone
 * @return LocalTime if parsing succeeds, null otherwise
 */
fun extractLocalTimeFromZoned(timeString: String): LocalTime? =
    parseTimezoneAwareTime(timeString)?.toLocalTime()

/**
 * Formats a timezone-aware ISO-8601 time string for display with timezone abbreviation.
 * Handles both:
 * - ZonedDateTime strings: "14:00:00-05:00[America/New_York]" → "2:00 PM EST"
 * - ISO Instant strings: "2026-01-10T14:32:00Z" → "6:32 PM" (in device timezone)
 *
 * Falls back to showing the original string if parsing fails.
 *
 * @param timeString Time string to format (ZonedDateTime or Instant format)
 * @return Formatted time string with timezone if available, or original string on failure
 */
fun formatTimeWithTimezone(timeString: String?): String? {
    if (timeString.isNullOrBlank()) return null

    return try {
        // Try parsing as ZonedDateTime first (new format with timezone)
        val zdt = ZonedDateTime.parse(timeString)
        val formatter = DateTimeFormatter.ofPattern("h:mm a z", Locale.getDefault())
        zdt.format(formatter)
    } catch (e: Exception) {
        try {
            // Fallback: Try parsing as Instant (old format, interpret as UTC then show in device TZ)
            val formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
            Instant.parse(timeString)
                .atZone(ZoneId.systemDefault())
                .format(formatter)
        } catch (e2: Exception) {
            // If all parsing fails, return the original string
            timeString
        }
    }
}