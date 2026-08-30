package com.example.mycutieschedule.data.sync

import com.example.mycutieschedule.core.extensions.toEpochMilli
import com.example.mycutieschedule.data.fake.FakeEventDao
import com.example.mycutieschedule.data.fake.FakeFirestoreEventDataSource
import com.example.mycutieschedule.data.local.entity.EventEntity
import com.example.mycutieschedule.data.notification.EventReminderScheduler
import com.example.mycutieschedule.domain.fake.FakeAuthRepository
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.util.TimeZone

/**
 * Регрессионные тесты для 60-секундного delete-safety-window в
 * [EventSyncManager] (ветка удалений в `applySnapshot`).
 *
 * Инвариант, который они закрепляют: окно измеряется **в той же временной
 * базе**, в которой хранится `EventEntity.updatedAt`.
 *
 * `updatedAt` пишется как `LocalDateTime.now().toEpochMilli()` — настенное
 * время системной зоны, интерпретированное как UTC. Это НЕ настоящий
 * UTC-epoch: значение смещено на offset зоны. Сравнение такого значения с
 * `System.currentTimeMillis()` даёт разницу, равную реальному возрасту записи
 * **минус offset зоны**, поэтому окно ломается везде, кроме UTC+0.
 *
 * Тесты гоняют менеджер в трёх зонах: восточнее UTC, западнее UTC и на UTC.
 */
class EventSyncManagerDeleteWindowTest {

    private val originalTimeZone: TimeZone = TimeZone.getDefault()
    private var appScope: CoroutineScope? = null

    @After
    fun tearDown() {
        appScope?.cancel()
        TimeZone.setDefault(originalTimeZone)
    }

    // ── Восточнее UTC: удаления обязаны доезжать ─────────────────────────────

    @Test
    fun `east of UTC - stale local event missing from snapshot is deleted`() =
        withTimeZone("Europe/Moscow") {                       // UTC+3, без DST
            val stale = eventEntity(
                id = "stale-event",
                updatedAt = LocalDateTime.now().minusHours(2).toEpochMilli(),
            )
            val dao = runSync(local = listOf(stale))

            assertEquals(
                "Событие, изменённое 2 часа назад и отсутствующее в снапшоте, " +
                    "должно быть удалено — safety window давно истёк",
                listOf("stale-event"),
                dao.deletedIds,
            )
        }

    @Test
    fun `east of UTC - freshly saved local event missing from snapshot is preserved`() =
        withTimeZone("Europe/Moscow") {
            val fresh = eventEntity(
                id = "fresh-event",
                updatedAt = LocalDateTime.now().toEpochMilli(),
            )
            val dao = runSync(local = listOf(fresh))

            assertTrue(
                "Только что сохранённое событие защищено safety window, " +
                    "но было удалено: ${dao.deletedIds}",
                dao.deletedIds.isEmpty(),
            )
        }

    // ── Западнее UTC: окно обязано защищать ──────────────────────────────────

    @Test
    fun `west of UTC - freshly saved local event missing from snapshot is preserved`() =
        withTimeZone("America/Chicago") {                     // UTC-5 / UTC-6
            val fresh = eventEntity(
                id = "fresh-event",
                updatedAt = LocalDateTime.now().toEpochMilli(),
            )
            val dao = runSync(local = listOf(fresh))

            assertTrue(
                "Только что сохранённое событие защищено safety window, " +
                    "но было удалено: ${dao.deletedIds}",
                dao.deletedIds.isEmpty(),
            )
        }

    @Test
    fun `west of UTC - stale local event missing from snapshot is deleted`() =
        withTimeZone("America/Chicago") {
            val stale = eventEntity(
                id = "stale-event",
                updatedAt = LocalDateTime.now().minusHours(2).toEpochMilli(),
            )
            val dao = runSync(local = listOf(stale))

            assertEquals(listOf("stale-event"), dao.deletedIds)
        }

    // ── UTC+0: контрольная группа ────────────────────────────────────────────

    @Test
    fun `at UTC - stale event is deleted and fresh event is preserved`() =
        withTimeZone("UTC") {
            val stale = eventEntity(
                id = "stale-event",
                updatedAt = LocalDateTime.now().minusHours(2).toEpochMilli(),
            )
            val fresh = eventEntity(
                id = "fresh-event",
                updatedAt = LocalDateTime.now().toEpochMilli(),
            )
            val dao = runSync(local = listOf(stale, fresh))

            assertEquals(listOf("stale-event"), dao.deletedIds)
        }

    // ── Harness ──────────────────────────────────────────────────────────────

    /**
     * Поднимает [EventSyncManager] с пустым «облаком», ждёт применения
     * снапшота и возвращает DAO для проверок.
     *
     * Пустой remote означает: каждое локальное событие — кандидат на удаление,
     * и единственное, что его спасает, — safety window.
     */
    private fun runSync(local: List<EventEntity>): FakeEventDao = runBlocking {
        val dao = FakeEventDao(local)
        val dataSource = FakeFirestoreEventDataSource(initial = emptyList())
        val auth = FakeAuthRepository().apply { signIn(USER_EMAIL, "password") }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO).also { appScope = it }

        EventSyncManager(
            authRepository    = auth,
            eventDataSource   = dataSource,
            eventDao          = dao,
            reminderScheduler = mockk<EventReminderScheduler>(relaxed = true),
            appScope          = scope,
        ).start()

        // applySnapshot выполняется на Dispatchers.IO, вне контроля теста.
        // Ждём оба применения (bootstrap fetchOnce + snapshot listener),
        // затем даём короткую паузу на завершение ветки удаления.
        awaitUntil { dao.snapshotReads >= 2 }
        delay(SETTLE_MS)
        dao
    }

    private suspend fun awaitUntil(condition: () -> Boolean) {
        val deadlineNanos = System.nanoTime() + AWAIT_TIMEOUT_MS * 1_000_000
        while (System.nanoTime() < deadlineNanos) {
            if (condition()) return
            delay(10)
        }
        throw AssertionError("Снапшот не был применён за $AWAIT_TIMEOUT_MS мс")
    }

    private fun withTimeZone(zoneId: String, body: () -> Unit) {
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
        body()
    }

    private fun eventEntity(id: String, updatedAt: Long): EventEntity {
        val start = LocalDateTime.now().plusDays(1)
        return EventEntity(
            id                      = id,
            title                   = "Событие $id",
            description             = "",
            startDatetime           = start.toEpochMilli(),
            endDatetime             = start.plusHours(1).toEpochMilli(),
            color                   = "PEONY",
            reminderMinutesBefore   = null,
            reminderType            = null,
            recurrenceType          = null,
            recurrenceRepeatDays    = null,
            recurrenceEndDate       = null,
            recurrenceCount         = null,
            recurrenceIntervalWeeks = null,
            recurrenceCustomDates   = null,
            recurrenceGroupId       = null,
            isRecurrenceGenerated   = false,
            ownerId                 = USER_ID,
            isImported              = false,
            sourceScheduleId        = null,
            createdAt               = updatedAt,
            updatedAt               = updatedAt,
        )
    }

    private companion object {
        /** [FakeAuthRepository.signIn] всегда выдаёт этот UID. */
        const val USER_ID = "uid_test"
        const val USER_EMAIL = "test@example.com"
        const val AWAIT_TIMEOUT_MS = 5_000L
        const val SETTLE_MS = 300L
    }
}
