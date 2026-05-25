package com.example.mycutieschedule.presentation.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.repository.AuthRepository
import com.example.mycutieschedule.domain.usecase.event.GetEventsForMonthUseCase
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
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

// full intensity (≥ 520 min clamps to 1.0)
private const val INTENSITY_MAX_MINUTES = 520f

// ── UI state ──────────────────────────────────────────────────────────────────

data class MonthCalendarUiState(
    val currentYearMonth: YearMonth            = YearMonth.now(),
    val eventsByDay:      Map<LocalDate, List<Event>> = emptyMap(),
    val dayIntensity:     Map<LocalDate, Float>       = emptyMap(),
    val isLoading:        Boolean              = true,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class MonthCalendarViewModel @Inject constructor(
    private val getEventsForMonthUseCase: GetEventsForMonthUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MonthCalendarUiState> = _yearMonth
        .flatMapLatest { ym ->
            val userId = authRepository.getCurrentUser()?.id
                ?: return@flatMapLatest flowOf(
                    MonthCalendarUiState(currentYearMonth = ym, isLoading = false),
                )
            getEventsForMonthUseCase(ym, userId)
                .map { events ->
                    val byDay     = events.groupBy { it.startDateTime.toLocalDate() }
                    val intensity = byDay.mapValues { (_, dayEvents) ->
                        val totalMinutes = dayEvents.sumOf {
                            ChronoUnit.MINUTES.between(it.startDateTime, it.endDateTime)
                                .coerceAtLeast(0L)
                        }
                        (totalMinutes.toFloat() / INTENSITY_MAX_MINUTES).coerceIn(0f, 1f)
                    }
                    MonthCalendarUiState(
                        currentYearMonth = ym,
                        eventsByDay      = byDay,
                        dayIntensity     = intensity,
                        isLoading        = false,
                    )
                }
        }
        .stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = MonthCalendarUiState(),
        )

    // ── Navigation ────────────────────────────────────────────────────────────

    fun goToPrevMonth() { _yearMonth.value = _yearMonth.value.minusMonths(1) }
    fun goToNextMonth() { _yearMonth.value = _yearMonth.value.plusMonths(1) }
    fun goToToday()     { _yearMonth.value = YearMonth.now() }

    fun onMonthSwiped(yearMonth: YearMonth) {
        if (_yearMonth.value != yearMonth) _yearMonth.value = yearMonth
    }
}
