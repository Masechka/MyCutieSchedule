package com.example.mycutieschedule.domain.usecase.event

import com.example.mycutieschedule.core.extensions.toWeekEnd
import com.example.mycutieschedule.core.extensions.toWeekStart
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class GetEventsForWeekUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    /**
     * Returns a live stream of events for the week containing [weekDate].
     * The week always starts on Monday and ends on Sunday.
     *
     * @param weekDate Any date within the desired week.
     * @param userId   Owner of the events to load.
     */
    operator fun invoke(weekDate: LocalDate, userId: String): Flow<List<Event>> {
        val weekStart = weekDate.toWeekStart()
        val weekEnd   = weekDate.toWeekEnd()
        return repository.observeEventsBetween(
            startDateTime = weekStart.atStartOfDay(),
            endDateTime   = weekEnd.atTime(LocalTime.MAX),
            userId        = userId,
        )
    }
}
