package com.example.mycutieschedule.domain.repository

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.AuthState
import com.example.mycutieschedule.domain.model.User
import kotlinx.coroutines.flow.Flow

/**
 * Contract for authentication operations.
 *
 * Implementation lives in data/repository/AuthRepositoryImpl.kt
 * and delegates to Firebase Authentication.
 */
interface AuthRepository {

    /**
     * Signs in an existing user with email and password.
     * Returns [Result.Success] with the authenticated [User], or [Result.Error].
     */
    suspend fun signIn(email: String, password: String): Result<User>

    /**
     * Creates a new Firebase Auth account and a Firestore user document.
     * Returns [Result.Success] with the new [User], or [Result.Error].
     */
    suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<User>

    /**
     * Signs out the current user and clears any cached credentials.
     */
    suspend fun logout(): Result<Unit>

    /**
     * Hot Flow of the current authentication state.
     * Immediately emits the current state on collection,
     * then emits again whenever the state changes (sign in / sign out).
     */
    fun observeAuthState(): Flow<AuthState>

    /**
     * Returns the currently signed-in user, or null if not authenticated.
     * Non-suspending — reads from the in-memory cache.
     */
    fun getCurrentUser(): User?
}
