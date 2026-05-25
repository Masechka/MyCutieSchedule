package com.example.mycutieschedule.domain.usecase.event

import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.model.Recurrence
import com.example.mycutieschedule.domain.model.RecurrenceType
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

private const val MAX_OCCURRENCES_PER_RANGE = 500

/**
 * Expands a recurring master [Event] into virtual occurrence copies for a given date range.
 *
 * The master's own original date IS included when it falls within the range, because the
 * calling use cases filter recurring events out of the regular range query so there is
 * no double-counting.
 *
 * Each virtual occurrence keeps all master fields (title, color, reminder, recurrence) and
 * gets a composite ID `{masterId}_{YYYY-MM-DD}` that is never persisted.
 */
class RecurrenceExpander @Inject constructor() {

    fun expand(master: Event, rangeStart: LocalDateTime, rangeEnd: LocalDateTime): List<Event> {
        val recurrence = master.recurrence ?: return emptyList()
        val masterDate     = master.startDateTime.toLocalDate()
        val rangeStartDate = rangeStart.toLocalDate()
        val rangeEndDate   = rangeEnd.toLocalDate()

        if (rangeEndDate.isBefore(masterDate)) return emptyList()

        val dates = when (recurrence.type) {
            RecurrenceType.DAILY       -> dailyDates(masterDate, rangeStartDate, rangeEndDate, recurrence)
            RecurrenceType.WEEKLY      -> weeklyDates(masterDate, rangeStartDate, rangeEndDate, recurrence)
            RecurrenceType.MONTHLY     -> monthlyDates(masterDate, rangeStartDate, rangeEndDate, recurrence)
            RecurrenceType.YEARLY      -> yearlyDates(masterDate, rangeStartDate, rangeEndDate, recurrence)
            RecurrenceType.CUSTOM_DAYS -> customDaysDates(masterDate, rangeStartDate, rangeEndDate, recurrence)
        }

        val durationMinutes = ChronoUnit.MINUTES.between(master.startDateTime, master.endDateTime)
        val masterTime      = master.startDateTime.toLocalTime()

        return dates.map { date ->
            val start = date.atTime(masterTime)
            master.copy(
                id            = "${master.id}_$date",
                startDateTime = start,
                endDateTime   = start.plusMinutes(durationMinutes),
            )
        }
    }

    // ── DAILY ─────────────────────────────────────────────────────────────────

    private fun dailyDates(
        masterDate: LocalDate, rangeStart: LocalDate, rangeEnd: LocalDate, r: Recurrence,
    ): List<LocalDate> {
        val effectiveEnd = r.endDate?.let { minOf(rangeEnd, it) } ?: rangeEnd
        val first        = maxOf(masterDate, rangeStart)
        if (first.isAfter(effectiveEnd)) return emptyList()

        val skipped        = ChronoUnit.DAYS.between(masterDate, first).toInt()
        val remainingCount = r.count?.minus(skipped)?.coerceAtLeast(0) ?: Int.MAX_VALUE

        val result = mutableListOf<LocalDate>()
        var current = first
        while (!current.isAfter(effectiveEnd) && result.size < remainingCount && result.size < MAX_OCCURRENCES_PER_RANGE) {
            result.add(current)
            current = current.plusDays(1)
        }
        return result
    }

    // ── WEEKLY ────────────────────────────────────────────────────────────────

    private fun weeklyDates(
        masterDate: LocalDate, rangeStart: LocalDate, rangeEnd: LocalDate, r: Recurrence,
    ): List<LocalDate> {
        val effectiveEnd = r.endDate?.let { minOf(rangeEnd, it) } ?: rangeEnd

        val first: LocalDate = if (rangeStart.isBefore(masterDate)) {
            masterDate
        } else {
            val daysDiff = ChronoUnit.DAYS.between(masterDate, rangeStart)
            val rem      = daysDiff % 7
            if (rem == 0L) rangeStart else rangeStart.plusDays(7 - rem)
        }
        if (first.isAfter(effectiveEnd)) return emptyList()

        val skipped        = ChronoUnit.WEEKS.between(masterDate, first).toInt()
        val remainingCount = r.count?.minus(skipped)?.coerceAtLeast(0) ?: Int.MAX_VALUE

        val result = mutableListOf<LocalDate>()
        var current = first
        while (!current.isAfter(effectiveEnd) && result.size < remainingCount && result.size < MAX_OCCURRENCES_PER_RANGE) {
            result.add(current)
            current = current.plusWeeks(1)
        }
        return result
    }

    // ── MONTHLY ───────────────────────────────────────────────────────────────

