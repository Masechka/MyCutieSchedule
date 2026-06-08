package com.example.mycutieschedule.domain.usecase

import app.cash.turbine.test
import com.example.mycutieschedule.domain.fake.FakeEventRepository
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.model.EventColor
import com.example.mycutieschedule.domain.usecase.event.GetEventsForWeekUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class GetEventsForWeekUseCaseTest {

    private lateinit var repository: FakeEventRepository
    private lateinit var useCase: GetEventsForWeekUseCase

    // Week of 18–24 May 2026 (Mon 18 → Sun 24)
    private val mondayInWeek    = LocalDate.of(2026, 5, 18)
    private val wednesdayInWeek = LocalDate.of(2026, 5, 20)
    private val sundayInWeek    = LocalDate.of(2026, 5, 24)
    private val userId = "uid_test"

    @Before
    fun setUp() {
        repository = FakeEventRepository()
        useCase = GetEventsForWeekUseCase(repository)
    }

    // ── Week boundary computation ─────────────────────────────────────────────

    @Test
    fun `passing Monday returns events for the full Mon-Sun range`() = runTest {
        val eventOnMonday = buildEvent("1", mondayInWeek.atTime(9, 0), mondayInWeek.atTime(10, 0))
        val eventOnSunday = buildEvent("2", sundayInWeek.atTime(18, 0), sundayInWeek.atTime(19, 0))
        repository.addEvent(eventOnMonday)
        repository.addEvent(eventOnSunday)

        useCase(mondayInWeek, userId).test {
            val events = awaitItem()
            assertEquals(2, events.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `passing midweek day computes same week range as Monday`() = runTest {
        val event = buildEvent("1", wednesdayInWeek.atTime(14, 0), wednesdayInWeek.atTime(15, 0))
        repository.addEvent(event)

        useCase(wednesdayInWeek, userId).test {
            val items = awaitItem()
            assertEquals(1, items.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Filtering ─────────────────────────────────────────────────────────────

    @Test
    fun `event outside the week is excluded`() = runTest {
        val nextWeekEvent = buildEvent(
            "outside",
            LocalDate.of(2026, 5, 25).atTime(9, 0),
            LocalDate.of(2026, 5, 25).atTime(10, 0),
        )
        repository.addEvent(nextWeekEvent)

        useCase(mondayInWeek, userId).test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `events from another user are excluded`() = runTest {
        val anotherUserEvent = buildEvent(
            id = "other",
            start = wednesdayInWeek.atTime(9, 0),
            end = wednesdayInWeek.atTime(10, 0),
            ownerId = "other_uid",
        )
        repository.addEvent(anotherUserEvent)

        useCase(mondayInWeek, userId).test {
            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Reactivity ────────────────────────────────────────────────────────────

    @Test
    fun `flow re-emits when a new event is added`() = runTest {
        useCase(mondayInWeek, userId).test {
            assertTrue(awaitItem().isEmpty())   // initial empty state

            val newEvent = buildEvent("new", mondayInWeek.atTime(9, 0), mondayInWeek.atTime(10, 0))
            repository.addEvent(newEvent)

            val updated = awaitItem()
            assertEquals(1, updated.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `flow re-emits when an event is deleted`() = runTest {
        val event = buildEvent("del", mondayInWeek.atTime(9, 0), mondayInWeek.atTime(10, 0))
        repository.addEvent(event)

        useCase(mondayInWeek, userId).test {
            assertEquals(1, awaitItem().size)

            repository.deleteEvent("del")

            assertTrue(awaitItem().isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun buildEvent(
        id: String,
        start: LocalDateTime,
        end: LocalDateTime,
        ownerId: String = userId,
    ) = Event(
        id = id,
        title = "Test event $id",
        startDateTime = start,
        endDateTime = end,
        color = EventColor.ROSE,
        ownerId = ownerId,
    )
}
