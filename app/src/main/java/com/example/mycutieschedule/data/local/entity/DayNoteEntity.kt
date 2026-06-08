package com.example.mycutieschedule.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for the day_note_table.
 *
 * One note per user per calendar day (enforced by unique composite index).
 * Upsert semantics: insert with REPLACE strategy on conflict.
 *
 * Maps to domain model: [com.example.mycutieschedule.domain.model.DayNote]
 */
@Entity(
    tableName = "day_note_table",
    indices = [
        Index(value = ["date_epoch", "owner_id"], unique = true),
    ],
)
data class DayNoteEntity(

    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    /** [java.time.LocalDate.toEpochDay] — epoch day value. */
    @ColumnInfo(name = "date_epoch")
    val dateEpoch: Long,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "owner_id")
    val ownerId: String,

    /** UTC epoch milliseconds — used for sync conflict resolution. */
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
