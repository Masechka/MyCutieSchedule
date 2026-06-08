package com.example.mycutieschedule.data.sync

import com.example.mycutieschedule.data.local.dao.EventDao
import com.example.mycutieschedule.data.local.dao.SharedScheduleDao
import com.example.mycutieschedule.data.local.entity.SharedScheduleEntity
import com.example.mycutieschedule.data.mapper.toDomain
import com.example.mycutieschedule.data.mapper.toEntity
import com.example.mycutieschedule.data.remote.api.FirestoreEventDataSource
import com.example.mycutieschedule.di.ApplicationScope
import com.example.mycutieschedule.domain.model.AuthState
import com.example.mycutieschedule.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Поддерживает локальные Room-зеркала **импортированных** расписаний в
 * актуальном состоянии относительно Firestore-данных их владельцев.
 *
 * **Архитектура:**
 *   - Одна корутина на каждое импортированное расписание регистрирует
 *     Firestore snapshot listener на `users/{ownerId}/events`. Когда
 *     владелец редактирует, добавляет или удаляет событие — listener
 *     срабатывает, и локальное зеркало приводится в соответствие.
 *   - Набор «импортированных расписаний для наблюдения» сам реактивен —
 *     наблюдается через [SharedScheduleDao.observeAll]. Добавление или
 *     удаление расписания прозрачно запускает / останавливает его listener
 *     через [reconcileWatchers].
 *
 * **Почему отдельный менеджер от [EventSyncManager]?**
 *   [EventSyncManager] синхронизирует **собственные** события viewer'а
 *   (`users/{viewerId}/events`). Этот менеджер синхронизирует
 *   **импортированные** события, которые физически живут в subcollection
 *   ДРУГОГО пользователя. Разные пути Firestore, разные правила доступа
 *   (viewer только читает, никогда не пишет в чужой subcollection) —
 *   объединение в одну корутину запутало бы логику и сделало
 *   отладку прав сложнее.
 *
 * **Жизненный цикл:**
 *   [start] вызывается один раз из
 *   [com.example.mycutieschedule.MyCutieScheduleApp.onCreate]. При sign-out
 *   все per-schedule listener'ы отменяются; при sign-in менеджер
 *   подхватывает список импортированных расписаний нового viewer'а
 *   и поднимает свежие listener'ы.
 */
