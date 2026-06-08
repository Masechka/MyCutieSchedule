package com.example.mycutieschedule.domain.usecase.auth

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.User
import com.example.mycutieschedule.domain.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        if (email.isBlank()) {
            return Result.Error(IllegalArgumentException("Email cannot be empty"))
        }
        if (!isValidEmail(email)) {
            return Result.Error(IllegalArgumentException("Invalid email format"))
        }
        if (password.isBlank()) {
            return Result.Error(IllegalArgumentException("Password cannot be empty"))
        }
        return repository.signIn(email.trim(), password)
    }

    private fun isValidEmail(email: String): Boolean =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$").matches(email.trim())
}
