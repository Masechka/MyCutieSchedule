package com.example.mycutieschedule.domain.usecase.share

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.SharedSchedule
import com.example.mycutieschedule.domain.repository.ShareRepository
import javax.inject.Inject

/**
 * Fetches a shared schedule by its share code.
 * Used when the user enters a code on the Share screen to preview it before importing.
 */
class GetSharedScheduleUseCase @Inject constructor(
    private val repository: ShareRepository,
) {
    suspend operator fun invoke(shareCode: String): Result<SharedSchedule> {
        if (shareCode.isBlank()) {
            return Result.Error(IllegalArgumentException("Share code cannot be empty"))
        }
        return repository.getSharedSchedule(shareCode.trim().uppercase())
    }
}
