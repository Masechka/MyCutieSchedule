package com.example.mycutieschedule.domain.usecase.event

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.model.Recurrence
import com.example.mycutieschedule.domain.model.RecurrenceType
import com.example.mycutieschedule.domain.repository.EventRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ── Generation horizons — tuned per recurrence type ───────────────────────────
// Shorter horizons for high-frequency types keep DB/Firebase usage proportional
// to the actual scheduling density while still providing a comfortable look-ahead.

/** Daily recurrence: next 3 months (~90 events max). */
private const val HORIZON_DAILY_MONTHS = 3L

/** Weekly / custom-days recurrence: next 6 months (~26 events max per weekday). */
private const val HORIZON_WEEKLY_MONTHS = 6L

/** Monthly, yearly, and custom-date recurrence: next 12 months. */
private const val HORIZON_DEFAULT_MONTHS = 12L

/** Absolute safety cap — no series ever exceeds this many generated rows. */
private const val MAX_OCCURRENCES = 500

/**
 * Generates real database rows for every future occurrence of a recurring [masterEvent].
 *
 * Each occurrence:
 * - has a unique UUID
 * - shares [Event.recurrenceGroupId] with the master
 * - has [Event.isRecurrenceGenerated] = true
 * - copies title, description, color, reminder, recurrence from the master
 * - adjusts startDateTime / endDateTime to the occurrence date (same time, same duration)
 *
 * The master's own start date is excluded — it already exists as its own row.
 *
 * The date-generation work runs on [Dispatchers.Default] to keep the Main thread free;
 * the subsequent [EventRepository.addAllEvents] call handles its own IO dispatching.
 *
 * Call after [AddEventUseCase] or [UpdateEventUseCase] succeeds.
 * Before calling during an update, delete stale occurrences via [DeleteRecurringEventUseCase].
 */
class GenerateRecurringEventsUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    suspend operator fun invoke(masterEvent: Event): Result<Unit> {
        val recurrence = masterEvent.recurrence ?: return Result.Success(Unit)
        val groupId    = masterEvent.recurrenceGroupId ?: return Result.Success(Unit)

        // Move CPU-bound date generation off the calling dispatcher (typically Main).
        val occurrences = withContext(Dispatchers.Default) {
            buildOccurrences(masterEvent, recurrence, groupId)
        }
        if (occurrences.isEmpty()) return Result.Success(Unit)

