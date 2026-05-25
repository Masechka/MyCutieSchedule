package com.example.mycutieschedule.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mycutieschedule.data.local.dao.DayNoteDao
import com.example.mycutieschedule.data.local.dao.EventDao
import com.example.mycutieschedule.data.local.dao.SharedScheduleDao
import com.example.mycutieschedule.data.local.entity.DayNoteEntity
import com.example.mycutieschedule.data.local.entity.EventEntity
import com.example.mycutieschedule.data.local.entity.SharedScheduleEntity

/**
 * Single Room database for MyCutieSchedule.
 *
 * Schema versioning:
 *   v1 — initial schema: event_table, day_note_table, shared_schedule_table.
 *   v2 — adds recurring-event columns to event_table:
 *          recurrence_interval_weeks, recurrence_custom_dates,
 *          recurrence_group_id, is_recurrence_generated.
 */
@Database(
    entities = [
        EventEntity::class,
        DayNoteEntity::class,
        SharedScheduleEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao
    abstract fun dayNoteDao(): DayNoteDao
    abstract fun sharedScheduleDao(): SharedScheduleDao

    companion object {
        const val DATABASE_NAME = "mycutieschedule.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE event_table ADD COLUMN recurrence_interval_weeks INTEGER")
                database.execSQL("ALTER TABLE event_table ADD COLUMN recurrence_custom_dates TEXT")
                database.execSQL("ALTER TABLE event_table ADD COLUMN recurrence_group_id TEXT")
                database.execSQL("ALTER TABLE event_table ADD COLUMN is_recurrence_generated INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
