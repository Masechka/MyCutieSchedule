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

    /** UTC epoch milliseconds — when the current user first accessed this schedule. */
    @ColumnInfo(name = "accessed_at")
    val accessedAt: Long,
)
