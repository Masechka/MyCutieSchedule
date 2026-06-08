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

    /**
     * Explicit one-shot fetch of every event owned by [userId].
     *
     * Used at sync start to:
     *   1. Verify Firestore connectivity (the suspending call returns a typed error
     *      if reads are denied by security rules, the network is unreachable, etc.).
     *   2. Prime the local Firestore cache so subsequent snapshot listeners start
     *      with up-to-date data instead of a stale or empty cache.
     *
     * This complements [observeEvents] — it does not replace it. The snapshot
     * listener is still the source of truth for ongoing live sync.
     */
    suspend fun fetchOnce(userId: String): Result<List<EventDto>>

    /** Creates or replaces an event document (upsert). */
    suspend fun saveEvent(event: EventDto, userId: String): Result<Unit>

    /** Hard-deletes an event document. */
    suspend fun deleteEvent(eventId: String, userId: String): Result<Unit>
}
