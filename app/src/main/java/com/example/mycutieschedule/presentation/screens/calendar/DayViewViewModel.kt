package com.example.mycutieschedule.presentation.screens.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.DayNote
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.repository.AuthRepository
import com.example.mycutieschedule.domain.repository.DayNoteRepository
import com.example.mycutieschedule.domain.repository.ShareRepository
import com.example.mycutieschedule.domain.usecase.event.GetEventsForDayUseCase
import com.example.mycutieschedule.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

private const val DAY_DEFAULT_START_HOUR = 7
private const val DAY_DEFAULT_END_HOUR   = 21

// ── Модель раскладки ──────────────────────────────────────────────────────────

/**
 * Заранее рассчитанная позиция события в колонке дня для решения проблемы
 * перекрывающихся событий (например, два звонка одновременно).
 *
 * Один и тот же day-column может быть разбит на несколько параллельных
 * под-колонок, если в момент времени T идут несколько событий. Каждое
 * событие получает свой [columnIndex] (0-based) и общий [columnCount]
 * (сколько всего параллельных колонок в группе пересечений).
 *
 * UI рисует событие по горизонтали как:
 *   left  = parentWidth × columnIndex / columnCount
 *   width = parentWidth / columnCount
 *
 * Намеренно дублирует структуру [WeekCalendarViewModel.PositionedEvent],
 * чтобы каждый экран был самодостаточным и независимым.
 *
 * @param columnIndex 0-based индекс колонки внутри группы пересечений.
 * @param columnCount Общее число параллельных колонок в группе.
 */
data class DayPositionedEvent(
    val event: Event,
    val columnIndex: Int,
    val columnCount: Int,
)

// ── One-shot events ───────────────────────────────────────────────────────────

sealed class DayViewEvent {
    data class ShowError(val message: String) : DayViewEvent()
}

// ── UI state ──────────────────────────────────────────────────────────────────

