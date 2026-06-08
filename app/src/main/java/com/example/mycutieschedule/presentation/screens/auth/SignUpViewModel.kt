package com.example.mycutieschedule.presentation.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.usecase.auth.SignUpUseCase
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
 * UI state for the Sign Up screen.
 *
 * @param isPasswordVisible        Toggles password field between visible/hidden.
 * @param isConfirmPasswordVisible Toggles confirm-password field.
 * @param nameError / emailError / passwordError / confirmPasswordError
 *        Non-null = show the string as a supporting error text under that field.
 * @param isLoading True while the Firebase account-creation call is in-flight.
 */
data class SignUpUiState(
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
)

@HiltViewModel
class SignUpViewModel @Inject constructor(
    private val signUpUseCase: SignUpUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState.asStateFlow()

    private val _snackbarChannel = Channel<String>(Channel.BUFFERED)
    val snackbarMessages = _snackbarChannel.receiveAsFlow()

    // ── Form event handlers ───────────────────────────────────────────────────

    fun onNameChange(value: String) {
        _uiState.update { it.copy(displayName = value, nameError = null) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, emailError = null) }
    }

    fun onPasswordChange(value: String) {
        // Clear confirm-password mismatch error when user edits the original password
        _uiState.update { it.copy(password = value, passwordError = null, confirmPasswordError = null) }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update { it.copy(confirmPassword = value, confirmPasswordError = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onToggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    fun onSignUp() {
        val state = _uiState.value
        if (!validateLocally(state)) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = signUpUseCase(
                email       = state.email.trim(),
                password    = state.password,
                displayName = state.displayName.trim(),
            )) {
                is Result.Success -> {
                    // AppContent.LaunchedEffect navigates when authState → Authenticated
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

    private fun validateLocally(state: SignUpUiState): Boolean {
        var valid = true

        if (state.displayName.isBlank()) {
            _uiState.update { it.copy(nameError = "Введите имя") }
            valid = false
        } else if (state.displayName.length > 50) {
            _uiState.update { it.copy(nameError = "Имя не должно превышать 50 символов") }
            valid = false
        }

        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Введите email") }
            valid = false
        }

        if (state.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Не менее 6 символов") }
            valid = false
        }

        if (state.confirmPassword != state.password) {
            _uiState.update { it.copy(confirmPasswordError = "Пароли не совпадают") }
            valid = false
        }

        return valid
    }
}
