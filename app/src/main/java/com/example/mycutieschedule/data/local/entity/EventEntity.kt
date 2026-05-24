package com.example.mycutieschedule.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for the event_table.
 *
 * Design notes:
 * - All datetimes stored as epoch millis (Long) — no TypeConverters needed,
 *   better SQLite range query performance, UTC-consistent across devices.
 * - EventColor stored as enum name (String) — human-readable in the DB file.
 * - Reminder/Recurrence stored as flat nullable columns instead of JSON,
 *   keeping the schema explicit and queryable.
 * - Two indices: owner_id (most queries filter by user) and start_datetime
 *   (range queries for day/week/month views).
 *
 * Maps to domain model: [com.example.mycutieschedule.domain.model.Event]
 */
@Entity(
    tableName = "event_table",
    indices = [
        Index(value = ["owner_id"]),
        Index(value = ["start_datetime"]),
    ],
)
data class EventEntity(

    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String,

    /** UTC epoch milliseconds. */
    @ColumnInfo(name = "start_datetime")
    val startDatetime: Long,

    /** UTC epoch milliseconds. */
    @ColumnInfo(name = "end_datetime")
    val endDatetime: Long,

    /** [com.example.mycutieschedule.domain.model.EventColor].name */
    @ColumnInfo(name = "color")
    val color: String,

    // ── Reminder (nullable pair) ──────────────────────────────────────────────

    @ColumnInfo(name = "reminder_minutes_before")
    val reminderMinutesBefore: Int?,

    /** [com.example.mycutieschedule.domain.model.ReminderType].name */
    @ColumnInfo(name = "reminder_type")
    val reminderType: String?,

    // ── Recurrence (nullable flat columns) ───────────────────────────────────

    /** [com.example.mycutieschedule.domain.model.RecurrenceType].name */
    @ColumnInfo(name = "recurrence_type")
    val recurrenceType: String?,

    /** Comma-separated [java.time.DayOfWeek].name values, e.g. "MONDAY,WEDNESDAY". */
    @ColumnInfo(name = "recurrence_repeat_days")
    val recurrenceRepeatDays: String?,

    /** Epoch day (LocalDate.toEpochDay()). */
    @ColumnInfo(name = "recurrence_end_date")
    val recurrenceEndDate: Long?,

    @ColumnInfo(name = "recurrence_count")
    val recurrenceCount: Int?,

    // ── Ownership & import ────────────────────────────────────────────────────

    @ColumnInfo(name = "owner_id")
    val ownerId: String,

    @ColumnInfo(name = "is_imported", defaultValue = "0")
    val isImported: Boolean,

    /** Set when [isImported] = true. References [SharedScheduleEntity.id]. */
    @ColumnInfo(name = "source_schedule_id")
    val sourceScheduleId: String?,

    // ── Timestamps ────────────────────────────────────────────────────────────

    /** UTC epoch milliseconds. */
    @ColumnInfo(name = "created_at")
    val createdAt: Long,

    /** UTC epoch milliseconds. Updated on every save to enable Firestore conflict resolution. */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
