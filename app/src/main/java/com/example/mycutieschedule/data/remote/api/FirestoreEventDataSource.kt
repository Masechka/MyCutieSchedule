package com.example.mycutieschedule.data.remote.api

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.data.remote.dto.EventDto
import kotlinx.coroutines.flow.Flow

/**
 * Contract for Firestore event operations.
 *
 * Implementation: [com.example.mycutieschedule.data.remote.datasource.FirestoreEventDataSourceImpl]
 * Firestore path: `users/{userId}/events/{eventId}`
 *
 * Read path uses a snapshot listener (Flow) so the local Firestore cache
 * emits immediately even when offline.
 * Write path uses `Task.await()` — wraps Firebase's callback API into suspend functions.
 */
interface FirestoreEventDataSource {

    /**
     * Live stream of all events owned by [userId].
     * Emits on collection and on every Firestore snapshot change (online or from local cache).
     * The flow never completes — it terminates only when the collector is cancelled.
     */
    fun observeEvents(userId: String): Flow<List<EventDto>>

    /** Creates or replaces an event document (upsert). */
    suspend fun saveEvent(event: EventDto, userId: String): Result<Unit>

    /** Hard-deletes an event document. */
    suspend fun deleteEvent(eventId: String, userId: String): Result<Unit>
}
