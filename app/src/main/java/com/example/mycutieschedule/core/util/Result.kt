package com.example.mycutieschedule.core.util

/**
 * Custom result wrapper for async domain operations.
 *
 * Used as the return type of all suspend use cases and repository methods.
 * Loading state is NOT included here — it belongs to UiState in the presentation layer.
 *
 * Usage:
 *   when (val result = useCase(event)) {
 *       is Result.Success -> // result.data
 *       is Result.Error   -> // result.exception / result.message
 *   }
 */
sealed class Result<out T> {

    data class Success<T>(val data: T) : Result<T>()

    data class Error(
        val exception: Throwable,
        val message: String? = exception.message,
    ) : Result<Nothing>()
}

// ── Extension helpers ─────────────────────────────────────────────────────────

inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(data)
    return this
}

inline fun <T> Result<T>.onError(action: (Throwable, String?) -> Unit): Result<T> {
    if (this is Result.Error) action(exception, message)
    return this
}

inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error   -> this
}

fun <T> Result<T>.getOrNull(): T? = if (this is Result.Success) data else null

fun <T> Result<T>.isSuccess(): Boolean = this is Result.Success
fun <T> Result<T>.isError(): Boolean   = this is Result.Error

/**
 * Bridges Kotlin's built-in [kotlin.Result] to our domain [Result].
 *
 * Typical use in repository implementations:
 * ```
 * override suspend fun addEvent(event: Event): Result<Unit> = runCatching {
 *     eventDao.insert(event.toEntity())
 * }.toDomainResult()
 * ```
 */
fun <T> kotlin.Result<T>.toDomainResult(): Result<T> = fold(
    onSuccess = { Result.Success(it) },
    onFailure = { Result.Error(it) },
)
