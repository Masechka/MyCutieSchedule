package com.example.mycutieschedule.domain.usecase.event

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.repository.EventRepository
import java.time.LocalDateTime
import javax.inject.Inject

class UpdateEventUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    suspend operator fun invoke(event: Event): Result<Unit> {
        if (event.id.isBlank()) {
            return Result.Error(IllegalArgumentException("Cannot update event without an ID"))
        }
        if (event.title.isBlank()) {
            return Result.Error(IllegalArgumentException("Event title cannot be empty"))
        }
        if (!event.startDateTime.isBefore(event.endDateTime)) {
            return Result.Error(
                IllegalArgumentException("Start time must be before end time")
            )
        }

        // Always refresh updatedAt on every save
        val eventToUpdate = event.copy(updatedAt = LocalDateTime.now())
        return repository.updateEvent(eventToUpdate)
    }
}
