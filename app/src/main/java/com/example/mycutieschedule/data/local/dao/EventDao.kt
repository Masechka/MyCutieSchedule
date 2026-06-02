package com.example.mycutieschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.mycutieschedule.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    // ── Core range query ──────────────────────────────────────────────────────

    /**
     * Returns a live stream of events that overlap the given time range.
     *
     * Overlap condition: event.start < rangeEnd AND event.end > rangeStart
     * This correctly handles events that start before or end after the range boundary.
     *
     * Results ordered by start time ascending.
     */
    @Query("""
        SELECT * FROM event_table
        WHERE owner_id = :userId
          AND start_datetime < :endEpochMilli
          AND end_datetime   > :startEpochMilli
        ORDER BY start_datetime ASC
    """)
    fun observeEventsBetween(
        startEpochMilli: Long,
        endEpochMilli: Long,
        userId: String,
    ): Flow<List<EventEntity>>

    // ── Named convenience queries (used by use cases / repository) ────────────

    /** Observe all events that overlap a single calendar day. */
    @Query("""
        SELECT * FROM event_table
        WHERE owner_id = :userId
          AND start_datetime < :dayEndMilli
          AND end_datetime   > :dayStartMilli
        ORDER BY start_datetime ASC
    """)
    fun observeByDate(
        dayStartMilli: Long,
        dayEndMilli: Long,
        userId: String,
    ): Flow<List<EventEntity>>

    /** Observe all events that overlap a Mon–Sun week range. */
    @Query("""
        SELECT * FROM event_table
        WHERE owner_id = :userId
          AND start_datetime < :weekEndMilli
          AND end_datetime   > :weekStartMilli
        ORDER BY start_datetime ASC
    """)
    fun observeByWeek(
        weekStartMilli: Long,
        weekEndMilli: Long,
        userId: String,
    ): Flow<List<EventEntity>>

    /** Observe all events that overlap a calendar month. */
    @Query("""
        SELECT * FROM event_table
        WHERE owner_id = :userId
          AND start_datetime < :monthEndMilli
          AND end_datetime   > :monthStartMilli
        ORDER BY start_datetime ASC
    """)
    fun observeByMonth(
        monthStartMilli: Long,
        monthEndMilli: Long,
        userId: String,
    ): Flow<List<EventEntity>>

    /** Observe all imported events for a user (for SharedSchedule composition). */
    @Query("""
        SELECT * FROM event_table
        WHERE owner_id = :userId
          AND is_imported = 1
        ORDER BY start_datetime ASC
    """)
    fun observeImportedEvents(userId: String): Flow<List<EventEntity>>

    // ── Sync helpers ──────────────────────────────────────────────────────────

    /**
     * One-shot read of every user-owned event (excludes imported ones from other users).
     * Used by [com.example.mycutieschedule.data.sync.EventSyncManager] to diff against
     * the Firestore snapshot in a single round-trip — much cheaper than calling
     * [findById] in a loop.
     */
    @Query("""
        SELECT * FROM event_table
        WHERE owner_id = :userId
          AND is_imported = 0
    """)
    suspend fun findAllOwnedByUser(userId: String): List<EventEntity>

    /**
     * Bulk delete by ID list. Used by [EventSyncManager] to propagate remote
     * deletions to Room when an event present locally is no longer in the
     * Firestore snapshot.
     */
    @Query("DELETE FROM event_table WHERE id IN (:eventIds)")
    suspend fun deleteByIds(eventIds: List<String>)

    /**
     * One-shot list of every imported event belonging to a specific shared
     * schedule (identified by the share code = sourceScheduleId).
     * Used by [com.example.mycutieschedule.data.sync.SharedSchedulesSyncManager]
     * to diff the local mirror against the owner's latest Firestore snapshot.
     */
    /**
     * Every future event with an attached reminder, across all users.
     * Used by [com.example.mycutieschedule.data.notification.BootCompletedReceiver]
     * to re-arm alarms after device reboot. Filters out imported events because
     * we never schedule notifications for events that belong to other users.
     */
    @Query("""
        SELECT * FROM event_table
        WHERE reminder_minutes_before IS NOT NULL
          AND reminder_type           IS NOT NULL
          AND start_datetime          >  :nowEpochMilli
          AND is_imported             =  0
    """)
    suspend fun findEventsWithFutureReminders(nowEpochMilli: Long): List<EventEntity>

    @Query("""
        SELECT * FROM event_table
        WHERE source_schedule_id = :sourceScheduleId
          AND is_imported = 1
    """)
    suspend fun findImportedEventsBySource(sourceScheduleId: String): List<EventEntity>

    // ── Recurrence chain queries ──────────────────────────────────────────────

    /**
     * Returns all events in a recurrence chain whose start is on or after [fromStartMilli].
     */
    @Query("""
        SELECT * FROM event_table
        WHERE recurrence_group_id = :groupId
          AND start_datetime >= :fromStartMilli
    """)
    suspend fun findByGroupIdFromDate(groupId: String, fromStartMilli: Long): List<EventEntity>

    /**
     * Bulk-deletes all events in a recurrence chain starting from [fromStartMilli].
     */
    @Query("""
        DELETE FROM event_table
        WHERE recurrence_group_id = :groupId
          AND start_datetime >= :fromStartMilli
    """)
    suspend fun deleteByGroupIdFromDate(groupId: String, fromStartMilli: Long)

    /**
     * Atomically finds and deletes all events in a recurrence chain from [fromStartMilli]
     * in a single Room transaction, then returns the deleted entities so their IDs
     * can be forwarded to Firestore cleanup.
     *
     * Using [Transaction] here prevents another observer from reading stale data
     * between the SELECT and the DELETE, and reduces the number of Flow emissions
     * to a single update instead of one per statement.
     */
    @Transaction
    suspend fun findAndDeleteByGroupIdFromDate(
        groupId: String,
        fromStartMilli: Long,
    ): List<EventEntity> {
        val entities = findByGroupIdFromDate(groupId, fromStartMilli)
        deleteByGroupIdFromDate(groupId, fromStartMilli)
        return entities
    }

    // ── Single-item lookup ────────────────────────────────────────────────────

    @Query("SELECT * FROM event_table WHERE id = :eventId LIMIT 1")
    suspend fun findById(eventId: String): EventEntity?

    // ── Write operations ──────────────────────────────────────────────────────

    /**
     * Inserts an event. REPLACE strategy handles upsert: if an event with the
     * same ID already exists (e.g. from Firestore sync), it is replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity)

    /**
     * Bulk upsert — used by [EventSyncManager] to apply a full Firestore snapshot.
     * REPLACE strategy makes this idempotent: re-inserting an unchanged event is a no-op.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(events: List<EventEntity>)

    @Update
    suspend fun update(event: EventEntity)

    @Query("DELETE FROM event_table WHERE id = :eventId")
    suspend fun deleteById(eventId: String)

    @Query("DELETE FROM event_table WHERE source_schedule_id = :scheduleId")
    suspend fun deleteBySourceScheduleId(scheduleId: String)
}
