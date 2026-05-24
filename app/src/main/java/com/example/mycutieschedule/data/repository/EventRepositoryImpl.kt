package com.example.mycutieschedule.data.repository

import com.example.mycutieschedule.core.extensions.toEpochMilli
import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.core.util.toDomainResult
import com.example.mycutieschedule.data.local.dao.EventDao
import com.example.mycutieschedule.data.mapper.toDomain
import com.example.mycutieschedule.data.mapper.toEntity
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Offline-first implementation of [EventRepository].
 *
 * Read path:  Room → immediate local data → UI is always responsive.
 * Write path: Room first (instant feedback) → Firestore in background (TODO Stage 5).
 *
 * Firestore sync will be added in Stage 5 (remote data layer).
 * At that point this class will receive a FirestoreEventDataSource dependency
 * and call it after every Room write.
 */
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
) : EventRepository {

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

    override suspend fun addEvent(event: Event): Result<Unit> = runCatching {
        eventDao.insert(event.toEntity())
        // TODO Stage 5: firestoreDataSource.saveEvent(event)
    }.toDomainResult()

    override suspend fun updateEvent(event: Event): Result<Unit> = runCatching {
        eventDao.update(event.toEntity())
        // TODO Stage 5: firestoreDataSource.saveEvent(event)
    }.toDomainResult()

    override suspend fun deleteEvent(eventId: String): Result<Unit> = runCatching {
        eventDao.deleteById(eventId)
        // TODO Stage 5: firestoreDataSource.deleteEvent(eventId)
    }.toDomainResult()
}
