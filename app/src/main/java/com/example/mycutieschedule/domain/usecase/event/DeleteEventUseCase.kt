package com.example.mycutieschedule.domain.usecase.event

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.repository.EventRepository
import javax.inject.Inject

class DeleteEventUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    suspend operator fun invoke(eventId: String): Result<Unit> {
        if (eventId.isBlank()) {
            return Result.Error(IllegalArgumentException("Event ID cannot be empty"))
        }
        return repository.deleteEvent(eventId)
    }
}
