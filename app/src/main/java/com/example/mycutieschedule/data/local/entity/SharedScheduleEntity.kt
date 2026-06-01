package com.example.mycutieschedule.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for the shared_schedule_table.
 *
 * Stores metadata about schedules shared by other users that the current user
 * has accessed (viewed or imported). The actual events live in event_table
 * with [EventEntity.isImported] = true and [EventEntity.sourceScheduleId] = this.id.
 *
 * Maps to domain model: [com.example.mycutieschedule.domain.model.SharedSchedule]
 */
@Entity(
    tableName = "shared_schedule_table",
    indices = [
        Index(value = ["viewer_id"]),
        Index(value = ["share_code"]),
    ],
)
data class SharedScheduleEntity(

    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    /** Firestore document ID of the source schedule (userSchedules/{scheduleId}). */
    @ColumnInfo(name = "schedule_id")
    val scheduleId: String,

    /** The 6-character code used to access this schedule. */
    @ColumnInfo(name = "share_code")
    val shareCode: String,

    /** Display name of the schedule owner — shown in the shared schedules list. */
    @ColumnInfo(name = "owner_name")
    val ownerName: String,

    /**
     * Local-only alias chosen by the viewer (e.g. "Аня — учёба") that overrides
     * [ownerName] in the viewer's UI. NULL means "use owner name as-is".
     * Never propagates back to the owner or to Firestore.
     */
    @ColumnInfo(name = "local_alias_name")
    val localAliasName: String?,

    /** Firebase Auth UID of the current user who accessed this schedule. */
    @ColumnInfo(name = "viewer_id")
    val viewerId: String,

    /** True if the user chose to copy events into their own calendar. */
    @ColumnInfo(name = "is_imported", defaultValue = "0")
    val isImported: Boolean,

    /**
     * Local-only visibility toggle for the main calendar (Month/Week/Day views).
     *
     * - true (default): events from this schedule appear in the viewer's calendar
     *   alongside personal events.
     * - false: events stay in Room and continue to sync in the background, but
     *   are filtered out of every calendar query and workload calculation.
     *
     * Never written to Firestore or visible to the owner — pure viewer preference.
     */
    @ColumnInfo(name = "is_visible_in_calendar", defaultValue = "1")
    val isVisibleInCalendar: Boolean,

    /**
     * Local-only flag: should this schedule's events contribute to Month View
     * workload intensity (the heatmap-style day coloring)?
     *
     * - true (default): events count toward day intensity along with personal events.
     * - false: events still render on the calendar (if visible) but are excluded
     *   from intensity / occupancy calculations.
     *
     * Independent from [isVisibleInCalendar]:
     *   visible=true,  includeInWorkload=true  → shown + counted
     *   visible=true,  includeInWorkload=false → shown, not counted
     *   visible=false, includeInWorkload=*     → hidden (workload effect moot —
     *                                            visibility filter runs first)
     */
    @ColumnInfo(name = "include_in_workload", defaultValue = "1")
    val includeInWorkload: Boolean,

    /** UTC epoch milliseconds — when the current user first accessed this schedule. */
    @ColumnInfo(name = "accessed_at")
    val accessedAt: Long,
)
