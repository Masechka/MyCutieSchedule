package com.example.mycutieschedule.data.remote.datasource

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.core.util.toDomainResult
import com.example.mycutieschedule.data.remote.api.FirestoreCollections
import com.example.mycutieschedule.data.remote.api.FirestoreShareDataSource
import com.example.mycutieschedule.data.remote.dto.SharedScheduleDto
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreShareDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : FirestoreShareDataSource {

    /**
     * Share codes are always uppercase to avoid ambiguous characters (0/O, 1/l).
     * Document ID == share code, so lookups are O(1) with no index needed.
     */
    private fun shareDoc(shareCode: String) = firestore
        .collection(FirestoreCollections.SHARED_SCHEDULES)
        .document(shareCode.uppercase())

    override suspend fun createShareDocument(
        shareCode: String,
        userId: String,
        ownerName: String,
    ): Result<Unit> = runCatching {
        val dto = SharedScheduleDto(
            userId    = userId,
            ownerName = ownerName,
            shareCode = shareCode.uppercase(),
            createdAt = System.currentTimeMillis(),
        )
        shareDoc(shareCode).set(dto).await()
    }.toDomainResult()

    override suspend fun getByShareCode(shareCode: String): Result<SharedScheduleDto?> =
        runCatching {
            val snapshot = shareDoc(shareCode).get().await()
            if (!snapshot.exists()) return@runCatching null
            snapshot.toObject(SharedScheduleDto::class.java)
        }.toDomainResult()

    override suspend fun deleteShareDocument(userId: String, shareCode: String): Result<Unit> =
        runCatching {
            shareDoc(shareCode).delete().await()
        }.toDomainResult()
}
