package com.example.mycutieschedule.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.usecase.auth.SignInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the Sign In screen.
 *
 * @param emailError      Non-null = show error under the email field.
 * @param passwordError   Non-null = show error under the password field.
 * @param isLoading       True while the Firebase sign-in call is in-flight.
 */
data class SignInUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
)

/**
 * ViewModel for [SignInScreen].
 *
 * Responsibilities:
 *   1. Hold form state (email, password, visibility toggle, field errors).
 *   2. Run client-side validation before hitting Firebase.
 *   3. Delegate auth to [SignInUseCase] and broadcast snackbar messages on failure.
 *
 * On success: Firebase auth state updates → [AppViewModel.authState] emits
 * [AuthState.Authenticated] → [AppContent.LaunchedEffect] navigates to MainGraph.
 * This ViewModel does NOT navigate — it just reacts to the use-case result.
 *
 * Snackbar pattern: one-shot messages are sent via [Channel] (not StateFlow) so
 * they are not re-shown on recomposition or configuration change.
 */
@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInUseCase: SignInUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignInUiState())
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    private val _snackbarChannel = Channel<String>(Channel.BUFFERED)
    val snackbarMessages = _snackbarChannel.receiveAsFlow()

    // ── Form event handlers ───────────────────────────────────────────────────

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, passwordError = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    fun onSignIn() {
        val state = _uiState.value
        if (!validateLocally(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = signInUseCase(state.email.trim(), state.password)) {
                is Result.Success -> {
                    // Navigation is handled by AppContent observing auth state.
                    // Reset loading in case the screen is briefly still visible
                    // during the exit animation.
                    _uiState.update { it.copy(isLoading = false) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _snackbarChannel.send(result.message.toRussian())
                }
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Checks obvious field-level errors before making a network call.
     * Returns false (and sets field errors) if validation fails.
     */
    private fun validateLocally(state: SignInUiState): Boolean {
        var valid = true
        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Введите email") }
            valid = false
        }
        if (state.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Введите пароль") }
            valid = false
        }
        return valid
    }
}

/**
 * Maps English error strings (from use cases + [FirebaseErrorMapper]) to Russian.
 * Keeps the data layer language-agnostic while giving users Russian messages.
 */
internal fun String?.toRussian(): String = when (this) {
    "Email cannot be empty"         -> "Введите email"
    "Invalid email format"          -> "Неверный формат email"
    "Password cannot be empty"      -> "Введите пароль"
    "Invalid email or password."    -> "Неверный email или пароль"
    "An account with this email already exists." -> "Аккаунт с таким email уже существует"
    "No internet connection. Your changes will sync when you're back online." ->
        "Нет подключения к интернету"
    "Service temporarily unavailable. Working offline." ->
        "Сервис временно недоступен"
    "You don't have permission to access this." ->
        "Недостаточно прав доступа"
    else -> this ?: "Произошла ошибка. Попробуйте ещё раз"
}
