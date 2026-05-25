package com.example.mycutieschedule.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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

    /**
     * Returns all events that carry a recurrence rule, regardless of their start date.
     * Used together with [RecurrenceExpander] to generate virtual occurrences for any
     * requested date range without duplicating rows in the database.
     */
    @Query("""
        SELECT * FROM event_table
        WHERE owner_id = :userId
          AND recurrence_type IS NOT NULL
        ORDER BY start_datetime ASC
    """)
    fun observeRecurringMasters(userId: String): Flow<List<EventEntity>>

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
