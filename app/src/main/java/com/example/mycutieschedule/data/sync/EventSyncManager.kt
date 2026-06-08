package com.example.mycutieschedule.data.sync

import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.data.local.dao.EventDao
import com.example.mycutieschedule.data.local.entity.EventEntity
import com.example.mycutieschedule.data.mapper.toDomain
import com.example.mycutieschedule.data.mapper.toEntity
import com.example.mycutieschedule.data.notification.EventReminderScheduler
import com.example.mycutieschedule.data.remote.api.FirestoreEventDataSource
import com.example.mycutieschedule.di.ApplicationScope
import com.example.mycutieschedule.domain.model.AuthState
import com.example.mycutieschedule.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Поддерживает Room в синхронизированном состоянии с Firestore путём подписки
 * на event-коллекцию аутентифицированного пользователя.
 *
 * Каждый snapshot из Firestore применяется к локальной БД с разрешением
 * конфликтов по принципу last-write-wins (`updatedAt`-сравнение).
 *
 * **Жизненный цикл:**
 * [start] вызывается один раз из
 * [com.example.mycutieschedule.MyCutieScheduleApp.onCreate]. Запускается
 * долгоживущая корутина в [appScope], которая:
 *   1. Подписывается на [AuthRepository.observeAuthState] и
 *      дедуплицирует по user-ID через `distinctUntilChanged`, чтобы избежать
 *      повторных перезапусков на одном и том же пользователе.
 *   2. При [AuthState.Authenticated] → выполняет bootstrap (одноразовый
 *      `fetchOnce`), затем регистрирует Firestore snapshot listener.
 *   3. При [AuthState.Unauthenticated] → отменяет listener.
 *
 * **Зачем И one-shot fetch, И snapshot listener?**
 * Snapshot listener сначала выдаёт данные из кэша, затем ждёт сервер.
 * На свежем входе или после переустановки кэш пуст, и нет сигнала
 * «ответ от сервера получен» до тех пор, пока события не появятся.
 * Явный [FirestoreEventDataSource.fetchOnce] делает видимыми:
 *   - Ошибки прав (Security Rules) — приходят как [Result.Error]
 *     вместо тихого отсутствия данных.
 *   - Сетевые ошибки — аналогично.
 *   - Пустой Firestore (легитимный «ноль событий») — отличимо от
 *     «мы ещё не дозвонились до сервера».
 * Кроме того, fetchOnce разогревает кэш, чтобы последующие UI-чтения
 * не ждали загрузки.
 *
 * **Разрешение конфликтов — last-write-wins:**
 * Каждое событие из снапшота сравнивается с локальной копией по `updatedAt`.
 * - Удалённое новее → перезаписываем Room (правка с другого устройства).
 * - Локальное новее → пропускаем (наш недавний save ещё не доехал до облака).
 * - Локальной строки нет → INSERT (новое событие с другого устройства).
 *
 * **Распространение удалений:**
 * События, которые есть локально, но отсутствуют в снапшоте, удаляются —
 * но с **60-секундным safety-window**: записи, изменённые локально в
 * последнюю минуту, никогда не удаляются sync'ом. Это защищает только что
 * сохранённую запись от стирания устаревшим или отклонённым сервером
 * снапшотом (например, при PERMISSION_DENIED).
 *
 * **Напоминания:**
 * После insertAll() мы заново планируем алармы для всех upserted-событий
 * через [reminderScheduler]. Это обеспечивает доставку напоминаний на
 * **других** устройствах, куда события прилетают только через sync.
 *
 * **Scope:**
 * Использует [@ApplicationScope] — [CoroutineScope], живущий столько же,
 * сколько процесс. [kotlinx.coroutines.SupervisorJob] гарантирует, что
 * падение одного sync-job'а не убивает watcher целиком.
 */
