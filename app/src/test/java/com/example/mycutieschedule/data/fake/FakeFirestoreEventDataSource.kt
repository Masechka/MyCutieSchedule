package com.example.mycutieschedule.data.fake

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.data.remote.api.FirestoreEventDataSource
import com.example.mycutieschedule.data.remote.dto.EventDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory fake for [FirestoreEventDataSource].
 *
 * [remoteEvents] — это «что лежит в облаке». Поток из [observeEvents] никогда
 * не завершается (как настоящий snapshot listener), поэтому sync-job остаётся
 * живым на всё время теста.
 */
class FakeFirestoreEventDataSource(
    initial: List<EventDto> = emptyList(),
) : FirestoreEventDataSource {

    private val remote = MutableStateFlow(initial)

    /** Заменяет содержимое «облака» и выпускает новый снапшот. */
    fun emit(events: List<EventDto>) {
        remote.value = events
    }

    val savedEvents = mutableListOf<EventDto>()
    val deletedEventIds = mutableListOf<String>()

    override fun observeEvents(userId: String): Flow<List<EventDto>> = remote

    override suspend fun fetchOnce(userId: String): Result<List<EventDto>> =
        Result.Success(remote.value)

    override suspend fun saveEvent(event: EventDto, userId: String): Result<Unit> {
        savedEvents += event
        return Result.Success(Unit)
    }

    override suspend fun deleteEvent(eventId: String, userId: String): Result<Unit> {
        deletedEventIds += eventId
        return Result.Success(Unit)
    }
}
