package com.example.mycutieschedule.domain.usecase.event

import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

class GetEventsForMonthUseCase @Inject constructor(
    private val repository: EventRepository,
    private val expander: RecurrenceExpander,
) {
    /**
     * Returns a live stream of events for the given [yearMonth].
     * The range is the 1st of the month at 00:00 through the last day at 23:59:59.
     *
     * @param userId Owner of the events to load.
     */
    operator fun invoke(yearMonth: YearMonth, userId: String): Flow<List<Event>> {
        val rangeStart = yearMonth.atDay(1).atStartOfDay()
        val rangeEnd   = yearMonth.atEndOfMonth().atTime(LocalTime.MAX)
        return combine(
            repository.observeEventsBetween(rangeStart, rangeEnd, userId)
                .map { events -> events.filter { it.recurrence == null } },
            repository.observeRecurringMasters(userId),
        ) { nonRecurring, masters ->
            val expanded = masters.flatMap { expander.expand(it, rangeStart, rangeEnd) }
            (nonRecurring + expanded).sortedBy { it.startDateTime }
        }
    }
}
