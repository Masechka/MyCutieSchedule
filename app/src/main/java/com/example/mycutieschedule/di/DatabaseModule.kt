package com.example.mycutieschedule.di

import android.content.Context
import androidx.room.Room
import com.example.mycutieschedule.data.local.dao.DayNoteDao
import com.example.mycutieschedule.data.local.dao.EventDao
import com.example.mycutieschedule.data.local.dao.SharedScheduleDao
import com.example.mycutieschedule.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Single Room database instance for the lifetime of the application.
     *
     * fallbackToDestructiveMigration is intentionally NOT set here.
     * Add explicit [androidx.room.migration.Migration] objects before
     * incrementing [AppDatabase.version] to preserve user data.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
            )
            .build()

    /**
     * DAO instances are lightweight wrappers — no need to scope them as singletons,
     * but since the database is a singleton, Room will reuse the same DAO internally.
     */
    @Provides
    fun provideEventDao(db: AppDatabase): EventDao = db.eventDao()

    @Provides
    fun provideDayNoteDao(db: AppDatabase): DayNoteDao = db.dayNoteDao()

    @Provides
    fun provideSharedScheduleDao(db: AppDatabase): SharedScheduleDao = db.sharedScheduleDao()
}
