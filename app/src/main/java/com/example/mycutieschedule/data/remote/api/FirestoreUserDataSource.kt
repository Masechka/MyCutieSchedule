package com.example.mycutieschedule.data.remote.api

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.data.remote.dto.UserDto

/**
 * Contract for Firestore user-profile operations.
 *
 * Implementation: [com.example.mycutieschedule.data.remote.datasource.FirestoreUserDataSourceImpl]
 * Firestore path: `users/{userId}`
 */
interface FirestoreUserDataSource {

    /**
     * Creates or overwrites the user document.
     * Called immediately after Firebase Auth sign-up to persist the profile.
     */
    suspend fun saveUser(user: UserDto): Result<Unit>

    /**
     * Fetches a user document by UID.
     * Returns [Result.Success] with `null` when the document doesn't exist yet.
     */
    suspend fun getUser(userId: String): Result<UserDto?>

    /**
     * Writes (or removes) the share code field on the user document.
     * Pass `null` to delete the existing share code (revoke sharing).
     */
    suspend fun updateShareCode(userId: String, shareCode: String?): Result<Unit>
}
