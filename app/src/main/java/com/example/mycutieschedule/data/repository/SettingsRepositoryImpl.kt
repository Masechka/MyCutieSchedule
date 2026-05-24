package com.example.mycutieschedule.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.mycutieschedule.domain.model.ThemeMode
import com.example.mycutieschedule.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    override fun observeThemeMode(): Flow<ThemeMode> =
        dataStore.data
            .catch { cause ->
                // DataStore can throw IOException on first read if the file is corrupted.
                // Recover gracefully by emitting empty preferences (→ default theme).
                if (cause is IOException) emit(emptyPreferences()) else throw cause
            }
            .map { prefs ->
                val name = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
                // valueOf with fallback: if a future schema adds a new ThemeMode that
                // an older install doesn't recognize, default to SYSTEM.
                ThemeMode.entries.find { it.name == name } ?: ThemeMode.SYSTEM
            }

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
