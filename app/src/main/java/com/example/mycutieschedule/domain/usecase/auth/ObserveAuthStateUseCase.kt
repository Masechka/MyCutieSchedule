package com.example.mycutieschedule.domain.usecase.auth

import com.example.mycutieschedule.domain.model.AuthState
import com.example.mycutieschedule.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Observes the authentication state as a hot Flow.
 * The root ViewModel collects this to decide which navigation graph to show.
 */
class ObserveAuthStateUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    operator fun invoke(): Flow<AuthState> = repository.observeAuthState()
}
