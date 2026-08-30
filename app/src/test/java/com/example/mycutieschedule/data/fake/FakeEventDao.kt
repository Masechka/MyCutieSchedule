package com.example.mycutieschedule.data.fake

import com.example.mycutieschedule.data.local.dao.EventDao
import com.example.mycutieschedule.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicInteger

/**
 * In-memory fake for [EventDao] — no Room, no SQLite, no Android runtime.
 *
 * Every query mirrors the WHERE clause of the real `@Query` it replaces, so a
 * test that depends on filter semantics (e.g. the boot-time "future reminders"
 * filter) exercises the same predicate the database would apply.
 *
 * Thread-safe: [com.example.mycutieschedule.data.sync.EventSyncManager] applies
 * snapshots on [kotlinx.coroutines.Dispatchers.IO], i.e. not on the test thread,
 * so every mutation takes [lock] and the read-modify-write on [eventsFlow] never
 * races.
 *
 * Test hooks:
 *   - [lastFutureRemindersArg] records the `nowEpochMilli` bound the production
 *     code passed in — a wrong time base shows up there as an offset.
 *   - [snapshotReads], [deletedIds] and [insertedIds] record what the sync path
 *     actually did, so a test can assert on behaviour instead of only on state.
 *
 * @param initial rows the table starts with.
 */
class FakeEventDao(initial: List<EventEntity> = emptyList()) : EventDao {

    private val lock = Any()
    private val eventsFlow = MutableStateFlow(initial)

    private val _snapshotReads = AtomicInteger(0)
    private val _deletedIds = mutableListOf<String>()
    private val _insertedIds = mutableListOf<String>()

    /** All rows currently in the fake table. */
    val events: List<EventEntity> get() = eventsFlow.value

    /** How many times the sync path read the local snapshot. */
    val snapshotReads: Int get() = _snapshotReads.get()

    /** Ids removed by any delete path, in call order. */
    val deletedIds: List<String> get() = synchronized(lock) { _deletedIds.toList() }

    /** Ids written by [insert] / [insertAll], in call order. */
    val insertedIds: List<String> get() = synchronized(lock) { _insertedIds.toList() }

    /** Bound most recently passed to [findEventsWithFutureReminders], or null. */
    var lastFutureRemindersArg: Long? = null
        private set

    fun seed(vararg entities: EventEntity) = synchronized(lock) {
        eventsFlow.value = eventsFlow.value + entities
    }

    /** Removes [ids] and records them, under [lock]. Callers must hold nothing. */
    private fun removeRecording(ids: List<String>) = synchronized(lock) {
        if (ids.isEmpty()) return@synchronized
        _deletedIds += ids
        val doomed = ids.toSet()
        eventsFlow.value = eventsFlow.value.filterNot { it.id in doomed }
    }

    // ── Range queries ─────────────────────────────────────────────────────────

    override fun observeEventsBetween(
        startEpochMilli: Long,
        endEpochMilli: Long,
        userId: String,
    ): Flow<List<EventEntity>> = eventsFlow.map { list ->
        list.filter {
            it.ownerId == userId &&
                it.startDatetime < endEpochMilli &&
                it.endDatetime > startEpochMilli
        }.sortedBy { it.startDatetime }
    }

    override fun observeByDate(
        dayStartMilli: Long,
        dayEndMilli: Long,
        userId: String,
    ): Flow<List<EventEntity>> = observeEventsBetween(dayStartMilli, dayEndMilli, userId)

    override fun observeByWeek(
        weekStartMilli: Long,
        weekEndMilli: Long,
        userId: String,
    ): Flow<List<EventEntity>> = observeEventsBetween(weekStartMilli, weekEndMilli, userId)

    override fun observeByMonth(
        monthStartMilli: Long,
        monthEndMilli: Long,
        userId: String,
    ): Flow<List<EventEntity>> = observeEventsBetween(monthStartMilli, monthEndMilli, userId)

    override fun observeImportedEvents(userId: String): Flow<List<EventEntity>> =
        eventsFlow.map { list ->
            list.filter { it.ownerId == userId && it.isImported }
                .sortedBy { it.startDatetime }
        }

    // ── Sync helpers ──────────────────────────────────────────────────────────

    override suspend fun findAllOwnedByUser(userId: String): List<EventEntity> {
        _snapshotReads.incrementAndGet()
        return eventsFlow.value.filter { it.ownerId == userId && !it.isImported }
    }

    override suspend fun deleteByIds(eventIds: List<String>) = removeRecording(eventIds)

    /**
     * Mirrors the real query:
     *   reminder_minutes_before IS NOT NULL
     *   AND reminder_type       IS NOT NULL
     *   AND start_datetime      >  :nowEpochMilli
     *   AND is_imported         =  0
     */
    override suspend fun findEventsWithFutureReminders(nowEpochMilli: Long): List<EventEntity> {
        lastFutureRemindersArg = nowEpochMilli
        return eventsFlow.value.filter {
            it.reminderMinutesBefore != null &&
                it.reminderType != null &&
                it.startDatetime > nowEpochMilli &&
                !it.isImported
        }
    }

    override suspend fun findImportedEventsBySource(sourceScheduleId: String): List<EventEntity> =
        eventsFlow.value.filter { it.sourceScheduleId == sourceScheduleId && it.isImported }

    // ── Recurrence chain ──────────────────────────────────────────────────────

    override suspend fun findByGroupIdFromDate(
        groupId: String,
        fromStartMilli: Long,
    ): List<EventEntity> = eventsFlow.value.filter {
        it.recurrenceGroupId == groupId && it.startDatetime >= fromStartMilli
    }

    override suspend fun deleteByGroupIdFromDate(groupId: String, fromStartMilli: Long) =
        removeRecording(findByGroupIdFromDate(groupId, fromStartMilli).map { it.id })

    // ── Single-item lookup ────────────────────────────────────────────────────

    override suspend fun findById(eventId: String): EventEntity? =
        eventsFlow.value.find { it.id == eventId }

    // ── Writes ────────────────────────────────────────────────────────────────

    override suspend fun insert(event: EventEntity) = insertAll(listOf(event))

    override suspend fun insertAll(events: List<EventEntity>) = synchronized(lock) {
        // REPLACE-on-conflict semantics: incoming rows win on matching id.
        val incomingIds = events.map { it.id }.toSet()
        _insertedIds += events.map { it.id }
        eventsFlow.value = eventsFlow.value.filterNot { it.id in incomingIds } + events
    }

    override suspend fun update(event: EventEntity) = synchronized(lock) {
        eventsFlow.value = eventsFlow.value.map { if (it.id == event.id) event else it }
    }

    override suspend fun deleteById(eventId: String) = removeRecording(listOf(eventId))

    override suspend fun deleteBySourceScheduleId(scheduleId: String) =
        removeRecording(eventsFlow.value.filter { it.sourceScheduleId == scheduleId }.map { it.id })
}
