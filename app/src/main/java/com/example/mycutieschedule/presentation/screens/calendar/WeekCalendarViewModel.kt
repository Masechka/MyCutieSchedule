package com.example.mycutieschedule.presentation.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycutieschedule.core.extensions.toWeekStart
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.repository.AuthRepository
import com.example.mycutieschedule.domain.repository.ShareRepository
import com.example.mycutieschedule.domain.usecase.event.GetEventsForWeekUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

// ── Visible hour range defaults ────────────────────────────────────────────────
private const val DEFAULT_VISIBLE_START_HOUR = 7
private const val DEFAULT_VISIBLE_END_HOUR   = 22

/**
 * Pre-computed layout position for an event within a single day column.
 *
 * @param columnIndex 0-based column in the overlap group (0 = leftmost).
 * @param columnCount Total parallel columns required to render the overlap group.
 */
data class PositionedEvent(
    val event: Event,
    val columnIndex: Int,
    val columnCount: Int,
)

data class WeekCalendarUiState(
    val currentWeekMonday: LocalDate = LocalDate.now().toWeekStart(),
    /** Events pre-processed with overlap-layout positions, keyed by day (Mon–Sun). */
    val eventsByDay: Map<LocalDate, List<PositionedEvent>> = emptyMap(),
    /**
     * Map of `sourceScheduleId` (= share code) → display name (alias or owner name).
     * Used to label imported events with the schedule they came from.
     */
    val sourceLabels: Map<String, String> = emptyMap(),
    val isLoading: Boolean = true,
    /**
     * First hour (inclusive) shown in the time grid.
     * Defaults to [DEFAULT_VISIBLE_START_HOUR]; shrinks toward 0 when events start earlier.
     */
    val visibleHourStart: Int = DEFAULT_VISIBLE_START_HOUR,
    /**
     * Last hour (exclusive) shown in the time grid.
     * Defaults to [DEFAULT_VISIBLE_END_HOUR]; grows toward 24 when events end later.
     */
    val visibleHourEnd: Int = DEFAULT_VISIBLE_END_HOUR,
)

@HiltViewModel
class WeekCalendarViewModel @Inject constructor(
    private val getEventsForWeekUseCase: GetEventsForWeekUseCase,
    private val authRepository: AuthRepository,
    private val shareRepository: ShareRepository,
) : ViewModel() {

    private val _weekMonday = MutableStateFlow(LocalDate.now().toWeekStart())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<WeekCalendarUiState> = _weekMonday
        .flatMapLatest { monday ->
            val userId = authRepository.getCurrentUser()?.id
                ?: return@flatMapLatest flowOf(
                    WeekCalendarUiState(currentWeekMonday = monday, isLoading = false),
                )
            combine(
                getEventsForWeekUseCase(weekDate = monday, userId = userId),
                shareRepository.observeImportedSchedules(userId),
            ) { events, sharedSchedules ->
                // ── Filter out events from hidden imported schedules ─────────
                // The events list from the use case includes every owner+imported
                // event for the user — we strip imported ones whose source schedule
                // has been toggled hidden. Personal events are never affected.
                val hiddenSourceIds = sharedSchedules
                    .filterNot { it.isVisibleInCalendar }
                    .map { it.id }
                    .toSet()
                val visibleEvents = events.filterNot { e ->
                    e.isImported && e.sourceScheduleId in hiddenSourceIds
                }

                val (startHour, endHour) = computeVisibleHourRange(visibleEvents)
                WeekCalendarUiState(
                    currentWeekMonday = monday,
                    eventsByDay       = buildEventsByDay(monday, visibleEvents),
                    sourceLabels      = sharedSchedules.associate { it.id to it.displayName },
                    isLoading         = false,
                    visibleHourStart  = startHour,
                    visibleHourEnd    = endHour,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = WeekCalendarUiState(),
        )

    // ── Navigation ────────────────────────────────────────────────────────────

    fun goToPrevWeek() { _weekMonday.value = _weekMonday.value.minusWeeks(1) }
    fun goToNextWeek() { _weekMonday.value = _weekMonday.value.plusWeeks(1) }
    fun goToToday()    { _weekMonday.value = LocalDate.now().toWeekStart() }

    /** Called when the user swipes the HorizontalPager to a different week. */
    fun onWeekSwiped(monday: LocalDate) {
        if (_weekMonday.value != monday) _weekMonday.value = monday
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildEventsByDay(
        monday: LocalDate,
        events: List<Event>,
    ): Map<LocalDate, List<PositionedEvent>> =
        (0..6)
            .map { monday.plusDays(it.toLong()) }
            .associateWith { day ->
                val dayEvents = events
                    .filter { it.startDateTime.toLocalDate() == day }
                    .sortedWith(compareBy({ it.startDateTime }, { it.endDateTime }))
                computeOverlapLayout(dayEvents)
            }

    /**
     * Greedy column-assignment algorithm for overlapping events.
     *
     * Each event is placed in the first column whose previous occupant has ended
     * at or before this event's start time. A new column is opened when all
     * existing columns are occupied.
     *
     * Time complexity: O(n²) — acceptable for typical day event counts (< 50).
     */
    private fun computeOverlapLayout(events: List<Event>): List<PositionedEvent> {
        if (events.isEmpty()) return emptyList()

        val columnEnds = mutableListOf<LocalDateTime>()
        val assignments = IntArray(events.size)

        events.forEachIndexed { i, event ->
            var placed = false
            for (col in columnEnds.indices) {
                if (!columnEnds[col].isAfter(event.startDateTime)) {
                    columnEnds[col] = event.endDateTime
                    assignments[i] = col
                    placed = true
                    break
                }
            }
            if (!placed) {
                assignments[i] = columnEnds.size
                columnEnds.add(event.endDateTime)
            }
        }

        val totalCols = columnEnds.size
        return events.mapIndexed { i, event ->
            PositionedEvent(event = event, columnIndex = assignments[i], columnCount = totalCols)
        }
    }

    /**
     * Computes the visible hour range for the week based on the events present.
     *
     * Rules:
     *   - Default window: [DEFAULT_VISIBLE_START_HOUR]..[DEFAULT_VISIBLE_END_HOUR] (07–21)
     *   - Any event starting before 07:00 → extend start back to that event's hour
     *   - Any event ending after 21:00    → extend end forward to the next whole hour
     *   - Result is always clamped to 0..24
     *
     * Examples:
     *   Events 10:00–18:00  →  7..21  (defaults unchanged)
     *   Event  06:30–08:00  →  6..21  (start extended to 6)
     *   Event  20:00–23:30  →  7..24  (end extended to 24 = 23+ceil(0.5h))
     */
    private fun computeVisibleHourRange(events: List<Event>): Pair<Int, Int> {
        var startHour = DEFAULT_VISIBLE_START_HOUR
        var endHour   = DEFAULT_VISIBLE_END_HOUR

        for (event in events) {
            if (event.startDateTime.hour < startHour) {
                startHour = event.startDateTime.hour
            }
            // Ceil the end to the next whole hour when the event has trailing minutes.
            val eventEndHour = event.endDateTime.hour +
                if (event.endDateTime.minute > 0) 1 else 0
            if (eventEndHour > endHour) {
                endHour = eventEndHour
            }
        }

        return startHour.coerceAtLeast(0) to endHour.coerceAtMost(24)
    }
}
