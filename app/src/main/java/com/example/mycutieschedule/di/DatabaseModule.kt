package com.example.mycutieschedule.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for the local Room database.
 *
 * Populated in Stage 4 when the following are defined:
 *   - AppDatabase         (data/local/database/)
 *   - EventEntity         (data/local/entity/)
 *   - ScheduleEntity      (data/local/entity/)
 *   - DayNoteEntity       (data/local/entity/)
 *   - SharedScheduleEntity(data/local/entity/)
 *   - EventDao            (data/local/dao/)
 *
 * Will provide:
 *   @Provides @Singleton fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase
 *   @Provides fun provideEventDao(db: AppDatabase): EventDao
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule
