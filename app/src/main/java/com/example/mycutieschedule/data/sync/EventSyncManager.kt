package com.example.mycutieschedule.data.sync

import com.example.mycutieschedule.data.local.dao.EventDao
import com.example.mycutieschedule.data.mapper.toDomain
import com.example.mycutieschedule.data.mapper.toEntity
import com.example.mycutieschedule.data.remote.api.FirestoreEventDataSource
import com.example.mycutieschedule.di.ApplicationScope
import com.example.mycutieschedule.domain.model.AuthState
import com.example.mycutieschedule.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps Room in sync with Firestore by subscribing to the authenticated user's
 * event collection and upserting every snapshot into the local database.
 *
 * **Lifecycle:**
 * [start] is called once from [com.example.mycutieschedule.MyCutieScheduleApp.onCreate].
 * It launches a long-lived coroutine (scoped to the application) that:
 *   1. Observes [AuthRepository.observeAuthState].
 *   2. On [AuthState.Authenticated] → starts a Firestore snapshot listener.
 *   3. On [AuthState.Unauthenticated]  → cancels the listener.
 *
 * **Conflict resolution — last-write-wins:**
 * When a Firestore snapshot arrives, each remote event is compared to its
 * local Room counterpart by `updatedAt` timestamp.
 * - Remote newer → overwrite Room (another device made a change).
 * - Local newer  → skip (local write hasn't reached Firestore yet).
 * - No local row → insert (new event from another device).
 *
 * This prevents a round-trip overwrite when the user makes a local edit and
 * Firestore echoes it back before Room has flushed.
 *
 * **Scope:**
 * Uses [@ApplicationScope] — a [CoroutineScope] that lives as long as the
 * process. [SupervisorJob] ensures one failed sync doesn't cancel the watcher.
 */
@Singleton
class EventSyncManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val eventDataSource: FirestoreEventDataSource,
    private val eventDao: EventDao,
    @ApplicationScope private val appScope: CoroutineScope,
) {

    /** Cancellable job for the active Firestore listener. */
    private var syncJob: Job? = null

    /**
     * Starts the auth-state watcher. Safe to call multiple times — the internal
     * job is cancelled and restarted only when auth state actually changes.
     */
    fun start() {
        appScope.launch {
            authRepository.observeAuthState().collect { state ->
                when (state) {
                    is AuthState.Authenticated  -> startSync(state.user.id)
                    is AuthState.Unauthenticated -> stopSync()
                }
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun startSync(userId: String) {
        syncJob?.cancel()
        syncJob = appScope.launch {
            Timber.d("EventSyncManager: starting Firestore sync for userId=$userId")
            eventDataSource.observeEvents(userId).collect { remoteDtos ->
                try {
                    applySnapshot(remoteDtos.map { it.toDomain().toEntity() })
                } catch (e: Exception) {
                    Timber.e(e, "EventSyncManager: failed to apply snapshot")
                }
            }
        }
    }

    private fun stopSync() {
        Timber.d("EventSyncManager: stopping sync (user signed out)")
        syncJob?.cancel()
        syncJob = null
    }

    /**
     * Applies a Firestore snapshot to Room using last-write-wins logic.
     *
     * Only rows where `remoteUpdatedAt >= localUpdatedAt` are written.
     * This avoids overwriting a locally-edited event that hasn't been
     * pushed to Firestore yet.
     */
    private suspend fun applySnapshot(
        remoteEntities: List<com.example.mycutieschedule.data.local.entity.EventEntity>,
    ) {
        if (remoteEntities.isEmpty()) return

        // Build a map of current local state for the same IDs.
        val remoteIds = remoteEntities.map { it.id }
        val localMap = remoteIds
            .mapNotNull { eventDao.findById(it) }
            .associateBy { it.id }

        val toUpsert = remoteEntities.filter { remote ->
            val local = localMap[remote.id]
            // Insert if no local copy, or if remote is at least as recent.
            local == null || remote.updatedAt >= local.updatedAt
        }

        if (toUpsert.isNotEmpty()) {
            eventDao.insertAll(toUpsert)
            Timber.d("EventSyncManager: upserted ${toUpsert.size} events from Firestore")
        }
    }
}
