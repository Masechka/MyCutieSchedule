package com.example.mycutieschedule.domain.usecase.event

import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalTime
import java.time.YearMonth
import javax.inject.Inject

class GetEventsForMonthUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    /**
     * Returns a live stream of events for the given [yearMonth].
     * The range is the 1st of the month at 00:00 through the last day at 23:59:59.
     */
    operator fun invoke(yearMonth: YearMonth, userId: String): Flow<List<Event>> =
        repository.observeEventsBetween(
            startDateTime = yearMonth.atDay(1).atStartOfDay(),
            endDateTime   = yearMonth.atEndOfMonth().atTime(LocalTime.MAX),
            userId        = userId,
        )
}
