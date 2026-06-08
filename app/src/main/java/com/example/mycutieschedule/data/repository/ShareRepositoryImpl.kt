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
 * Реализация [ShareRepository] — функциональность общих (расшаренных)
 * расписаний.
 *
 * **Генерация share-кода:**
 * 6-символьный буквенно-цифровой код в верхнем регистре, получаемый из UUID.
 * Код хранится в двух местах:
 *   1. `sharedSchedules/{code}` — для быстрого O(1) поиска по коду.
 *   2. `users/{userId}.shareCode` — чтобы владелец мог видеть свой код
 *      в профиле и копировать его.
 *
 * **Импорт расписания:**
 * События из event-subcollection владельца скачиваются и сохраняются в
 * локальной Room-БД с метками `isImported = true` и
 * `sourceScheduleId = shareCode`.
 * Кроме того, `ownerId` ПЕРЕЗАПИСЫВАЕТСЯ на ID viewer'а (того, кто
 * импортирует) — это критически важно, потому что все календарные запросы
 * фильтруют по `owner_id = currentUserId`. Без этого импортированные
 * события не появлялись бы в основном календаре viewer'а.
 *
 * После импорта живая синхронизация изменений владельца ведётся отдельным
 * сервисом — [com.example.mycutieschedule.data.sync.SharedSchedulesSyncManager].
 *
 * **observeImportedSchedules:**
 * Объединяет два Flow из Room: список `SharedScheduleEntity` (метаданные) +
 * список импортированных `EventEntity`. События группируются по
 * `sourceScheduleId` и прикрепляются к соответствующему расписанию.
 * Это чистый поток на основе Room, без обращений к Firestore — данные
 * актуальны столько, насколько свеж локальный кэш (обновляется
 * SharedSchedulesSyncManager'ом).
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
            //
            // CRITICAL: override ownerId to the *viewer's* userId so the events
            // surface in the viewer's calendar queries (which all filter by
            // owner_id = :userId). Without this rewrite, imported events would
            // be stored under the owner's userId and become invisible to the
            // viewer — matching the spec that imported events are "copied into
            // the user's own calendar, visually distinguished but editable".
            //
            // The original owner is still reachable via sourceScheduleId →
            // sharedSchedules/{shareCode}.userId, and is_imported = true keeps
            // them out of the Firestore sync write path (so they never leak
            // into the viewer's own Firestore subcollection).
            val importedEntities = eventDataSource
                .observeEvents(shareDto.userId)
                .first()
                .map { dto ->
                    dto.toDomain()
                        .copy(
                            ownerId          = userId,
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

    override suspend fun updateImportedScheduleAlias(
        scheduleId: String,
        alias: String?,
    ): Result<Unit> = runCatching {
        // Normalize: blank → null, so the UI fallback to ownerName kicks in.
        val normalized = alias?.trim()?.takeIf { it.isNotBlank() }
        sharedScheduleDao.updateAlias(scheduleId, normalized)
    }.toDomainResult()

    override suspend fun setImportedScheduleVisible(
        scheduleId: String,
        visible: Boolean,
    ): Result<Unit> = runCatching {
        sharedScheduleDao.updateVisibility(scheduleId, visible)
    }.toDomainResult()

    override suspend fun setImportedScheduleIncludeInWorkload(
        scheduleId: String,
        include: Boolean,
    ): Result<Unit> = runCatching {
        sharedScheduleDao.updateIncludeInWorkload(scheduleId, include)
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
