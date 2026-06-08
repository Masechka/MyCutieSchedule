package com.example.mycutieschedule.domain.usecase.share

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.repository.ShareRepository
import javax.inject.Inject

/**
 * Copies events from another user's shared schedule into the current user's calendar.
 * Imported events are stored with [Event.isImported] = true and displayed in gray-pink.
 */
class ImportSharedScheduleUseCase @Inject constructor(
    private val repository: ShareRepository,
) {
    suspend operator fun invoke(shareCode: String, userId: String): Result<Unit> {
        if (shareCode.isBlank()) {
            return Result.Error(IllegalArgumentException("Share code cannot be empty"))
        }
        if (userId.isBlank()) {
            return Result.Error(IllegalArgumentException("User ID cannot be empty"))
        }
        return repository.importSharedSchedule(shareCode.trim().uppercase(), userId)
    }
}
