package com.example.mycutieschedule.domain.usecase.event

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.repository.EventRepository
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Deletes [fromStart] and every subsequent event that shares [recurrenceGroupId].
 *
 * "Subsequent" means [Event.startDateTime] >= [fromStart] within the same recurrence chain.
 * This intentionally leaves any earlier occurrences intact so the user can keep past history.
 */
class DeleteRecurringEventUseCase @Inject constructor(
    private val repository: EventRepository,
) {
    suspend operator fun invoke(
        recurrenceGroupId: String,
        fromStart: LocalDateTime,
    ): Result<Unit> = repository.deleteEventAndFollowing(recurrenceGroupId, fromStart)
}
