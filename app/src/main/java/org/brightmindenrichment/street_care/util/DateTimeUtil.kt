package org.brightmindenrichment.street_care.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

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
