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
        Timber.d("Firestore: registering snapshot listener for users/$userId/events")
        val registration = eventsCollection(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Don't close the flow — Firestore will retry on reconnection.
                Timber.w(error, "Firestore events snapshot error (userId=$userId)")
                return@addSnapshotListener
            }

            val isFromCache       = snapshot?.metadata?.isFromCache ?: true
            val hasPendingWrites  = snapshot?.metadata?.hasPendingWrites() ?: false

            val dtos = snapshot?.documents?.mapNotNull { doc ->
                runCatching {
                    doc.toObject(EventDto::class.java)?.copy(id = doc.id)
                }
                    .onFailure { Timber.e(it, "Failed to parse EventDto (id=${doc.id})") }
                    .getOrNull()
            } ?: emptyList()

            Timber.d(
                "Firestore: snapshot for userId=$userId — ${dtos.size} events " +
                    "(fromCache=$isFromCache, pendingWrites=$hasPendingWrites)"
            )

            trySend(dtos)
        }
        awaitClose {
            Timber.d("Firestore: unregistering snapshot listener for userId=$userId")
            registration.remove()
        }
    }

    override suspend fun fetchOnce(userId: String): Result<List<EventDto>> =
        runCatching {
            Timber.d("Firestore: one-shot fetch for users/$userId/events")
            val snapshot = eventsCollection(userId).get().await()
            val dtos = snapshot.documents.mapNotNull { doc ->
                runCatching { doc.toObject(EventDto::class.java)?.copy(id = doc.id) }
                    .onFailure { Timber.e(it, "Failed to parse EventDto (id=${doc.id})") }
                    .getOrNull()
            }
            Timber.d(
                "Firestore: one-shot fetch returned ${dtos.size} events for userId=$userId " +
                    "(fromCache=${snapshot.metadata.isFromCache})"
            )
            dtos
        }.toDomainResult()

    override suspend fun saveEvent(event: EventDto, userId: String): Result<Unit> =
        runCatching {
            eventsCollection(userId)
                .document(event.id)
                .set(event)
                .await()
            Timber.d("Firestore: saved event id=${event.id} for userId=$userId")
            Unit  // Task<Void>.await() returns Void?, not Unit — explicit Unit fixes the type
        }.toDomainResult()

    override suspend fun deleteEvent(eventId: String, userId: String): Result<Unit> =
        runCatching {
            eventsCollection(userId)
                .document(eventId)
                .delete()
                .await()
            Timber.d("Firestore: deleted event id=$eventId for userId=$userId")
            Unit
        }.toDomainResult()
}