data class DayViewUiState(
    val date: LocalDate,
    val positionedEvents: List<DayPositionedEvent> = emptyList(),
    /**
     * `sourceScheduleId` (= share code) → display name (alias or owner name).
     * Used to label imported events on the day timeline.
     */
    val sourceLabels: Map<String, String> = emptyMap(),
    val isLoadingEvents: Boolean  = true,
    /** First hour (inclusive) shown in the time grid. Auto-extends for early events. */
    val visibleHourStart: Int     = DAY_DEFAULT_START_HOUR,
    /** Last hour (exclusive) shown in the time grid. Auto-extends for late events. */
    val visibleHourEnd: Int       = DAY_DEFAULT_END_HOUR,
    // ── Notes ────────────────────────────────────────────────────────────────
    val note: DayNote?            = null,
    /** Live text-field content — decoupled from [note] to allow unsaved editing. */
    val noteDraft: String         = "",
    val isNoteModified: Boolean   = false,
    val isSavingNote: Boolean     = false,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class DayViewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEventsForDayUseCase: GetEventsForDayUseCase,
    private val dayNoteRepository: DayNoteRepository,
    private val authRepository: AuthRepository,
    private val shareRepository: ShareRepository,
) : ViewModel() {

    val date: LocalDate = LocalDate.ofEpochDay(
        savedStateHandle.toRoute<Screen.DayView>().dateEpochDay,
    )

    private val _uiState = MutableStateFlow(DayViewUiState(date = date))
    val uiState: StateFlow<DayViewUiState> = _uiState.asStateFlow()

    private val _events = Channel<DayViewEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        val userId = authRepository.getCurrentUser()?.id
        if (userId != null) {
            observeEventsAndSchedules(userId)
            observeNote(userId)
        } else {
            _uiState.update { it.copy(isLoadingEvents = false) }
        }
    }

    // ── Data observers ────────────────────────────────────────────────────────

    /**
     * Single combined observer for events + imported-schedule metadata.
     *
     * Imported schedules contribute two things:
     *   1. `sourceLabels` — labels for event cards belonging to imported events.
     *   2. `hiddenSourceIds` — set of share-codes flagged
     *      [SharedSchedule.isVisibleInCalendar] = false. Events whose
     *      `sourceScheduleId` is in this set are filtered out BEFORE overlap
     *      layout and visible-hour computation — so hidden schedules genuinely
     *      vanish from the timeline without affecting workload geometry.
     */
    private fun observeEventsAndSchedules(userId: String) {
        viewModelScope.launch {
            combine(
                getEventsForDayUseCase(date, userId),
                shareRepository.observeImportedSchedules(userId),
            ) { events, sharedSchedules ->
                val hiddenSourceIds = sharedSchedules
                    .filterNot { it.isVisibleInCalendar }
                    .map { it.id }
                    .toSet()
                val visibleEvents = events.filterNot { e ->
                    e.isImported && e.sourceScheduleId in hiddenSourceIds
                }

                val sorted     = visibleEvents
                    .sortedWith(compareBy({ it.startDateTime }, { it.endDateTime }))
                val positioned = computeOverlapLayout(sorted)
                val (startH, endH) = computeVisibleHourRange(sorted)

                Triple(
                    positioned,
                    startH to endH,
                    sharedSchedules.associate { it.id to it.displayName },
                )
            }.collect { (positioned, hourRange, labels) ->
                _uiState.update {
                    it.copy(
                        positionedEvents = positioned,
                        sourceLabels     = labels,
                        isLoadingEvents  = false,
                        visibleHourStart = hourRange.first,
                        visibleHourEnd   = hourRange.second,
                    )
                }
            }
        }
    }

    private fun observeNote(userId: String) {
        viewModelScope.launch {
            dayNoteRepository.observeByDate(date, userId).collect { note ->
                _uiState.update { state ->
                    // Only sync text from DB when the user has no unsaved changes.
                    // This prevents the DB emission from wiping in-progress edits.
                    val newDraft = if (!state.isNoteModified) note?.content ?: "" else state.noteDraft
                    state.copy(note = note, noteDraft = newDraft)
                }
            }
        }
    }

    // ── Note interactions ─────────────────────────────────────────────────────

    fun onNoteDraftChange(text: String) {
        _uiState.update { state ->
            state.copy(
                noteDraft      = text,
                isNoteModified = text != (state.note?.content ?: ""),
            )
        }
    }

    fun saveNote() {
        val state  = _uiState.value
        val userId = authRepository.getCurrentUser()?.id ?: run {
            viewModelScope.launch {
                _events.send(DayViewEvent.ShowError("Вы не авторизованы"))
            }
            return
        }

        _uiState.update { it.copy(isSavingNote = true) }

        viewModelScope.launch {
            val result = if (state.noteDraft.isBlank()) {
                dayNoteRepository.deleteNote(date, userId)
            } else {
                val note = state.note?.copy(content = state.noteDraft)
                    ?: DayNote(date = date, content = state.noteDraft, ownerId = userId)
                dayNoteRepository.saveNote(note)
            }

            // Clear the "modified" flag so the next DB emission syncs the draft.
            _uiState.update { it.copy(isSavingNote = false, isNoteModified = false) }

            if (result is Result.Error) {
                _events.send(
                    DayViewEvent.ShowError(result.message ?: "Не удалось сохранить заметку")
                )
            }
        }
    }

    // ── Private layout helpers ────────────────────────────────────────────────

    /**
     * Greedy column-assignment for overlapping events.
     * Each event is placed in the first column whose previous occupant has ended
     * at or before this event's start. New columns are opened as needed.
     * O(n²) — acceptable for a single day's event count.
     */
    private fun computeOverlapLayout(events: List<Event>): List<DayPositionedEvent> {
        if (events.isEmpty()) return emptyList()

        val columnEnds  = mutableListOf<LocalDateTime>()
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
            DayPositionedEvent(
                event       = event,
                columnIndex = assignments[i],
                columnCount = totalCols,
            )
        }
    }

    /**
     * Computes visible hour range from the event list.
     * Defaults to [DAY_DEFAULT_START_HOUR]..[DAY_DEFAULT_END_HOUR] (07–21).
     * Extends automatically when events start earlier or end later.
     */
    private fun computeVisibleHourRange(events: List<Event>): Pair<Int, Int> {
        var start = DAY_DEFAULT_START_HOUR
        var end   = DAY_DEFAULT_END_HOUR
        for (event in events) {
            if (event.startDateTime.hour < start) start = event.startDateTime.hour
            val eventEnd = event.endDateTime.hour + if (event.endDateTime.minute > 0) 1 else 0
            if (eventEnd > end) end = eventEnd
        }
        return start.coerceAtLeast(0) to end.coerceAtMost(24)
    }
}
