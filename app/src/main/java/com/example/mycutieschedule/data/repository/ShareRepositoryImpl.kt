package com.example.mycutieschedule.data.repository

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.core.util.toDomainResult
import com.example.mycutieschedule.data.local.dao.EventDao
import com.example.mycutieschedule.data.local.dao.SharedScheduleDao
import com.example.mycutieschedule.data.mapper.toDomain
import com.example.mycutieschedule.data.mapper.toDto
import com.example.mycutieschedule.data.mapper.toEntity
import com.example.mycutieschedule.data.remote.api.FirestoreEventDataSource
import com.example.mycutieschedule.data.remote.api.FirestoreShareDataSource
import com.example.mycutieschedule.data.remote.api.FirestoreUserDataSource
import com.example.mycutieschedule.domain.model.SharedSchedule
import com.example.mycutieschedule.domain.repository.ShareRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject

/**
 * Implementation of [ShareRepository].
 *
 * **Share code generation:**
 * A 6-character uppercase alphanumeric code is derived from a UUID.
 * The code is stored in two places:
 *   1. `sharedSchedules/{code}` — for fast O(1) lookups by code.
 *   2. `users/{userId}.shareCode` — so the user can display their own code.
 *
 * **Importing a schedule:**
 * Events from the owner's Firestore subcollection are downloaded and stored in
 * Room with `isImported = true` and `sourceScheduleId = shareCode`.
 * The import is a snapshot — changes made by the owner after import are NOT
 * automatically reflected (a future sync feature can address this).
 *
 * **observeImportedSchedules:**
 * Combines two Room Flows — SharedScheduleEntity list + imported EventEntity list.
 * Events are grouped by `sourceScheduleId` and re-attached to their schedule.
 * This is a pure Room-backed live stream; no Firestore calls are made here.
 */
class ShareRepositoryImpl @Inject constructor(
    private val sharedScheduleDao: SharedScheduleDao,
    private val eventDao: EventDao,
    private val shareDataSource: FirestoreShareDataSource,
    private val userDataSource: FirestoreUserDataSource,
    private val eventDataSource: FirestoreEventDataSource,
) : ShareRepository {

    override suspend fun generateShareCode(userId: String): Result<String> = runCatching {
        // Return the existing code if the user already has one.
        val userResult = userDataSource.getUser(userId)
        val existingCode = (userResult as? Result.Success)?.data?.shareCode
        if (existingCode != null) return@runCatching existingCode

        // Generate a fresh 6-char code from a UUID (collision probability negligible).
        val code = UUID.randomUUID()
            .toString()
            .replace("-", "")
            .take(6)
            .uppercase()

        val ownerName = (userResult as? Result.Success)?.data?.displayName ?: ""

        // Write to Firestore atomically (best-effort — no transactions needed at this scale).
        shareDataSource.createShareDocument(code, userId, ownerName)
        userDataSource.updateShareCode(userId, code)

        code
    }.toDomainResult()

    override suspend fun getSharedSchedule(shareCode: String): Result<SharedSchedule> =
        runCatching {
            val shareDto = requireShareDto(shareCode)

            // One-shot read: collect the first emission from the snapshot Flow.
            // Firestore serves from local cache immediately, so this doesn't block.
            val events = eventDataSource
                .observeEvents(shareDto.userId)
                .first()
                .map { it.toDomain() }

            SharedSchedule(
                id         = shareCode,
                scheduleId = shareDto.userId,
                shareCode  = shareCode,
                ownerName  = shareDto.ownerName,
                events     = events,
                isImported = false,
            )
        }.toDomainResult()

    override suspend fun importSharedSchedule(shareCode: String, userId: String): Result<Unit> =
        runCatching {
            val shareDto = requireShareDto(shareCode)

            // Download the owner's events and mark them as imported.
            val importedEntities = eventDataSource
                .observeEvents(shareDto.userId)
                .first()
                .map { dto ->
                    dto.toDomain()
                        .copy(
                            isImported       = true,
                            sourceScheduleId = shareCode,
                        )
                        .toEntity()
                }
            eventDao.insertAll(importedEntities)

            // Save the SharedSchedule record so it appears in the imported list.
            val schedule = SharedSchedule(
                id         = shareCode,
                scheduleId = shareDto.userId,
                shareCode  = shareCode,
                ownerName  = shareDto.ownerName,
                isImported = true,
            )
            sharedScheduleDao.insert(schedule.toEntity(viewerId = userId))
        }.toDomainResult()

    override suspend fun removeImportedSchedule(scheduleId: String, userId: String): Result<Unit> =
        runCatching {
            // Delete all imported events that came from this schedule.
            eventDao.deleteBySourceScheduleId(scheduleId)
            // Delete the SharedSchedule record itself.
            sharedScheduleDao.deleteById(scheduleId)
        }.toDomainResult()

    /**
     * Combines two Room flows to build a live [SharedSchedule] list:
     *   - [SharedScheduleDao.observeAll] — schedule metadata
     *   - [EventDao.observeImportedEvents] — imported events, grouped by schedule
     *
     * [combine] re-emits whenever either upstream emits, keeping the list consistent.
     */
    override fun observeImportedSchedules(userId: String): Flow<List<SharedSchedule>> =
        combine(
            sharedScheduleDao.observeAll(userId),
            eventDao.observeImportedEvents(userId),
        ) { scheduleEntities, importedEventEntities ->
            val eventsByShareCode = importedEventEntities
                .map { it.toDomain() }
                .groupBy { it.sourceScheduleId }

            scheduleEntities.map { entity ->
                entity.toDomain(
                    events = eventsByShareCode[entity.shareCode] ?: emptyList(),
                )
            }
        }

    override suspend fun revokeShareCode(userId: String): Result<Unit> = runCatching {
        val userResult = userDataSource.getUser(userId)
        val shareCode = (userResult as? Result.Success)?.data?.shareCode
        if (shareCode != null) {
            shareDataSource.deleteShareDocument(userId, shareCode)
            userDataSource.updateShareCode(userId, null)
        }
    }.toDomainResult()

    // ── Internal helpers ──────────────────────────────────────────────────────

    private suspend fun requireShareDto(shareCode: String) =
        (shareDataSource.getByShareCode(shareCode) as? Result.Success)?.data
            ?: error("Share code '$shareCode' not found.")
}
