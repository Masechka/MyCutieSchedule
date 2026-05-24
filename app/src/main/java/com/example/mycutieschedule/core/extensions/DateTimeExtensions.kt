package com.example.mycutieschedule.core.extensions

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

// ── LocalDate ─────────────────────────────────────────────────────────────────

/** Returns the Monday of the week containing this date. */
fun LocalDate.toWeekStart(): LocalDate =
    with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

/** Returns the Sunday of the week containing this date. */
fun LocalDate.toWeekEnd(): LocalDate =
    with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

/** Returns the first day of the month of this date. */
fun LocalDate.toMonthStart(): LocalDate = withDayOfMonth(1)

/** Returns the last day of the month of this date. */
fun LocalDate.toMonthEnd(): LocalDate = withDayOfMonth(lengthOfMonth())

/** E.g. "28 апр" / "28 Apr" — short date for calendar headers. */
fun LocalDate.toShortDisplay(locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("d MMM", locale).format(this)

/** E.g. "28 апреля 2026" — full date for event detail. */
fun LocalDate.toLongDisplay(locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("d MMMM yyyy", locale).format(this)

/** Returns all 7 days of the week (Mon–Sun) that contain this date. */
fun LocalDate.toWeekDays(): List<LocalDate> {
    val monday = toWeekStart()
    return (0..6).map { monday.plusDays(it.toLong()) }
}

// ── LocalDateTime ─────────────────────────────────────────────────────────────

/** E.g. "09:00" — time label for week view time column. */
fun LocalDateTime.toTimeLabel(): String =
    DateTimeFormatter.ofPattern("HH:mm").format(this)

/** E.g. "28 апр · 09:00" — compact event display. */
fun LocalDateTime.toEventDisplay(locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("d MMM · HH:mm", locale).format(this)

/** Duration in minutes between two LocalDateTimes. Negative if end is before start. */
fun LocalDateTime.minutesUntil(end: LocalDateTime): Long =
    java.time.Duration.between(this, end).toMinutes()

// ── YearMonth ─────────────────────────────────────────────────────────────────

// ── Epoch conversion — used by Room entity mappers ────────────────────────────
// All datetimes are stored as UTC epoch milliseconds for consistent cross-device sync.

/** Converts [LocalDateTime] to epoch milliseconds using UTC offset. */
fun LocalDateTime.toEpochMilli(): Long =
    toInstant(ZoneOffset.UTC).toEpochMilli()

/** Restores a [LocalDateTime] from epoch milliseconds using UTC offset. */
fun Long.toLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneOffset.UTC)

/**
 * Restores a [LocalDate] from an epoch day value.
 * Complement of the stdlib's [LocalDate.toEpochDay].
 */
fun Long.toLocalDate(): LocalDate = LocalDate.ofEpochDay(this)

// ── YearMonth ─────────────────────────────────────────────────────────────────

/** E.g. "Апрель 2026" / "April 2026" — month header. */
fun YearMonth.toDisplayString(locale: Locale = Locale.getDefault()): String =
    DateTimeFormatter.ofPattern("LLLL yyyy", locale)
        .withLocale(locale)
        .format(this)
        .replaceFirstChar { it.uppercaseChar() }
