package com.example.mycutieschedule.presentation

import androidx.lifecycle.ViewModel
import com.example.mycutieschedule.domain.model.AuthState
import com.example.mycutieschedule.domain.model.ThemeMode
import com.example.mycutieschedule.domain.repository.AuthRepository
import com.example.mycutieschedule.domain.usecase.auth.ObserveAuthStateUseCase
import com.example.mycutieschedule.domain.usecase.settings.ObserveThemeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import javax.inject.Inject

/**
 * ViewModel scoped to the root [AppContent] composable.
 *
 * Sole responsibility: expose a [StateFlow] of [AuthState] so [AppContent]
 * can navigate between the auth graph and the main graph reactively.
 *
 * **Why [SharingStarted.Eagerly]?**
 * Auth state must be known before the first frame is rendered to avoid a
 * visible flash of the wrong screen. `Eagerly` starts the upstream
 * `callbackFlow` immediately on ViewModel creation, not when the first
 * collector subscribes.
 *
 * **Why inject [AuthRepository] directly (not a UseCase)?**
 * [AuthRepository.getCurrentUser] is a synchronous, non-suspending call
 * used only to compute the correct [initialValue]. No use-case wraps a
 * simple synchronous getter — that would be over-engineering. The Flow
 * itself still comes through [ObserveAuthStateUseCase].
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    observeAuthState: ObserveAuthStateUseCase,
    observeTheme: ObserveThemeUseCase,
) : ViewModel() {

    val authState: StateFlow<AuthState> = observeAuthState()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.Eagerly,
            // Synchronous read from FirebaseAuth.currentUser — correct before first emission.
            // Prevents the auth graph flashing briefly when the user is already signed in.
            initialValue = authRepository.getCurrentUser()
                ?.let  { AuthState.Authenticated(it) }
                ?: AuthState.Unauthenticated,
        )

    /**
     * User-selected theme preference, persisted in DataStore.
     *
     * Eagerly started so the very first composition of [AppContent] already has
     * the right value — eliminates a flash from default → user choice on cold
     * launches. Defaults to [ThemeMode.SYSTEM] until DataStore emits.
     */
    val themeMode: StateFlow<ThemeMode> = observeTheme()
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.Eagerly,
            initialValue = ThemeMode.SYSTEM,
        )
}
