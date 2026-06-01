package com.example.mycutieschedule.presentation.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.User
import com.example.mycutieschedule.domain.repository.AuthRepository
import com.example.mycutieschedule.domain.usecase.auth.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── One-shot UI events ────────────────────────────────────────────────────────

sealed class ProfileEvent {
    /** Logout succeeded — root AppContent observes auth state and routes to AuthGraph. */
    data object LoggedOut : ProfileEvent()
    data class ShowError(val message: String) : ProfileEvent()
}

// ── UI state ──────────────────────────────────────────────────────────────────

data class ProfileUiState(
    /** Resolved current user; null when not signed in (or briefly during logout). */
    val user: User? = null,
    /** True while logout is in flight — disables the button to prevent double-taps. */
    val isLoggingOut: Boolean = false,
    /** Confirmation dialog visibility. */
    val showLogoutConfirm: Boolean = false,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * Owns the Profile screen state.
 *
 * Reads [User] synchronously from [AuthRepository.getCurrentUser] — the user
 * info is already cached by Firebase Auth so we don't need a Flow.
 * Logout goes through [LogoutUseCase]; on success a one-shot event lets the
 * screen navigate back, while the root [com.example.mycutieschedule.presentation.AppContent]
 * detects the auth state change and switches to the auth graph.
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ProfileUiState(user = authRepository.getCurrentUser()),
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun requestLogout() {
        _uiState.update { it.copy(showLogoutConfirm = true) }
    }

    fun dismissLogoutConfirm() {
        _uiState.update { it.copy(showLogoutConfirm = false) }
    }

    fun confirmLogout() {
        if (_uiState.value.isLoggingOut) return
        _uiState.update { it.copy(isLoggingOut = true, showLogoutConfirm = false) }
        viewModelScope.launch {
            when (val result = logoutUseCase()) {
                is Result.Success -> _events.send(ProfileEvent.LoggedOut)
                is Result.Error   -> {
                    _uiState.update { it.copy(isLoggingOut = false) }
                    _events.send(
                        ProfileEvent.ShowError(
                            "Не удалось выйти: ${result.message ?: "неизвестная ошибка"}"
                        )
                    )
                }
            }
        }
    }
}
