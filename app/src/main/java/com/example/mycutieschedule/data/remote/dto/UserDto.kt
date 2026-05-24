package com.example.mycutieschedule.data.remote.dto

/**
 * Firestore representation of a user profile.
 *
 * **All fields must have default values** for Firebase reflection-based deserialization.
 *
 * Firestore path: `users/{id}`
 * Maps to domain: [com.example.mycutieschedule.domain.model.User]
 */
data class UserDto(
    val id: String = "",
    val email: String = "",
    val displayName: String = "",

    /** UTC epoch milliseconds — when the account was created. */
    val createdAt: Long = 0L,

    /**
     * 6-character alphanumeric share code.
     * Null when the user has never enabled schedule sharing.
     */
    val shareCode: String? = null,
)
