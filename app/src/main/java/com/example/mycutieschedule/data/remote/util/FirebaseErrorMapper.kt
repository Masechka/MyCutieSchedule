package com.example.mycutieschedule.data.remote.util

import com.example.mycutieschedule.core.util.Result
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Translates raw Firebase exceptions into human-readable [Result.Error] messages.
 *
 * **Why this exists:** Firebase throws platform-specific exception subtypes.
 * The domain layer must not depend on Firebase, so error messages are resolved
 * here in the data layer before being wrapped in [Result.Error].
 *
 * Usage: call via the extension inside `runCatching` failure handlers:
 * ```
 * runCatching { ... }.fold(
 *     onSuccess = { Result.Success(it) },
 *     onFailure = { it.toFirebaseError() },
 * )
 * ```
 */
fun Throwable.toFirebaseError(): Result.Error = Result.Error(
    exception = this,
    message = when (this) {
        is FirebaseAuthInvalidCredentialsException ->
            "Invalid email or password."

        is FirebaseAuthUserCollisionException ->
            "An account with this email already exists."

        is FirebaseNetworkException ->
            "No internet connection. Your changes will sync when you're back online."

        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.UNAVAILABLE ->
                "Service temporarily unavailable. Working offline."
            FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "You don't have permission to access this."
            FirebaseFirestoreException.Code.NOT_FOUND ->
                "The requested data was not found."
            else ->
                message ?: "An unexpected Firestore error occurred."
        }

        else -> message ?: "An unexpected error occurred."
    },
)
