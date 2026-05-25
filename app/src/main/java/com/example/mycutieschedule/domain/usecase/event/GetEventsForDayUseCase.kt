package com.example.mycutieschedule.domain.usecase.event

import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

class GetEventsForDayUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    operator fun invoke(date: LocalDate, userId: String): Flow<List<Event>> =
        repository.observeEventsBetween(
            startDateTime = date.atStartOfDay(),
            endDateTime   = date.atTime(LocalTime.MAX),
            userId        = userId,
        )
}
