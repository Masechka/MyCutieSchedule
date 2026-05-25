package com.example.mycutieschedule.domain.model

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Recurrence rule for a repeating event.
 *
 * @param type          How often the event repeats.
 * @param intervalWeeks Week step for [RecurrenceType.WEEKLY] (1 = every week, 2 = every 2 weeks, …).
 * @param repeatDays    Specific weekdays (used only for [RecurrenceType.WEEKLY] and [RecurrenceType.CUSTOM_DAYS]).
 * @param customDates   Manually selected arbitrary dates (used only for [RecurrenceType.CUSTOM_DATES]).
 * @param endDate       The last date on which the event may occur (inclusive). Null = no end.
 * @param count         Maximum number of occurrences. Null = unlimited.
 *                      If both [endDate] and [count] are set, whichever triggers first wins.
 */
data class Recurrence(
    val type: RecurrenceType,
    val intervalWeeks: Int = 1,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val customDates: List<LocalDate> = emptyList(),
    val endDate: LocalDate? = null,
    val count: Int? = null,
)

enum class RecurrenceType {
    DAILY,
    WEEKLY,
    MONTHLY,
    /** Legacy — kept for backward-compatibility with existing data. Not offered in the UI. */
    YEARLY,
    /** Repeats every week on explicitly selected weekdays. Uses [Recurrence.repeatDays]. */
    CUSTOM_DAYS,
    /** Repeats on manually chosen arbitrary dates. Uses [Recurrence.customDates]. */
    CUSTOM_DATES,
}
