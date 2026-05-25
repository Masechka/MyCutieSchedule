package com.example.mycutieschedule.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Recurrence rule for a repeating event.
 *
 * @param type       How often the event repeats.
 * @param repeatDays Specific days of the week (used only when [type] = [RecurrenceType.WEEKLY]).
 *                   E.g. setOf(MONDAY, WEDNESDAY, FRIDAY) for an event on those days each week.
 * @param endDate    The last date on which the event may occur (inclusive). Null = no end.
 * @param count      Maximum number of occurrences. Null = unlimited.
 *                   If both [endDate] and [count] are set, whichever triggers first wins.
 */
data class Recurrence(
    val type: RecurrenceType,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val endDate: LocalDate? = null,
    val count: Int? = null,
)

enum class RecurrenceType {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    /** Repeats on explicitly selected weekdays (Mon/Wed/Fri, etc.). Uses [Recurrence.repeatDays]. */
    CUSTOM_DAYS,
}
