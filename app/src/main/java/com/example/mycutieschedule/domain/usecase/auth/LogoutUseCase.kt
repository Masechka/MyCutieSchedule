package com.example.mycutieschedule.domain.usecase.auth

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.logout()
}
