package com.example.mycutieschedule.presentation.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.repository.AuthRepository
import com.example.mycutieschedule.domain.repository.ShareRepository
import com.example.mycutieschedule.domain.usecase.event.GetEventsForMonthUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject

// Максимум суммарной длительности событий за день для расчёта интенсивности.
// 520 минут ≈ 8 ч 40 мин — обычный рабочий день. Любая нагрузка ≥ этого
// значения отображается в Month View как 100% насыщенная ячейка.
private const val INTENSITY_MAX_MINUTES = 520f

// ── UI state ──────────────────────────────────────────────────────────────────

/**
 * State для экрана месячного календаря.
 *
 * @property currentYearMonth выбранный пользователем месяц (управляется свайпом ViewPager'а)
 * @property eventsByDay      события, сгруппированные по дате — для рендера маркеров/чипов
 * @property dayIntensity     карта «дата → интенсивность 0..1» для теплокарты нагрузки.
 *                            Подмножество [eventsByDay] (после фильтра видимости/учёта в нагрузке).
 * @property isLoading        true пока первый Flow-эмит ещё не пришёл
 */
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
    private val shareRepository: ShareRepository,
) : ViewModel() {

    private val _yearMonth = MutableStateFlow(YearMonth.now())

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<MonthCalendarUiState> = _yearMonth
        .flatMapLatest { ym ->
            val userId = authRepository.getCurrentUser()?.id
                ?: return@flatMapLatest flowOf(
                    MonthCalendarUiState(currentYearMonth = ym, isLoading = false),
                )
            combine(
                getEventsForMonthUseCase(ym, userId),
                shareRepository.observeImportedSchedules(userId),
            ) { events, sharedSchedules ->
                // ── Расчёт фильтров по импортированным расписаниям ───────────
                //
                // Логика двухступенчатая:
                //   1. Видимость (isVisibleInCalendar = false) → событие НЕ
                //      отображается на календаре И НЕ участвует в расчёте
                //      нагрузки. Скрытые расписания «как будто не существуют»
                //      с точки зрения UI.
                //   2. Учёт в нагрузке (includeInWorkload) — только для тех,
                //      что прошли первый фильтр. Если false: событие
                //      отображается, но интенсивность дня считается так,
                //      будто его нет. Полезно для «факультативных» событий
                //      типа дней рождений друзей — видеть хочется, а считать
                //      как загрузку — нет.
                //
                // Личные события (isImported=false) фильтрами не затрагиваются
                // никогда — оба условия проверяют `e.isImported && ...`.
                val hiddenSourceIds = sharedSchedules
                    .filterNot { it.isVisibleInCalendar }
                    .map { it.id }
                    .toSet()
                val notCountedSourceIds = sharedSchedules
                    .filter { it.isVisibleInCalendar && !it.includeInWorkload }
                    .map { it.id }
                    .toSet()

                // Events that render on the calendar.
                val visibleEvents = events.filterNot { e ->
                    e.isImported && e.sourceScheduleId in hiddenSourceIds
                }
                // Events that contribute to workload intensity — strict subset
                // of [visibleEvents] (visible + counted).
                val workloadEvents = visibleEvents.filterNot { e ->
                    e.isImported && e.sourceScheduleId in notCountedSourceIds
                }

                val byDay         = visibleEvents.groupBy { it.startDateTime.toLocalDate() }
                val byDayWorkload = workloadEvents.groupBy { it.startDateTime.toLocalDate() }

                val intensity = byDayWorkload.mapValues { (_, dayEvents) ->
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
