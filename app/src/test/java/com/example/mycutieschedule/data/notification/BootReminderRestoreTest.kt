package com.example.mycutieschedule.data.notification

import com.example.mycutieschedule.core.extensions.toEpochMilli
import com.example.mycutieschedule.data.fake.FakeEventDao
import com.example.mycutieschedule.data.local.entity.EventEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

/**
 * Regression tests for the boot-time reminder restore query.
 *
 * The column `start_datetime` is NOT a true UTC epoch: [toEpochMilli] takes the
 * user's wall-clock [LocalDateTime] and reinterprets it as UTC, so the stored
 * value is `trueEpoch + zoneOffset`. Comparing it against
 * `System.currentTimeMillis()` therefore shifts the filter by exactly the
 * device's UTC offset — which silently drops near-term reminders west of UTC.
 *
 * Fixed offsets (no DST) are used so the expected results are unambiguous.
 */
class BootReminderRestoreTest {

    private val originalTimeZone: TimeZone = TimeZone.getDefault()

    private lateinit var dao: FakeEventDao

    @Before
    fun setUp() {
        dao = FakeEventDao()
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    // ── The reported bug ──────────────────────────────────────────────────────

    @Test
    fun `west of UTC a reminder due in one hour is still restored`() = runTest {
        setZone("-05:00")
        dao.seed(eventStartingIn(hours = 1, id = "in-one-hour"))

        val restored = findPendingReminderEvents(dao)

        assertEquals(listOf("in-one-hour"), restored.map { it.id })
    }

    @Test
    fun `west of UTC a reminder due in four hours is still restored`() = runTest {
        setZone("-05:00")
        dao.seed(eventStartingIn(hours = 4, id = "in-four-hours"))

        val restored = findPendingReminderEvents(dao)

        assertEquals(listOf("in-four-hours"), restored.map { it.id })
    }

    // ── The mirror-image symptom east of UTC ──────────────────────────────────

    @Test
    fun `east of UTC an event that already started is not restored`() = runTest {
        setZone("+03:00")
        dao.seed(eventStartingIn(hours = -1, id = "already-started"))

        val restored = findPendingReminderEvents(dao)

        assertEquals(emptyList<String>(), restored.map { it.id })
    }

    @Test
    fun `east of UTC a reminder due in one hour is still restored`() = runTest {
        setZone("+03:00")
        dao.seed(eventStartingIn(hours = 1, id = "in-one-hour"))

        val restored = findPendingReminderEvents(dao)

        assertEquals(listOf("in-one-hour"), restored.map { it.id })
    }

    // ── UTC — where both time bases agree ─────────────────────────────────────

    @Test
    fun `at UTC future events are restored and past events are not`() = runTest {
        setZone("Z")
        dao.seed(
            eventStartingIn(hours = -1, id = "past"),
            eventStartingIn(hours = 1, id = "future"),
        )

        val restored = findPendingReminderEvents(dao)

        assertEquals(listOf("future"), restored.map { it.id })
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun setZone(offsetId: String) {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(offsetId)))
    }

    /**
     * Builds an event whose wall-clock start is [hours] from now, encoded into
     * `start_datetime` exactly the way `EventMapper.toEntity()` encodes it.
     */
    private fun eventStartingIn(hours: Long, id: String): EventEntity {
        val start = LocalDateTime.now().plusHours(hours)
        return EventEntity(
            id                      = id,
            title                   = id,
            description             = "",
            startDatetime           = start.toEpochMilli(),
            endDatetime             = start.plusHours(1).toEpochMilli(),
            color                   = "PEONY",
            reminderMinutesBefore   = 15,
            reminderType            = "NOTIFICATION",
            recurrenceType          = null,
            recurrenceRepeatDays    = null,
            recurrenceEndDate       = null,
            recurrenceCount         = null,
            recurrenceIntervalWeeks = null,
            recurrenceCustomDates   = null,
            recurrenceGroupId       = null,
            isRecurrenceGenerated   = false,
            ownerId                 = "user-1",
            isImported              = false,
            sourceScheduleId        = null,
            createdAt               = LocalDateTime.now().toEpochMilli(),
            updatedAt               = LocalDateTime.now().toEpochMilli(),
        )
    }
}
