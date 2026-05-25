package com.example.mycutieschedule.data.repository

import com.example.mycutieschedule.core.extensions.toEpochMilli
import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.core.util.toDomainResult
import com.example.mycutieschedule.data.local.dao.EventDao
import com.example.mycutieschedule.data.mapper.toDomain
import com.example.mycutieschedule.data.mapper.toDto
import com.example.mycutieschedule.data.mapper.toEntity
import com.example.mycutieschedule.data.remote.api.FirestoreEventDataSource
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Offline-first implementation of [EventRepository].
 *
 * **Read path:** Room only — always instant, always available offline.
 *   [EventSyncManager] keeps Room up-to-date from Firestore in the background.
 *
 * **Write path:** Room first (instant UI feedback), then Firestore best-effort.
 *   If the Firestore write fails, the data stays in Room and will be uploaded
 *   the next time [EventSyncManager] successfully syncs.
 */
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val eventDataSource: FirestoreEventDataSource,
) : EventRepository {

    // ── Read operations ───────────────────────────────────────────────────────

    override fun observeEventsBetween(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        userId: String,
    ): Flow<List<Event>> = eventDao
        .observeEventsBetween(
            startEpochMilli = startDateTime.toEpochMilli(),
            endEpochMilli   = endDateTime.toEpochMilli(),
            userId          = userId,
        )
        .map { entities -> entities.map { it.toDomain() } }

    override suspend fun getEventById(eventId: String): Event? =
        eventDao.findById(eventId)?.toDomain()

    // ── Write operations ──────────────────────────────────────────────────────

    override suspend fun addEvent(event: Event): Result<Unit> = runCatching {
        eventDao.insert(event.toEntity())
        writeToFirestore(event)
    }.toDomainResult()

    override suspend fun addAllEvents(events: List<Event>): Result<Unit> = runCatching {
        eventDao.insertAll(events.map { it.toEntity() })
        events.forEach { writeToFirestore(it) }
    }.toDomainResult()

    override suspend fun updateEvent(event: Event): Result<Unit> = runCatching {
        eventDao.update(event.toEntity())
        writeToFirestore(event)
    }.toDomainResult()

    override suspend fun deleteEvent(eventId: String): Result<Unit> = runCatching {
        val entity = eventDao.findById(eventId)
        eventDao.deleteById(eventId)
        if (entity != null) deleteFromFirestore(eventId, entity.ownerId)
    }.toDomainResult()

    override suspend fun deleteEventAndFollowing(
        recurrenceGroupId: String,
        fromStart: LocalDateTime,
    ): Result<Unit> = runCatching {
        val fromMilli  = fromStart.toEpochMilli()
        val toDelete   = eventDao.findByGroupIdFromDate(recurrenceGroupId, fromMilli)
        eventDao.deleteByGroupIdFromDate(recurrenceGroupId, fromMilli)
        toDelete.forEach { entity -> deleteFromFirestore(entity.id, entity.ownerId) }
    }.toDomainResult()

    // ── Firestore helpers ─────────────────────────────────────────────────────

    private suspend fun writeToFirestore(event: Event) {
        val result = eventDataSource.saveEvent(event.toDto(), event.ownerId)
        if (result is Result.Error) {
            Timber.w(
                result.exception,
                "Firestore write failed for event id=${event.id}. " +
                    "EventSyncManager will reconcile on next connection.",
            )
        }
    }

    private suspend fun deleteFromFirestore(eventId: String, ownerId: String) {
        val result = eventDataSource.deleteEvent(eventId, ownerId)
        if (result is Result.Error) {
            Timber.w(result.exception, "Firestore delete failed for event id=$eventId.")
        }
    }
}
