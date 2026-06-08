package com.example.mycutieschedule.domain.usecase

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.fake.FakeEventRepository
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.model.EventColor
import com.example.mycutieschedule.domain.usecase.event.AddEventUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class AddEventUseCaseTest {

    private lateinit var repository: FakeEventRepository
    private lateinit var useCase: AddEventUseCase

    private val baseStart = LocalDateTime.of(2026, 5, 24, 10, 0)
    private val baseEnd   = LocalDateTime.of(2026, 5, 24, 11, 0)

    @Before
    fun setUp() {
        repository = FakeEventRepository()
        useCase = AddEventUseCase(repository)
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    fun `valid event is saved and returns Success`() = runTest {
        val event = buildEvent(title = "Stand-up", start = baseStart, end = baseEnd)

        val result = useCase(event)

        assertTrue(result is Result.Success)
        assertEquals(1, repository.events.size)
    }

    @Test
    fun `when event id is blank a uuid is assigned before saving`() = runTest {
        val event = buildEvent(id = "", title = "Stand-up", start = baseStart, end = baseEnd)

        useCase(event)

        val saved = repository.events.first()
        assertTrue("ID should be non-blank UUID", saved.id.isNotBlank())
    }

    @Test
    fun `when event already has an id that id is preserved`() = runTest {
        val event = buildEvent(id = "my-fixed-id", title = "Stand-up", start = baseStart, end = baseEnd)

        useCase(event)

        assertEquals("my-fixed-id", repository.events.first().id)
    }

    // ── Validation — title ────────────────────────────────────────────────────

    @Test
    fun `blank title returns Error`() = runTest {
        val event = buildEvent(title = "", start = baseStart, end = baseEnd)

        val result = useCase(event)

        assertIsError(result, "title")
    }

    @Test
    fun `whitespace-only title returns Error`() = runTest {
        val event = buildEvent(title = "   ", start = baseStart, end = baseEnd)

        val result = useCase(event)

        assertIsError(result, "title")
    }

    // ── Validation — time range ───────────────────────────────────────────────

    @Test
    fun `end before start returns Error`() = runTest {
        val event = buildEvent(title = "Bad event", start = baseEnd, end = baseStart)

        val result = useCase(event)

        assertIsError(result, "Start")
    }

    @Test
    fun `end equal to start returns Error`() = runTest {
        val event = buildEvent(title = "Zero duration", start = baseStart, end = baseStart)

        val result = useCase(event)

        assertIsError(result, "Start")
    }

    // ── Repository failure ────────────────────────────────────────────────────

    @Test
    fun `repository failure is propagated as Error`() = runTest {
        repository.shouldFailWith = RuntimeException("DB error")
        val event = buildEvent(title = "Stand-up", start = baseStart, end = baseEnd)

        val result = useCase(event)

        assertTrue(result is Result.Error)
    }

    @Test
    fun `nothing is stored when repository fails`() = runTest {
        repository.shouldFailWith = RuntimeException("DB error")
        val event = buildEvent(title = "Stand-up", start = baseStart, end = baseEnd)

        useCase(event)

        assertTrue(repository.events.isEmpty())
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun buildEvent(
        id: String = "",
        title: String = "Test event",
        start: LocalDateTime = baseStart,
        end: LocalDateTime = baseEnd,
        ownerId: String = "uid_test",
    ) = Event(
        id = id,
        title = title,
        startDateTime = start,
        endDateTime = end,
        color = EventColor.PEONY,
        ownerId = ownerId,
    )

    private fun assertIsError(result: Result<*>, messageFragment: String) {
        assertTrue("Expected Result.Error but got $result", result is Result.Error)
        val error = result as Result.Error
        assertNotNull(error.message)
        assertTrue(
            "Expected error message to contain '$messageFragment' but was '${error.message}'",
            error.message!!.contains(messageFragment, ignoreCase = true),
        )
    }
}
