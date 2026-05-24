package com.example.mycutieschedule.di

import com.example.mycutieschedule.data.repository.DayNoteRepositoryImpl
import com.example.mycutieschedule.data.repository.EventRepositoryImpl
import com.example.mycutieschedule.data.repository.SettingsRepositoryImpl
import com.example.mycutieschedule.domain.repository.DayNoteRepository
import com.example.mycutieschedule.domain.repository.EventRepository
import com.example.mycutieschedule.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Bridges domain repository interfaces to their data-layer implementations.
 *
 * Why @Binds instead of @Provides:
 *   @Binds is more efficient — it doesn't generate a factory class, just an alias.
 *   Use @Provides only when you need to call a constructor with arguments that
 *   Hilt can't infer (e.g. third-party classes).
 *
 * Pending bindings (added in Stage 5 — Firebase remote layer):
 *   AuthRepository  → AuthRepositoryImpl
 *   ShareRepository → ShareRepositoryImpl
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindEventRepository(
        impl: EventRepositoryImpl,
    ): EventRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl,
    ): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindDayNoteRepository(
        impl: DayNoteRepositoryImpl,
    ): DayNoteRepository
}
