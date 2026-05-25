package com.example.mycutieschedule.presentation.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycutieschedule.core.extensions.toWeekStart
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.repository.AuthRepository
import com.example.mycutieschedule.domain.usecase.event.GetEventsForWeekUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

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
    val isLoading: Boolean = true,
)

@HiltViewModel
class WeekCalendarViewModel @Inject constructor(
    private val getEventsForWeekUseCase: GetEventsForWeekUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _weekMonday = MutableStateFlow(LocalDate.now().toWeekStart())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<WeekCalendarUiState> = _weekMonday
        .flatMapLatest { monday ->
            val userId = authRepository.getCurrentUser()?.id
                ?: return@flatMapLatest flowOf(
                    WeekCalendarUiState(currentWeekMonday = monday, isLoading = false),
                )
            getEventsForWeekUseCase(weekDate = monday, userId = userId)
                .map { events ->
                    WeekCalendarUiState(
                        currentWeekMonday = monday,
                        eventsByDay = buildEventsByDay(monday, events),
                        isLoading = false,
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
}
