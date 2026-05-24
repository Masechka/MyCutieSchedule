package com.example.mycutieschedule.domain.usecase.share

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.repository.ShareRepository
import javax.inject.Inject

/**
 * Generates (or retrieves) a share code for the current user's schedule.
 * Returns the 6-character code the user can send to others.
 */
class ShareScheduleUseCase @Inject constructor(
    private val repository: ShareRepository,
) {
    suspend operator fun invoke(userId: String): Result<String> {
        if (userId.isBlank()) {
            return Result.Error(IllegalArgumentException("User ID cannot be empty"))
        }
        return repository.generateShareCode(userId)
    }
}