        return repository.addAllEvents(occurrences)
    }

    // ── Occurrence builder ────────────────────────────────────────────────────

    private fun buildOccurrences(
        master: Event,
        recurrence: Recurrence,
        groupId: String,
    ): List<Event> {
        val masterDate   = master.startDateTime.toLocalDate()
        val horizonMonths = horizonFor(recurrence.type)
        val horizon      = masterDate.plusMonths(horizonMonths)
        val seriesEnd    = recurrence.endDate?.let { if (it.isBefore(horizon)) it else horizon } ?: horizon
        val durationMins = ChronoUnit.MINUTES.between(master.startDateTime, master.endDateTime)
        val masterTime   = master.startDateTime.toLocalTime()
        val now          = LocalDateTime.now()

        val dates: List<LocalDate> = when (recurrence.type) {
            RecurrenceType.DAILY  -> dailyDates(masterDate, seriesEnd)

            RecurrenceType.WEEKLY -> {
                val step = recurrence.intervalWeeks.coerceAtLeast(1)
                if (recurrence.repeatDays.isEmpty()) {
                    weeklyDates(masterDate, seriesEnd, step)
                } else {
                    weeklyDatesWithDays(masterDate, seriesEnd, step, recurrence.repeatDays)
                }
            }

            RecurrenceType.MONTHLY     -> monthlyDates(masterDate, seriesEnd)
            RecurrenceType.YEARLY      -> yearlyDates(masterDate, seriesEnd)

            // Legacy — kept for backward-compat with existing DB rows.
            RecurrenceType.CUSTOM_DAYS -> customDaysDates(masterDate, seriesEnd, recurrence)

            RecurrenceType.CUSTOM_DATES ->
                recurrence.customDates.filter { it.isAfter(masterDate) }
        }

        return dates.take(MAX_OCCURRENCES).map { date ->
            val start = date.atTime(masterTime)
            Event(
                id                    = UUID.randomUUID().toString(),
                title                 = master.title,
                description           = master.description,
                startDateTime         = start,
                endDateTime           = start.plusMinutes(durationMins),
                color                 = master.color,
                reminder              = master.reminder,
                recurrence            = master.recurrence,
                ownerId               = master.ownerId,
                recurrenceGroupId     = groupId,
                isRecurrenceGenerated = true,
                createdAt             = now,
                updatedAt             = now,
            )
        }
    }

    /** Returns the generation horizon in months for the given [RecurrenceType]. */
    private fun horizonFor(type: RecurrenceType): Long = when (type) {
        RecurrenceType.DAILY                   -> HORIZON_DAILY_MONTHS
        RecurrenceType.WEEKLY,
        RecurrenceType.CUSTOM_DAYS             -> HORIZON_WEEKLY_MONTHS
        RecurrenceType.MONTHLY,
        RecurrenceType.YEARLY,
        RecurrenceType.CUSTOM_DATES            -> HORIZON_DEFAULT_MONTHS
    }

    // ── Date generators (always exclude the master date itself) ───────────────

    private fun dailyDates(masterDate: LocalDate, seriesEnd: LocalDate): List<LocalDate> {
        val result  = mutableListOf<LocalDate>()
        var current = masterDate.plusDays(1)
        while (!current.isAfter(seriesEnd) && result.size < MAX_OCCURRENCES) {
            result.add(current)
            current = current.plusDays(1)
        }
        return result
    }

    /** Weekly on the same weekday as [masterDate], stepping [step] weeks. */
    private fun weeklyDates(masterDate: LocalDate, seriesEnd: LocalDate, step: Int): List<LocalDate> {
        val result  = mutableListOf<LocalDate>()
        var current = masterDate.plusWeeks(step.toLong())
        while (!current.isAfter(seriesEnd) && result.size < MAX_OCCURRENCES) {
            result.add(current)
            current = current.plusWeeks(step.toLong())
        }
        return result
    }

    /**
     * Weekly on [repeatDays] of the week, advancing [step] weeks at a time.
     *
     * Starting from the Monday of the first eligible week (master's week + step),
     * collects each selected weekday in chronological order. Respects both [seriesEnd]
     * and [MAX_OCCURRENCES].
     */
    private fun weeklyDatesWithDays(
        masterDate: LocalDate,
        seriesEnd: LocalDate,
        step: Int,
        repeatDays: Set<DayOfWeek>,
    ): List<LocalDate> {
        val masterDowValue = masterDate.dayOfWeek.value           // 1 = Mon … 7 = Sun
        val masterWeekMon  = masterDate.minusDays((masterDowValue - 1).toLong())

        val result  = mutableListOf<LocalDate>()
        var weekMon = masterWeekMon.plusWeeks(step.toLong())

        while (result.size < MAX_OCCURRENCES) {
            if (weekMon.isAfter(seriesEnd)) break
            repeatDays.sortedBy { it.value }.forEach { dow ->
                val date = weekMon.plusDays((dow.value - 1).toLong())
                if (date.isAfter(masterDate) && !date.isAfter(seriesEnd)
                    && result.size < MAX_OCCURRENCES
                ) {
                    result.add(date)
                }
            }
            weekMon = weekMon.plusWeeks(step.toLong())
        }
        return result
    }

    private fun monthlyDates(masterDate: LocalDate, seriesEnd: LocalDate): List<LocalDate> {
        val targetDay = masterDate.dayOfMonth
        val result    = mutableListOf<LocalDate>()
        var current   = masterDate.plusMonths(1)
            .let { it.withDayOfMonth(minOf(targetDay, it.lengthOfMonth())) }
        while (!current.isAfter(seriesEnd) && result.size < MAX_OCCURRENCES) {
            result.add(current)
            current = current.plusMonths(1)
                .let { it.withDayOfMonth(minOf(targetDay, it.lengthOfMonth())) }
        }
        return result
    }

    private fun yearlyDates(masterDate: LocalDate, seriesEnd: LocalDate): List<LocalDate> {
        val result = mutableListOf<LocalDate>()
        var year   = masterDate.year + 1
        while (result.size < MAX_OCCURRENCES) {
            val maxDay = YearMonth.of(year, masterDate.monthValue).lengthOfMonth()
            val date   = LocalDate.of(year, masterDate.monthValue, minOf(masterDate.dayOfMonth, maxDay))
            if (date.isAfter(seriesEnd)) break
            result.add(date)
            year++
        }
        return result
    }

    /** Legacy — kept for backward-compat with CUSTOM_DAYS rows in the DB. */
    private fun customDaysDates(
        masterDate: LocalDate,
        seriesEnd: LocalDate,
        recurrence: Recurrence,
    ): List<LocalDate> {
        if (recurrence.repeatDays.isEmpty()) return emptyList()
        val result  = mutableListOf<LocalDate>()
        var current = masterDate.plusDays(1)
        while (!current.isAfter(seriesEnd) && result.size < MAX_OCCURRENCES) {
            if (current.dayOfWeek in recurrence.repeatDays) result.add(current)
            current = current.plusDays(1)
        }
        return result
    }
}
