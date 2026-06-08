package com.example.mycutieschedule.domain.usecase.share

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.repository.ShareRepository
import javax.inject.Inject

/**
 * Removes a previously imported schedule and all its events from the user's calendar.
 * Does not affect the original owner's schedule.
 */
class RemoveImportedScheduleUseCase @Inject constructor(
    private val repository: ShareRepository,
) {
    suspend operator fun invoke(scheduleId: String, userId: String): Result<Unit> {
        if (scheduleId.isBlank()) {
            return Result.Error(IllegalArgumentException("Schedule ID cannot be empty"))
        }
        return repository.removeImportedSchedule(scheduleId, userId)
    }
}
