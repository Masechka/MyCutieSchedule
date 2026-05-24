package com.example.mycutieschedule.domain.model

import java.time.LocalDateTime

/**
 * Authenticated user in the domain layer.
 *
 * Maps to a Firebase Auth user + a Firestore user document (users/{id}).
 *
 * @param id          Firebase Auth UID.
 * @param email       User's email address (used for sign-in).
 * @param displayName Human-readable name shown in the profile screen.
 * @param createdAt   Account creation time (UTC).
 */
data class User(
    val id: String,
    val email: String,
    val displayName: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
