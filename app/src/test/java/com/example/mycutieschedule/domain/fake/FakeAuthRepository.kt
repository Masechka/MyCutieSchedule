package com.example.mycutieschedule.domain.fake

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.AuthState
import com.example.mycutieschedule.domain.model.User
import com.example.mycutieschedule.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAuthRepository : AuthRepository {

    private val authStateFlow = MutableStateFlow<AuthState>(AuthState.Unauthenticated)

    var shouldFailWith: Throwable? = null
    var signedInUser: User? = null

    override suspend fun signIn(email: String, password: String): Result<User> {
        shouldFailWith?.let { return Result.Error(it).also { shouldFailWith = null } }
        val user = User(id = "uid_test", email = email, displayName = "Test User")
        signedInUser = user
        authStateFlow.value = AuthState.Authenticated(user)
        return Result.Success(user)
    }

    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<User> {
        shouldFailWith?.let { return Result.Error(it).also { shouldFailWith = null } }
        val user = User(id = "uid_new", email = email, displayName = displayName)
        signedInUser = user
        authStateFlow.value = AuthState.Authenticated(user)
        return Result.Success(user)
    }

    override suspend fun logout(): Result<Unit> {
        signedInUser = null
        authStateFlow.value = AuthState.Unauthenticated
        return Result.Success(Unit)
    }

    override fun observeAuthState(): Flow<AuthState> = authStateFlow

    override fun getCurrentUser(): User? = signedInUser
}
