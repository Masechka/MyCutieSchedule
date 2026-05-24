package com.example.mycutieschedule.domain.usecase.auth

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.User
import com.example.mycutieschedule.domain.repository.AuthRepository
import javax.inject.Inject

private const val MIN_PASSWORD_LENGTH = 6
private const val MAX_DISPLAY_NAME_LENGTH = 50

class SignUpUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String,
    ): Result<User> {
        if (email.isBlank()) {
            return Result.Error(IllegalArgumentException("Email cannot be empty"))
        }
        if (!isValidEmail(email)) {
            return Result.Error(IllegalArgumentException("Invalid email format"))
        }
        if (password.length < MIN_PASSWORD_LENGTH) {
            return Result.Error(
                IllegalArgumentException("Password must be at least $MIN_PASSWORD_LENGTH characters")
            )
        }
        if (displayName.isBlank()) {
            return Result.Error(IllegalArgumentException("Name cannot be empty"))
        }
        if (displayName.length > MAX_DISPLAY_NAME_LENGTH) {
            return Result.Error(
                IllegalArgumentException("Name must be $MAX_DISPLAY_NAME_LENGTH characters or fewer")
            )
        }
        return repository.signUp(email.trim(), password, displayName.trim())
    }

    private fun isValidEmail(email: String): Boolean =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$").matches(email.trim())
}