@Singleton
class SharedSchedulesSyncManager @Inject constructor(
    private val authRepository: AuthRepository,
    private val sharedScheduleDao: SharedScheduleDao,
    private val eventDao: EventDao,
    private val eventDataSource: FirestoreEventDataSource,
    @ApplicationScope private val appScope: CoroutineScope,
) {

    /** Outer job watching auth state. */
    private var authWatcher: Job? = null

    /** Inner job watching the current viewer's imported-schedules list. */
    private var importedListWatcher: Job? = null

    /** One snapshot-listener job per imported schedule, keyed by schedule.id (= share code). */
    private val perScheduleJobs: MutableMap<String, Job> = mutableMapOf()

    fun start() {
        authWatcher?.cancel()
        authWatcher = appScope.launch {
            authRepository.observeAuthState()
                .map { state ->
                    when (state) {
                        is AuthState.Authenticated   -> state.user.id
                        is AuthState.Unauthenticated -> null
                    }
                }
                .distinctUntilChanged()
                .collect { viewerId ->
                    if (viewerId != null) watchImportedList(viewerId) else stopAll()
                }
        }
    }

    // ── Per-viewer: watch which schedules to mirror ──────────────────────────

    private fun watchImportedList(viewerId: String) {
        importedListWatcher?.cancel()
        stopAllPerSchedule()  // clear previous viewer's jobs

        Timber.d("SharedSchedulesSyncManager: starting list watcher for viewerId=$viewerId")
        importedListWatcher = sharedScheduleDao.observeAll(viewerId)
            .onEach { schedules -> reconcileWatchers(viewerId, schedules) }
            .launchIn(appScope)
    }

    /**
     * Brings [perScheduleJobs] in line with [currentSchedules]:
     *   - Start a listener for schedules in the list but not in [perScheduleJobs].
     *   - Stop and remove listeners for schedules that were unimported.
     */
    private fun reconcileWatchers(
        viewerId: String,
        currentSchedules: List<SharedScheduleEntity>,
    ) {
        val currentIds = currentSchedules.map { it.id }.toSet()

        // Stop watchers for schedules the user removed.
        val toStop = perScheduleJobs.keys - currentIds
        toStop.forEach { id ->
            perScheduleJobs.remove(id)?.cancel()
            Timber.d("SharedSchedulesSyncManager: stopped listener for scheduleId=$id")
        }

        // Start watchers for newly-imported schedules.
        currentSchedules.forEach { schedule ->
            if (schedule.id !in perScheduleJobs) {
                perScheduleJobs[schedule.id] = launchScheduleListener(viewerId, schedule)
            }
        }
    }

    // ── Per-schedule: live mirror via Firestore snapshot listener ────────────

    private fun launchScheduleListener(
        viewerId: String,
        schedule: SharedScheduleEntity,
    ): Job = appScope.launch {
        Timber.d(
            "SharedSchedulesSyncManager: starting listener for " +
                "scheduleId=${schedule.id} owner=${schedule.scheduleId}"
        )
        eventDataSource.observeEvents(schedule.scheduleId).collect { dtos ->
            try {
                applyOwnerSnapshot(
                    viewerId       = viewerId,
                    shareCode      = schedule.id,
                    ownerEventDtos = dtos,
                )
            } catch (e: Exception) {
                Timber.e(
                    e,
                    "SharedSchedulesSyncManager: failed to apply snapshot for " +
                        "scheduleId=${schedule.id}"
                )
            }
        }
    }

    /**
     * Reconciles the local imported-events mirror for a single shared schedule
     * with the owner's latest Firestore snapshot.
     *
     * - Upserts every owner event under the viewer's `owner_id` so it surfaces
     *   in the viewer's calendar queries (which filter by owner_id = viewerId).
     *   `isImported = true` and `sourceScheduleId = shareCode` are pinned so
     *   the viewer's own sync (EventSyncManager) NEVER touches these rows.
     * - Deletes events that disappeared from the owner's collection (owner
     *   deleted them) — but only those marked `is_imported = 1` AND scoped to
     *   the same `source_schedule_id`, so the viewer's own events are safe.
     *
     * Idempotent. Same event IDs across resyncs use Room's REPLACE strategy.
     */
    private suspend fun applyOwnerSnapshot(
        viewerId: String,
        shareCode: String,
        ownerEventDtos: List<com.example.mycutieschedule.data.remote.dto.EventDto>,
    ) = withContext(Dispatchers.IO) {

        // ── Upsert ──────────────────────────────────────────────────────────
        val mirrored = ownerEventDtos.map { dto ->
            dto.toDomain()
                .copy(
                    ownerId          = viewerId,   // viewer's id — so calendar queries find it
                    isImported       = true,       // visual + sync exclusion
                    sourceScheduleId = shareCode,  // links back to the shared_schedule_table row
                )
                .toEntity()
        }
        if (mirrored.isNotEmpty()) {
            eventDao.insertAll(mirrored)
            Timber.d(
                "SharedSchedulesSyncManager: upserted ${mirrored.size} mirrored event(s) " +
                    "for scheduleId=$shareCode"
            )
        }

        // ── Delete events the owner removed ─────────────────────────────────
        // Get every currently-mirrored event from THIS share code, see which
        // IDs are missing in the new owner snapshot, delete those.
        // Owner-deletes don't get blanket safety windows like own-event sync —
        // there's no way for the viewer to have "freshly written" an imported
        // event, so propagating deletes immediately is safe.
        val mirroredIds = mirrored.map { it.id }.toSet()
        val toDelete = eventDao
            .findImportedEventsBySource(shareCode)
            .filter { it.id !in mirroredIds }
            .map { it.id }
        if (toDelete.isNotEmpty()) {
            eventDao.deleteByIds(toDelete)
            Timber.d(
                "SharedSchedulesSyncManager: deleted ${toDelete.size} stale mirrored event(s) " +
                    "for scheduleId=$shareCode"
            )
        }
    }

    // ── Teardown ──────────────────────────────────────────────────────────────

    private fun stopAll() {
        Timber.d("SharedSchedulesSyncManager: stopping all listeners (user signed out)")
        importedListWatcher?.cancel()
        importedListWatcher = null
        stopAllPerSchedule()
    }

    private fun stopAllPerSchedule() {
        perScheduleJobs.values.forEach { it.cancel() }
        perScheduleJobs.clear()
    }
}
