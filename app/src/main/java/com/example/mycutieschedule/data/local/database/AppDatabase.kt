package com.example.mycutieschedule.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.mycutieschedule.data.local.dao.DayNoteDao
import com.example.mycutieschedule.data.local.dao.EventDao
import com.example.mycutieschedule.data.local.dao.SharedScheduleDao
import com.example.mycutieschedule.data.local.entity.DayNoteEntity
import com.example.mycutieschedule.data.local.entity.EventEntity
import com.example.mycutieschedule.data.local.entity.SharedScheduleEntity

/**
 * Single Room database for MyCutieSchedule.
 *
 * All datetimes are stored as Long (epoch millis/days) — no TypeConverters needed.
 *
 * Schema versioning:
 *   v1 — initial schema with event_table, day_note_table, shared_schedule_table.
 *
 * Migration strategy:
 *   Add a [androidx.room.migration.Migration] object and pass it to
 *   RoomDatabase.Builder.addMigrations() in [DatabaseModule] before incrementing [version].
 */
@Database(
    entities = [
        EventEntity::class,
        DayNoteEntity::class,
        SharedScheduleEntity::class,
    ],
    version = 1,
    exportSchema = false,   // set to true + configure schemaLocation for production migrations
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao
    abstract fun dayNoteDao(): DayNoteDao
    abstract fun sharedScheduleDao(): SharedScheduleDao

    companion object {
        const val DATABASE_NAME = "mycutieschedule.db"
    }
}
