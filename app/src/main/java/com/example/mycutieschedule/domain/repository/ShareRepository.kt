package com.example.mycutieschedule.domain.repository

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.SharedSchedule
import kotlinx.coroutines.flow.Flow

/**
 * Contract for schedule sharing operations.
 *
 * Implementation: data/repository/ShareRepositoryImpl.kt
 * Backend: Firestore collection sharedSchedules/{shareCode}
 */
interface ShareRepository {

    /**
     * Generates (or returns an existing) share code for [userId]'s schedule.
     * Creates a Firestore document under sharedSchedules/{code} pointing to the user's events.
     *
     * @return [Result.Success] with the 6-character alphanumeric share code.
     */
    suspend fun generateShareCode(userId: String): Result<String>

    /**
     * Looks up a shared schedule by its [shareCode].
     * Downloads the owner's events and returns a [SharedSchedule] snapshot.
     */
    suspend fun getSharedSchedule(shareCode: String): Result<SharedSchedule>

    /**
     * Saves a [SharedSchedule] to the current user's local Room DB and Firestore.
     * Events are stored with [Event.isImported] = true and a gray-pink display color.
     */
    suspend fun importSharedSchedule(shareCode: String, userId: String): Result<Unit>

    /**
     * Removes an imported schedule and all its events from local storage.
     * Does NOT affect the original owner's data.
     */
    suspend fun removeImportedSchedule(scheduleId: String, userId: String): Result<Unit>

    /**
     * Updates the viewer's local alias for an imported schedule.
     * Local-only: the original owner's display name is unaffected.
     * Pass [alias] = null (or blank) to clear the alias and revert to the owner name.
     */
    suspend fun updateImportedScheduleAlias(scheduleId: String, alias: String?): Result<Unit>

    /**
     * Live stream of all schedules the current user has imported or is viewing.
     */
    fun observeImportedSchedules(userId: String): Flow<List<SharedSchedule>>

    /**
     * Disables the share link for [userId]'s schedule.
     * Other users who already imported events keep their copies.
     */
    suspend fun revokeShareCode(userId: String): Result<Unit>
}
