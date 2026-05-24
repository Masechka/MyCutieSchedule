package com.example.mycutieschedule.data.remote.datasource

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.core.util.toDomainResult
import com.example.mycutieschedule.data.remote.api.FirestoreCollections
import com.example.mycutieschedule.data.remote.api.FirestoreUserDataSource
import com.example.mycutieschedule.data.remote.dto.UserDto
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirestoreUserDataSourceImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
) : FirestoreUserDataSource {

    private fun userDoc(userId: String) = firestore
        .collection(FirestoreCollections.USERS)
        .document(userId)

    override suspend fun saveUser(user: UserDto): Result<Unit> = runCatching {
        // set() overwrites the entire document — safe for initial creation.
        userDoc(user.id).set(user).await()
    }.toDomainResult()

    override suspend fun getUser(userId: String): Result<UserDto?> = runCatching {
        val snapshot = userDoc(userId).get().await()
        if (!snapshot.exists()) return@runCatching null
        snapshot.toObject(UserDto::class.java)?.copy(id = snapshot.id)
    }.toDomainResult()

    override suspend fun updateShareCode(userId: String, shareCode: String?): Result<Unit> =
        runCatching {
            // Pass FieldValue.delete() to remove the field when shareCode is null (revoke).
            val value: Any = shareCode ?: FieldValue.delete()
            userDoc(userId)
                .update(FirestoreCollections.UserFields.SHARE_CODE, value)
                .await()
        }.toDomainResult()
}
