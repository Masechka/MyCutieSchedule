package com.example.mycutieschedule.data.repository

import com.example.mycutieschedule.core.extensions.toEpochMilli
import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.core.util.toDomainResult
import com.example.mycutieschedule.data.local.dao.EventDao
import com.example.mycutieschedule.data.mapper.toDomain
import com.example.mycutieschedule.data.mapper.toDto
import com.example.mycutieschedule.data.mapper.toEntity
import com.example.mycutieschedule.data.notification.EventReminderScheduler
import com.example.mycutieschedule.data.remote.api.FirestoreEventDataSource
import com.example.mycutieschedule.di.ApplicationScope
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.repository.EventRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Offline-first implementation of [EventRepository].
 *
 * **Read path:** Room only — always instant, always available offline.
 *   [EventSyncManager] keeps Room up-to-date from Firestore in the background.
 *
 * **Write path (single events):** Room first → Firestore synchronously (1 network call, fast).
 *
 * **Write path (batch — recurrence generation / deletion):**
 *   Room is updated atomically and the suspend function returns immediately.
 *   Firestore sync is then dispatched as a fire-and-forget job on [applicationScope]
 *   (application-scoped, survives ViewModel clearing) so the UI can navigate away
 *   without waiting for N network round-trips.
 *   If a Firestore write fails it is logged; [EventSyncManager] reconciles on reconnect.
 */
class EventRepositoryImpl @Inject constructor(
    private val eventDao: EventDao,
    private val eventDataSource: FirestoreEventDataSource,
    private val reminderScheduler: EventReminderScheduler,
    @ApplicationScope private val applicationScope: CoroutineScope,
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

    /** Single-event insert — Firestore write is synchronous (fast, 1 call). */
    override suspend fun addEvent(event: Event): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) { eventDao.insert(event.toEntity()) }
        reminderScheduler.schedule(event)
        writeToFirestore(event)
    }.toDomainResult()

    /**
     * Batch insert for generated recurrence occurrences.
     *
     * Room insert is wrapped in [Dispatchers.IO] and completes before this function
     * returns. Firestore sync is launched in [applicationScope] (fire-and-forget) so
     * the caller is unblocked after the local write and can close the UI immediately.
     */
    override suspend fun addAllEvents(events: List<Event>): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            eventDao.insertAll(events.map { it.toEntity() })
        }
        // Schedule reminders for every generated occurrence. AlarmManager is
        // cheap; scheduling N alarms is O(N) inside the receiver process and
        // doesn't touch the network.
        events.forEach { reminderScheduler.schedule(it) }
        // Fire-and-forget: application scope survives ViewModel/screen lifecycle.
        // launch() returns Job; trailing Unit keeps the lambda typed as () -> Unit.
        applicationScope.launch(Dispatchers.IO) {
            events.forEach { writeToFirestore(it) }
        }
        Unit
    }.toDomainResult()

    /** Single-event update — Firestore write is synchronous (fast, 1 call). */
    override suspend fun updateEvent(event: Event): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) { eventDao.update(event.toEntity()) }
        // Re-schedule: handles add-reminder, remove-reminder, edit-time, and
        // edit-minutesBefore uniformly. If event.reminder is null, schedule()
        // returns early and the previous alarm is cancelled below.
        if (event.reminder != null) {
            reminderScheduler.schedule(event)
        } else {
            reminderScheduler.cancel(event.id)
        }
        writeToFirestore(event)
    }.toDomainResult()

    /** Single-event delete — Firestore delete is synchronous (fast, 1 call). */
    override suspend fun deleteEvent(eventId: String): Result<Unit> = runCatching {
        val entity = withContext(Dispatchers.IO) { eventDao.findById(eventId) }
        withContext(Dispatchers.IO) { eventDao.deleteById(eventId) }
        reminderScheduler.cancel(eventId)
        if (entity != null) deleteFromFirestore(eventId, entity.ownerId)
    }.toDomainResult()

    /**
     * Deletes all events in a recurrence chain from [fromStart] onwards.
     *
     * The SELECT + DELETE run atomically inside a Room [@Transaction][androidx.room.Transaction]
     * (defined on the DAO), reducing Flow emissions to a single update and preventing
     * a reader from observing stale rows between the two statements.
     *
     * Firestore deletes are fire-and-forget via [applicationScope] — the function returns
     * as soon as Room commits, letting the screen close without waiting for N network calls.
     */
    override suspend fun deleteEventAndFollowing(
        recurrenceGroupId: String,
        fromStart: LocalDateTime,
    ): Result<Unit> = runCatching {
        val fromMilli = fromStart.toEpochMilli()
        // Atomic Room transaction: find IDs first (needed for Firestore), then delete.
        val toDelete = withContext(Dispatchers.IO) {
            eventDao.findAndDeleteByGroupIdFromDate(recurrenceGroupId, fromMilli)
        }
        // Cancel reminder alarms for every deleted occurrence — otherwise we'd
        // fire notifications for events the user just removed.
        toDelete.forEach { entity -> reminderScheduler.cancel(entity.id) }
        // Fire-and-forget Firestore cleanup.
        // launch() returns Job; trailing Unit keeps the lambda typed as () -> Unit.
        applicationScope.launch(Dispatchers.IO) {
            toDelete.forEach { entity -> deleteFromFirestore(entity.id, entity.ownerId) }
        }
        Unit
    }.toDomainResult()

    // ── Firestore helpers ─────────────────────────────────────────────────────

    private suspend fun writeToFirestore(event: Event) {
        val result = eventDataSource.saveEvent(event.toDto(), event.ownerId)
        if (result is Result.Error) {
            // VISIBLE failure mode — if this fires consistently, sync between
            // devices is broken (most often: Firestore Security Rules block
            // writes to users/{uid}/events). The event lives in Room locally
            // so the user keeps seeing it, but it never reaches the cloud.
            Timber.e(
                result.exception,
                "Firestore write FAILED for event id=${event.id} ownerId=${event.ownerId}. " +
                    "Event saved locally but will NOT propagate to other devices " +
                    "until the underlying error is resolved (check Firestore Security Rules).",
            )
        }
    }

    private suspend fun deleteFromFirestore(eventId: String, ownerId: String) {
        val result = eventDataSource.deleteEvent(eventId, ownerId)
        if (result is Result.Error) {
            Timber.e(
                result.exception,
                "Firestore delete FAILED for event id=$eventId ownerId=$ownerId. " +
                    "Event removed locally but still exists in Firestore — " +
                    "other devices will continue to see it.",
            )
        }
    }
}
