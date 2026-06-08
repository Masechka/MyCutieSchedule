package com.example.mycutieschedule.domain.repository

import com.example.mycutieschedule.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

/**
 * Contract for user preferences storage.
 *
 * Implementation: data/repository/SettingsRepositoryImpl.kt
 * Backend: DataStore<Preferences>
 *
 * Note: operations here do not return Result<T> because DataStore
 * writes are fire-and-forget (they never meaningfully fail in our use cases).
 */
interface SettingsRepository {

    /**
     * Persists the user's preferred [ThemeMode].
     * The new value is immediately reflected in [observeThemeMode].
     */
    suspend fun setThemeMode(mode: ThemeMode)

    /**
     * Hot Flow of the current theme mode.
     * Defaults to [ThemeMode.SYSTEM] if the user has never set a preference.
     */
    fun observeThemeMode(): Flow<ThemeMode>
}