    private fun monthlyDates(
        masterDate: LocalDate, rangeStart: LocalDate, rangeEnd: LocalDate, r: Recurrence,
    ): List<LocalDate> {
        val effectiveEnd = r.endDate?.let { minOf(rangeEnd, it) } ?: rangeEnd
        val targetDay    = masterDate.dayOfMonth

        var first = if (rangeStart.isBefore(masterDate)) {
            masterDate
        } else {
            val inMonth = rangeStart.withDayOfMonth(minOf(targetDay, rangeStart.lengthOfMonth()))
            if (inMonth.isBefore(rangeStart)) nextMonthlyDate(inMonth, targetDay) else inMonth
        }
        if (first.isAfter(effectiveEnd)) return emptyList()

        val skipped        = ChronoUnit.MONTHS.between(masterDate, first).toInt()
        val remainingCount = r.count?.minus(skipped)?.coerceAtLeast(0) ?: Int.MAX_VALUE

        val result = mutableListOf<LocalDate>()
        while (!first.isAfter(effectiveEnd) && result.size < remainingCount && result.size < MAX_OCCURRENCES_PER_RANGE) {
            result.add(first)
            first = nextMonthlyDate(first, targetDay)
        }
        return result
    }

    private fun nextMonthlyDate(current: LocalDate, targetDay: Int): LocalDate {
        val next = current.plusMonths(1)
        return next.withDayOfMonth(minOf(targetDay, next.lengthOfMonth()))
    }

    // ── YEARLY ────────────────────────────────────────────────────────────────

    private fun yearlyDates(
        masterDate: LocalDate, rangeStart: LocalDate, rangeEnd: LocalDate, r: Recurrence,
    ): List<LocalDate> {
        val effectiveEnd = r.endDate?.let { minOf(rangeEnd, it) } ?: rangeEnd

        var first = if (rangeStart.isBefore(masterDate)) {
            masterDate
        } else {
            var candidate = yearlyOccurrence(masterDate, rangeStart.year)
            if (candidate.isBefore(rangeStart)) candidate = yearlyOccurrence(masterDate, rangeStart.year + 1)
            candidate
        }
        if (first.isAfter(effectiveEnd)) return emptyList()

        val skipped        = ChronoUnit.YEARS.between(masterDate, first).toInt()
        val remainingCount = r.count?.minus(skipped)?.coerceAtLeast(0) ?: Int.MAX_VALUE

        val result = mutableListOf<LocalDate>()
        while (!first.isAfter(effectiveEnd) && result.size < remainingCount && result.size < MAX_OCCURRENCES_PER_RANGE) {
            result.add(first)
            first = yearlyOccurrence(masterDate, first.year + 1)
        }
        return result
    }

    // Handles Feb 29 → Feb 28 in non-leap years via YearMonth.lengthOfMonth().
    private fun yearlyOccurrence(masterDate: LocalDate, year: Int): LocalDate {
        val maxDay = YearMonth.of(year, masterDate.monthValue).lengthOfMonth()
        return LocalDate.of(year, masterDate.monthValue, minOf(masterDate.dayOfMonth, maxDay))
    }

    // ── CUSTOM_DAYS ───────────────────────────────────────────────────────────

    private fun customDaysDates(
        masterDate: LocalDate, rangeStart: LocalDate, rangeEnd: LocalDate, r: Recurrence,
    ): List<LocalDate> {
        if (r.repeatDays.isEmpty()) return emptyList()
        val effectiveEnd   = r.endDate?.let { minOf(rangeEnd, it) } ?: rangeEnd
        val effectiveStart = maxOf(masterDate, rangeStart)
        if (effectiveStart.isAfter(effectiveEnd)) return emptyList()

        val skipped        = if (r.count != null) countOccurrences(r.repeatDays, masterDate, effectiveStart.minusDays(1)) else 0
        var remainingCount = r.count?.minus(skipped)?.coerceAtLeast(0) ?: Int.MAX_VALUE

        val result = mutableListOf<LocalDate>()
        var current = effectiveStart
        while (!current.isAfter(effectiveEnd) && result.size < MAX_OCCURRENCES_PER_RANGE) {
            if (current.dayOfWeek in r.repeatDays) {
                if (remainingCount <= 0) break
                result.add(current)
                remainingCount--
            }
            current = current.plusDays(1)
        }
        return result
    }

    // O(1) occurrence counter: full weeks × days-per-week + partial-week scan.
    private fun countOccurrences(days: Set<DayOfWeek>, from: LocalDate, to: LocalDate): Int {
        if (from.isAfter(to)) return 0
        val totalDays = ChronoUnit.DAYS.between(from, to) + 1
        val fullWeeks = totalDays / 7
        var count     = (fullWeeks * days.size).toInt()
        var current   = from.plusDays(fullWeeks * 7)
        repeat((totalDays % 7).toInt()) {
            if (current.dayOfWeek in days) count++
            current = current.plusDays(1)
        }
        return count
    }
}
