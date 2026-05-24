package com.example.mycutieschedule.domain.model

/**
 * Current authentication state observed via [AuthRepository.observeAuthState].
 *
 * Emitted as a Flow — the root composable reacts to changes and routes to
 * AuthScreen or CalendarScreen accordingly.
 */
sealed class AuthState {

    /** The user is signed in. [user] contains their profile. */
    data class Authenticated(val user: User) : AuthState()

    /** No user is signed in. Navigate to the auth flow. */
    data object Unauthenticated : AuthState()
}
