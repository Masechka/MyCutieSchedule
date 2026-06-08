package com.example.mycutieschedule.domain.usecase.event

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.repository.EventRepository
import java.util.UUID
import javax.inject.Inject

class AddEventUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    suspend operator fun invoke(event: Event): Result<Unit> {
        if (event.title.isBlank()) {
            return Result.Error(IllegalArgumentException("Event title cannot be empty"))
        }
        if (!event.startDateTime.isBefore(event.endDateTime)) {
            return Result.Error(
                IllegalArgumentException("Start time must be before end time")
            )
        }

        // Assign a new ID if the caller didn't provide one
        val eventToSave = if (event.id.isBlank()) {
            event.copy(id = UUID.randomUUID().toString())
        } else {
            event
        }

        return repository.addEvent(eventToSave)
    }
}
