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
 *   v3 — adds local_alias_name column to shared_schedule_table so the viewer
 *          can rename imported schedules locally without affecting the owner.
 *   v4 — adds is_visible_in_calendar column to shared_schedule_table so the
 *          viewer can temporarily hide a shared schedule from the main calendar
 *          without removing the local data or interrupting sync.
 *   v5 — adds include_in_workload column to shared_schedule_table so the viewer
 *          can decide per-schedule whether imported events contribute to Month
 *          View workload intensity, independent from visibility.
 */
@Database(
    entities = [
        EventEntity::class,
        DayNoteEntity::class,
        SharedScheduleEntity::class,
    ],
    version = 5,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE shared_schedule_table ADD COLUMN local_alias_name TEXT")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Default 1 = visible; existing imports keep their current
                // calendar presence after upgrade.
                database.execSQL(
                    "ALTER TABLE shared_schedule_table " +
                        "ADD COLUMN is_visible_in_calendar INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Default 1 = counted in workload; preserves existing behaviour
                // for already-imported schedules. Users can opt out per-schedule
                // via the details screen.
                database.execSQL(
                    "ALTER TABLE shared_schedule_table " +
                        "ADD COLUMN include_in_workload INTEGER NOT NULL DEFAULT 1"
                )
            }
        }
    }
}
