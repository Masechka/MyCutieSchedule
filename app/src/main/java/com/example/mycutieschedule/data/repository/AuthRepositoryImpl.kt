package com.example.mycutieschedule.data.repository

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.core.util.toDomainResult
import com.example.mycutieschedule.data.mapper.toDto
import com.example.mycutieschedule.data.remote.util.toFirebaseError
import com.example.mycutieschedule.data.remote.api.FirestoreUserDataSource
import com.example.mycutieschedule.domain.model.AuthState
import com.example.mycutieschedule.domain.model.User
import com.example.mycutieschedule.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

/**
 * Firebase implementation of [AuthRepository].
 *
 * Sign-in / sign-up delegate to [FirebaseAuth].
 * After sign-up a Firestore user document is created via [FirestoreUserDataSource]
 * so other features (share codes, display name) are always available.
 *
 * Auth state is observed via [callbackFlow]:
 *   Firebase fires [FirebaseAuth.AuthStateListener] on the main thread whenever
 *   the user signs in or out. [callbackFlow] bridges this callback into a coroutine
 *   Flow without blocking the listener thread. [awaitClose] removes the listener
 *   when the Flow's coroutine scope is cancelled.
 */
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val userDataSource: FirestoreUserDataSource,
) : AuthRepository {

    // ── Auth state ────────────────────────────────────────────────────────────

    override fun observeAuthState(): Flow<AuthState> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser?.toDomainUser()
            trySend(
                if (user != null) AuthState.Authenticated(user)
                else AuthState.Unauthenticated,
            )
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override fun getCurrentUser(): User? = auth.currentUser?.toDomainUser()

    // ── Sign-in ───────────────────────────────────────────────────────────────

    override suspend fun signIn(email: String, password: String): Result<User> =
        runCatching {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.toDomainUser()
                ?: error("Sign-in succeeded but no FirebaseUser was returned.")
        }.fold(
            onSuccess = { Result.Success(it) },
            // toFirebaseError() gives human-readable messages (vs raw Firebase exception text)
            onFailure = { it.toFirebaseError() },
        )

    // ── Sign-up ───────────────────────────────────────────────────────────────

    /**
     * Three-step sign-up:
     * 1. Create Firebase Auth account.
     * 2. Write [displayName] to the Firebase Auth profile (shown in Firebase Console).
     * 3. Create a Firestore user document so other features can look up display names.
     *
     * Step 3 failure is non-fatal: the account still exists and the document can be
     * recreated on next sign-in. We log the error and return the user normally.
     */
    override suspend fun signUp(
        email: String,
        password: String,
        displayName: String,
    ): Result<User> = runCatching {
        // Step 1 — create account
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user
            ?: error("Sign-up succeeded but no FirebaseUser was returned.")

        // Step 2 — update Auth profile
        val profileUpdates = userProfileChangeRequest { this.displayName = displayName }
        firebaseUser.updateProfile(profileUpdates).await()

        // Step 3 — create Firestore user document
        val user = User(
            id          = firebaseUser.uid,
            email       = email,
            displayName = displayName,
        )
        val saveResult = userDataSource.saveUser(user.toDto())
        if (saveResult is Result.Error) {
            Timber.w(
                saveResult.exception,
                "Firestore user document creation failed for uid=${user.id}. " +
                    "Will be retried on next sign-in.",
            )
        }
        user
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { it.toFirebaseError() },
    )

    // ── Logout ────────────────────────────────────────────────────────────────

    override suspend fun logout(): Result<Unit> = runCatching {
        auth.signOut()
    }.toDomainResult()
}

// ── Internal helpers ──────────────────────────────────────────────────────────

/**
 * Converts a [FirebaseUser] to a domain [User].
 * Private to this file — Firebase types must not leak beyond the data layer.
 */
private fun FirebaseUser.toDomainUser(): User = User(
    id          = uid,
    email       = email ?: "",
    displayName = displayName ?: "",
)
