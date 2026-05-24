package com.example.mycutieschedule.data.remote.api

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.data.remote.dto.SharedScheduleDto

/**
 * Contract for Firestore share-code lookup operations.
 *
 * Implementation: [com.example.mycutieschedule.data.remote.datasource.FirestoreShareDataSourceImpl]
 * Firestore path: `sharedSchedules/{shareCode}`
 */
interface FirestoreShareDataSource {

    /**
     * Creates a share-code document that maps [shareCode] → [userId].
     * Called when a user enables schedule sharing for the first time.
     */
    suspend fun createShareDocument(
        shareCode: String,
        userId: String,
        ownerName: String,
    ): Result<Unit>

    /**
     * Looks up a share code.
     * Returns [Result.Success] with `null` when the code doesn't exist.
     */
    suspend fun getByShareCode(shareCode: String): Result<SharedScheduleDto?>

    /**
     * Deletes the share-code document identified by [shareCode].
     * Called when the owner revokes their share link.
     */
    suspend fun deleteShareDocument(userId: String, shareCode: String): Result<Unit>
}
