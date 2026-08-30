package com.example.mycutieschedule.data.fake

import com.example.mycutieschedule.data.local.dao.EventDao
import com.example.mycutieschedule.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory fake for [EventDao] — no Room, no SQLite, no Android runtime.
 *
 * Every query mirrors the WHERE clause of the real `@Query` it replaces, so a
 * test that depends on filter semantics (e.g. the boot-time "future reminders"
 * filter) exercises the same predicate the database would apply.
 *
 * Test hook:
 *   [lastFutureRemindersArg] records the `nowEpochMilli` bound the production
 *   code passed in — useful when a failure needs diagnosing, since a wrong
 *   time base shows up as an offset there.
 */
class FakeEventDao : EventDao {

    private val eventsFlow = MutableStateFlow<List<EventEntity>>(emptyList())

    /** All rows currently in the fake table. */
    val events: List<EventEntity> get() = eventsFlow.value

    /** Bound most recently passed to [findEventsWithFutureReminders], or null. */
    var lastFutureRemindersArg: Long? = null
        private set

    fun seed(vararg entities: EventEntity) {
        eventsFlow.value = eventsFlow.value + entities
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

    override suspend fun findAllOwnedByUser(userId: String): List<EventEntity> =
        eventsFlow.value.filter { it.ownerId == userId && !it.isImported }

    override suspend fun deleteByIds(eventIds: List<String>) {
        eventsFlow.value = eventsFlow.value.filterNot { it.id in eventIds }
    }

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

    override suspend fun deleteByGroupIdFromDate(groupId: String, fromStartMilli: Long) {
        eventsFlow.value = eventsFlow.value.filterNot {
            it.recurrenceGroupId == groupId && it.startDatetime >= fromStartMilli
        }
    }

    // ── Single-item lookup ────────────────────────────────────────────────────

    override suspend fun findById(eventId: String): EventEntity? =
        eventsFlow.value.find { it.id == eventId }

    // ── Writes ────────────────────────────────────────────────────────────────

    override suspend fun insert(event: EventEntity) = insertAll(listOf(event))

    override suspend fun insertAll(events: List<EventEntity>) {
        // REPLACE-on-conflict semantics: incoming rows win on matching id.
        val incomingIds = events.map { it.id }.toSet()
        eventsFlow.value = eventsFlow.value.filterNot { it.id in incomingIds } + events
    }

    override suspend fun update(event: EventEntity) {
        eventsFlow.value = eventsFlow.value.map { if (it.id == event.id) event else it }
    }

    override suspend fun deleteById(eventId: String) {
        eventsFlow.value = eventsFlow.value.filterNot { it.id == eventId }
    }

    override suspend fun deleteBySourceScheduleId(scheduleId: String) {
        eventsFlow.value = eventsFlow.value.filterNot { it.sourceScheduleId == scheduleId }
    }
}
