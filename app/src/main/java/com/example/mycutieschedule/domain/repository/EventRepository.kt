package com.example.mycutieschedule.domain.repository

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.Event
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Contract for event CRUD and observation.
 *
 * Implementation: data/repository/EventRepositoryImpl.kt
 * Strategy: write-through — writes go to both Room and Firestore;
 *           reads serve from Room (offline-first), Firestore syncs in background.
 */
interface EventRepository {

    /**
     * Returns a live stream of events whose start or end overlaps [startDateTime]..[endDateTime].
     * Room re-emits whenever the underlying data changes.
     *
     * @param userId Filter to only this user's own events (excludes imported).
     *               Pass null to include imported events as well.
     */
    fun observeEventsBetween(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        userId: String,
    ): Flow<List<Event>>

    /** Returns a single event by ID, or null if not found. */
    suspend fun getEventById(eventId: String): Event?

    /**
     * Persists a new event locally (Room) and remotely (Firestore).
     * The [event] must already have a non-blank [Event.id] assigned by the use case.
     */
    suspend fun addEvent(event: Event): Result<Unit>

    /**
     * Updates an existing event. Matched by [Event.id].
     * Sets [Event.updatedAt] to now before persisting.
     */
    suspend fun updateEvent(event: Event): Result<Unit>

    /**
     * Deletes an event by ID from both Room and Firestore.
     */
    suspend fun deleteEvent(eventId: String): Result<Unit>
}
