package com.example.mycutieschedule.domain.usecase.event

import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class GetEventsForDayUseCase @Inject constructor(
    private val repository: EventRepository,
    private val expander: RecurrenceExpander,
) {
    operator fun invoke(date: LocalDate, userId: String): Flow<List<Event>> {
        val rangeStart = date.atStartOfDay()
        val rangeEnd   = date.atTime(LocalTime.MAX)
        return combine(
            // Non-recurring events that physically overlap the day.
            repository.observeEventsBetween(rangeStart, rangeEnd, userId)
                .map { events -> events.filter { it.recurrence == null } },
            // All recurring masters — expanded into the day range by RecurrenceExpander.
            repository.observeRecurringMasters(userId),
        ) { nonRecurring, masters ->
            val expanded = masters.flatMap { expander.expand(it, rangeStart, rangeEnd) }
            (nonRecurring + expanded).sortedBy { it.startDateTime }
        }
    }
}