@Singleton
class EventSyncManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val eventDataSource: FirestoreEventDataSource,
    private val eventDao: EventDao,
    private val reminderScheduler: EventReminderScheduler,
    @ApplicationScope private val appScope: CoroutineScope,
) {

    /** Cancellable job for the active Firestore listener. */
    private var syncJob: Job? = null

    /** The user ID currently being synced — null when no sync is active. */
    private var activeUserId: String? = null

    /**
     * Starts the auth-state watcher. Safe to call multiple times — the internal
     * collector is idempotent, and downstream filtering by user ID prevents
     * thrashing on identical re-emissions.
     */
    fun start() {
        appScope.launch {
            authRepository.observeAuthState()
                // Map to the user ID (or null) so we restart sync only when the
                // signed-in user actually changes — not on every listener re-fire.
                .map { state ->
                    when (state) {
                        is AuthState.Authenticated   -> state.user.id
                        is AuthState.Unauthenticated -> null
                    }
                }
                .distinctUntilChanged()
                .collect { userId ->
                    if (userId != null) startSync(userId) else stopSync()
                }
        }
    }

    /**
     * Forces an explicit one-shot fetch + snapshot restart for the current user.
     * Safe to call from UI as a "pull to refresh" or recovery action.
     * No-op when not signed in.
     */
    fun forceResync() {
        val userId = activeUserId ?: return
        Timber.d("EventSyncManager: forceResync requested for userId=$userId")
        startSync(userId)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun startSync(userId: String) {
        syncJob?.cancel()
        activeUserId = userId
        syncJob = appScope.launch {
            Timber.d("EventSyncManager: starting sync for userId=$userId")

            // ── Step 1: explicit server fetch ─────────────────────────────────
            // Surfaces permission / connectivity errors that would otherwise be
            // invisible behind the snapshot listener's silent failure mode.
            when (val initial = eventDataSource.fetchOnce(userId)) {
                is Result.Success -> {
                    Timber.d(
                        "EventSyncManager: bootstrap fetch OK — ${initial.data.size} events " +
                            "from Firestore for userId=$userId"
                    )
                    runCatching {
                        applySnapshot(
                            userId          = userId,
                            remoteEntities  = initial.data.map { it.toDomain().toEntity() },
                            allowDeletions  = true,
                        )
                    }.onFailure { Timber.e(it, "EventSyncManager: bootstrap apply failed") }
                }
                is Result.Error -> {
                    // CRITICAL diagnostic — if this triggers in production, sync is
                    // broken and the snapshot listener almost certainly will be too.
                    Timber.e(
                        initial.exception,
                        "EventSyncManager: bootstrap fetch FAILED for userId=$userId. " +
                            "Sync will rely on the snapshot listener; check Firestore " +
                            "Security Rules and network connectivity."
                    )
                }
            }

            // ── Step 2: continuous snapshot listener ──────────────────────────
            // This is the source of truth for ongoing live sync. It also retries
            // automatically on network restoration.
            eventDataSource.observeEvents(userId).collect { remoteDtos ->
                try {
                    applySnapshot(
                        userId          = userId,
                        remoteEntities  = remoteDtos.map { it.toDomain().toEntity() },
                        allowDeletions  = true,
                    )
                } catch (e: Exception) {
                    Timber.e(e, "EventSyncManager: failed to apply snapshot")
                }
            }
        }
    }

    private fun stopSync() {
        Timber.d("EventSyncManager: stopping sync (user signed out)")
        syncJob?.cancel()
        syncJob = null
        activeUserId = null
    }

    /**
     * Reconciles a Firestore snapshot with the local Room database for [userId].
     *
     * - **Upsert**: every remote event is inserted/updated locally if it is newer
     *   than (or absent from) the local copy. Idempotent via REPLACE strategy.
     * - **Delete**: every locally-owned non-imported event that is missing from
     *   the snapshot is removed — UNLESS it was updated locally within the last
     *   [DELETE_SAFETY_WINDOW_MS] ms, in which case it is preserved (it may be a
     *   just-saved local write whose Firestore write is still in flight).
     *
     * All Room calls run on [Dispatchers.IO].
     */
    private suspend fun applySnapshot(
        userId: String,
        remoteEntities: List<EventEntity>,
        allowDeletions: Boolean,
    ) = withContext(Dispatchers.IO) {

        // Single round-trip: pull every user-owned non-imported event from Room.
        val localEntities = eventDao.findAllOwnedByUser(userId)
        val localMap      = localEntities.associateBy { it.id }
        val remoteMap     = remoteEntities.associateBy { it.id }

        // ── Upserts ──────────────────────────────────────────────────────────
        val toUpsert = remoteEntities.filter { remote ->
            val local = localMap[remote.id]
            local == null || remote.updatedAt >= local.updatedAt
        }
        if (toUpsert.isNotEmpty()) {
            eventDao.insertAll(toUpsert)
            // (Re-)schedule reminder alarms for every upserted event. This is
            // how reminders propagate to *other* devices the user signed into:
            // their EventSyncManager applies the snapshot, sees the reminder
            // fields, and arms the local AlarmManager. Idempotent — calling
            // schedule() with the same eventId replaces any prior alarm.
            toUpsert.forEach { reminderScheduler.schedule(it.toDomain()) }
            Timber.d(
                "EventSyncManager: upserted ${toUpsert.size} event(s) for userId=$userId"
            )
        }

        // ── Deletes ──────────────────────────────────────────────────────────
        // Safety window: never delete an event that was touched locally very
        // recently — it might be a fresh write whose Firestore replica hasn't
        // arrived yet (or a permission-denied write that should stay visible
        // to the user until they can react).
        if (allowDeletions) {
            val nowMillis = System.currentTimeMillis()
            val toDelete = localEntities
                .filter { local ->
                    local.id !in remoteMap &&
                        (nowMillis - local.updatedAt) > DELETE_SAFETY_WINDOW_MS
                }
                .map { it.id }

            if (toDelete.isNotEmpty()) {
                eventDao.deleteByIds(toDelete)
                // Also kill any scheduled alarms for events the owner deleted
                // on another device — otherwise we'd ring for a phantom event.
                toDelete.forEach { reminderScheduler.cancel(it) }
                Timber.d(
                    "EventSyncManager: deleted ${toDelete.size} event(s) " +
                        "(missing from Firestore) for userId=$userId"
                )
            }
        }
    }

    private companion object {
        /**
         * Window during which a freshly-updated local event is protected from
         * sync-driven deletion. One minute is generous enough to cover slow
         * mobile networks while still catching genuine remote deletes within
         * a reasonable UX timeframe.
         */
        const val DELETE_SAFETY_WINDOW_MS = 60_000L
    }
}
