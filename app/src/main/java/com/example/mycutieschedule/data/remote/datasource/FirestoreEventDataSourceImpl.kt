package com.example.mycutieschedule.data.remote.datasource

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.core.util.toDomainResult
import com.example.mycutieschedule.data.remote.api.FirestoreCollections
import com.example.mycutieschedule.data.remote.api.FirestoreEventDataSource
import com.example.mycutieschedule.data.remote.dto.EventDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

/**
 * Firestore implementation of [FirestoreEventDataSource].
 *
 * Read path: Snapshot listener via [callbackFlow].
 *   - Firestore delivers from local offline cache immediately, then from the server
 *     when connectivity is available — true offline-first behaviour.
 *   - Errors from the listener are logged but don't close the Flow;
 *     Firestore retries automatically on reconnection.
 *
 * Write path: [Task.await] via kotlinx-coroutines-play-services.
 *   - Room is always written first (in the repository); Firestore is best-effort.
 */
class FirestoreEventDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : FirestoreEventDataSource {

    private fun eventsCollection(userId: String) = firestore
        .collection(FirestoreCollections.USERS)
        .document(userId)
        .collection(FirestoreCollections.EVENTS)

    override fun observeEvents(userId: String): Flow<List<EventDto>> = callbackFlow {
        val registration = eventsCollection(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Don't close the flow — Firestore will retry on reconnection.
                Timber.w(error, "Firestore events snapshot error (userId=$userId)")
                return@addSnapshotListener
            }
            val dtos = snapshot?.documents?.mapNotNull { doc ->
                runCatching {
                    doc.toObject(EventDto::class.java)?.copy(id = doc.id)
                }
                    .onFailure { Timber.e(it, "Failed to parse EventDto (id=${doc.id})") }
                    .getOrNull()
            } ?: emptyList()

            trySend(dtos)
        }
        awaitClose { registration.remove() }
    }

    override suspend fun saveEvent(event: EventDto, userId: String): Result<Unit> =
        runCatching {
            eventsCollection(userId)
                .document(event.id)
                .set(event)
                .await()
        }.toDomainResult()

    override suspend fun deleteEvent(eventId: String, userId: String): Result<Unit> =
        runCatching {
            eventsCollection(userId)
                .document(eventId)
                .delete()
                .await()
        }.toDomainResult()
}
