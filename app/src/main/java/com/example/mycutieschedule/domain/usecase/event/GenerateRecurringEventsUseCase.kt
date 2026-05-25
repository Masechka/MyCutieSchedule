package com.example.mycutieschedule.domain.usecase.event

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.model.Recurrence
import com.example.mycutieschedule.domain.model.RecurrenceType
import com.example.mycutieschedule.domain.repository.EventRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

private val HORIZON_MONTHS = 6L   // generate up to 6 months ahead of the master
private const val MAX_OCCURRENCES = 500  // hard cap per series

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
 * The master's own start date is excluded — it already exists as its own event.
 *
 * Call after [com.example.mycutieschedule.domain.usecase.event.AddEventUseCase] succeeds.
 */
class GenerateRecurringEventsUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    suspend operator fun invoke(masterEvent: Event): Result<Unit> {
        val recurrence = masterEvent.recurrence ?: return Result.Success(Unit)
        val groupId    = masterEvent.recurrenceGroupId ?: return Result.Success(Unit)

        val occurrences = buildOccurrences(masterEvent, recurrence, groupId)
        if (occurrences.isEmpty()) return Result.Success(Unit)

        return repository.addAllEvents(occurrences)
    }

    // ── Occurrence builder ────────────────────────────────────────────────────

    private fun buildOccurrences(
        master: Event,
        recurrence: Recurrence,
        groupId: String,
    ): List<Event> {
        val masterDate     = master.startDateTime.toLocalDate()
        val horizon        = masterDate.plusMonths(HORIZON_MONTHS)
        val seriesEnd      = recurrence.endDate?.let { if (it.isBefore(horizon)) it else horizon } ?: horizon
        val durationMins   = ChronoUnit.MINUTES.between(master.startDateTime, master.endDateTime)
        val masterTime     = master.startDateTime.toLocalTime()
        val now            = LocalDateTime.now()

        val dates: List<LocalDate> = when (recurrence.type) {
            RecurrenceType.DAILY       -> dailyDates(masterDate, seriesEnd)
            RecurrenceType.WEEKLY      -> weeklyDates(masterDate, seriesEnd, recurrence.intervalWeeks.coerceAtLeast(1))
            RecurrenceType.MONTHLY     -> monthlyDates(masterDate, seriesEnd)
            RecurrenceType.YEARLY      -> yearlyDates(masterDate, seriesEnd)
            RecurrenceType.CUSTOM_DAYS -> customDaysDates(masterDate, seriesEnd, recurrence)
            RecurrenceType.CUSTOM_DATES -> recurrence.customDates.filter { it.isAfter(masterDate) }
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

    private fun weeklyDates(masterDate: LocalDate, seriesEnd: LocalDate, step: Int): List<LocalDate> {
        val result  = mutableListOf<LocalDate>()
        var current = masterDate.plusWeeks(step.toLong())
        while (!current.isAfter(seriesEnd) && result.size < MAX_OCCURRENCES) {
            result.add(current)
            current = current.plusWeeks(step.toLong())
        }
        return result
    }

    private fun monthlyDates(masterDate: LocalDate, seriesEnd: LocalDate): List<LocalDate> {
        val targetDay = masterDate.dayOfMonth
        val result    = mutableListOf<LocalDate>()
        var current   = masterDate.plusMonths(1).let { it.withDayOfMonth(minOf(targetDay, it.lengthOfMonth())) }
        while (!current.isAfter(seriesEnd) && result.size < MAX_OCCURRENCES) {
            result.add(current)
            current = current.plusMonths(1).let { it.withDayOfMonth(minOf(targetDay, it.lengthOfMonth())) }
        }
        return result
    }

    private fun yearlyDates(masterDate: LocalDate, seriesEnd: LocalDate): List<LocalDate> {
        val result  = mutableListOf<LocalDate>()
        var year    = masterDate.year + 1
        while (result.size < MAX_OCCURRENCES) {
            val maxDay = YearMonth.of(year, masterDate.monthValue).lengthOfMonth()
            val date   = LocalDate.of(year, masterDate.monthValue, minOf(masterDate.dayOfMonth, maxDay))
            if (date.isAfter(seriesEnd)) break
            result.add(date)
            year++
        }
        return result
    }

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
