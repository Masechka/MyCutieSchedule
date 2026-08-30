package com.example.mycutieschedule.domain.fake

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.repository.EventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime

/**
 * In-memory fake for [EventRepository].
 * Used in unit tests — no Room, no Firestore, no coroutines infrastructure needed.
 *
 * Controllable failure mode:
 *   fake.shouldFailWith = RuntimeException("network error")
 *   → next write operation returns Result.Error
 */
class FakeEventRepository : EventRepository {

    private val eventsFlow = MutableStateFlow<List<Event>>(emptyList())

    /** Set to a non-null throwable to make the next write call return Result.Error. */
    var shouldFailWith: Throwable? = null

    /** All events currently in the fake store. */
    val events: List<Event> get() = eventsFlow.value

    override fun observeEventsBetween(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        userId: String,
    ): Flow<List<Event>> = eventsFlow.map { list ->
        list.filter { event ->
            event.ownerId == userId &&
            event.startDateTime < endDateTime &&
            event.endDateTime > startDateTime
        }
    }

    override suspend fun getEventById(eventId: String): Event? =
        eventsFlow.value.find { it.id == eventId }

    override suspend fun addEvent(event: Event): Result<Unit> {
        shouldFailWith?.let { return Result.Error(it).also { shouldFailWith = null } }
        eventsFlow.value = eventsFlow.value + event
        return Result.Success(Unit)
    }

    override suspend fun addAllEvents(events: List<Event>): Result<Unit> {
        shouldFailWith?.let { return Result.Error(it).also { shouldFailWith = null } }
        eventsFlow.value = eventsFlow.value + events
        return Result.Success(Unit)
    }

    override suspend fun updateEvent(event: Event): Result<Unit> {
        shouldFailWith?.let { return Result.Error(it).also { shouldFailWith = null } }
        eventsFlow.value = eventsFlow.value.map {
            if (it.id == event.id) event else it
        }
        return Result.Success(Unit)
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        shouldFailWith?.let { return Result.Error(it).also { shouldFailWith = null } }
        eventsFlow.value = eventsFlow.value.filter { it.id != eventId }
        return Result.Success(Unit)
    }

    override suspend fun deleteEventAndFollowing(
        recurrenceGroupId: String,
        fromStart: LocalDateTime,
    ): Result<Unit> {
        shouldFailWith?.let { return Result.Error(it).also { shouldFailWith = null } }
        eventsFlow.value = eventsFlow.value.filterNot { event ->
            event.recurrenceGroupId == recurrenceGroupId && event.startDateTime >= fromStart
        }
        return Result.Success(Unit)
    }
}
