package com.example.mycutieschedule.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycutieschedule.domain.model.ThemeMode
import com.example.mycutieschedule.domain.usecase.settings.ChangeThemeUseCase
import com.example.mycutieschedule.domain.usecase.settings.ObserveThemeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for [SettingsScreen].
 *
 * Single source of truth is the [ObserveThemeUseCase] flow — selecting a new
 * mode goes through [ChangeThemeUseCase], DataStore persists it, and the same
 * flow re-emits the new value here. No optimistic local state needed.
 */
data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    observeTheme: ObserveThemeUseCase,
    private val changeTheme: ChangeThemeUseCase,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = observeTheme()
        .map { SettingsUiState(themeMode = it) }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState(),
        )

    fun setThemeMode(mode: ThemeMode) {
        if (mode == uiState.value.themeMode) return  // no-op for identical selection
        viewModelScope.launch { changeTheme(mode) }
    }
}
