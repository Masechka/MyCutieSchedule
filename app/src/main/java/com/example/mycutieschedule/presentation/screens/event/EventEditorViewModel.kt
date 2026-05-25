package com.example.mycutieschedule.presentation.screens.event

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycutieschedule.core.extensions.toLocalDateTime
import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.model.EventColor
import com.example.mycutieschedule.domain.model.Recurrence
import com.example.mycutieschedule.domain.model.RecurrenceType
import com.example.mycutieschedule.domain.model.Reminder
import com.example.mycutieschedule.domain.repository.AuthRepository
import com.example.mycutieschedule.domain.usecase.event.AddEventUseCase
import com.example.mycutieschedule.domain.usecase.event.DeleteEventUseCase
import com.example.mycutieschedule.domain.usecase.event.DeleteRecurringEventUseCase
import com.example.mycutieschedule.domain.usecase.event.GenerateRecurringEventsUseCase
import com.example.mycutieschedule.domain.usecase.event.GetEventByIdUseCase
import com.example.mycutieschedule.domain.usecase.event.UpdateEventUseCase
import com.example.mycutieschedule.presentation.navigation.Screen
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject

// ── One-shot UI events (navigation + snackbar) ────────────────────────────────

sealed class EventEditorEvent {
    /** Emitted when a save completes — Screen should pop the back stack. */
    data object SaveSuccess   : EventEditorEvent()
    /** Emitted when a delete completes — Screen should pop the back stack. */
    data object DeleteSuccess : EventEditorEvent()
    /** Emitted on validation failure or repository error — Screen shows a snackbar. */
    data class  ShowError(val message: String) : EventEditorEvent()
}

// ── UI state ──────────────────────────────────────────────────────────────────

data class EventEditorUiState(
    /** True while the existing event is being loaded from the repository. */
    val isLoading: Boolean            = false,
    /** True while a save / delete call is in progress. */
    val isSaving: Boolean             = false,
    // ── Form fields ──────────────────────────────────────────────────────────
    val title: String                 = "",
    val titleError: String?           = null,
    val description: String           = "",
    val startDateTime: LocalDateTime  = defaultStart(),
    val endDateTime: LocalDateTime    = defaultStart().plusHours(1),
    val dateRangeError: String?       = null,
    val color: EventColor             = EventColor.PEONY,
    val reminder: Reminder?           = null,
    val recurrence: Recurrence?       = null,
    /**
     * True when the event being edited belongs to a recurrence chain
     * (i.e. its [Event.recurrenceGroupId] is non-null).
     * Controls whether the Delete button shows the single-vs-series dialog.
     */
    val isRecurringEvent: Boolean     = false,
)

private fun defaultStart(): LocalDateTime =
    LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class EventEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEventByIdUseCase: GetEventByIdUseCase,
    private val addEventUseCase: AddEventUseCase,
    private val updateEventUseCase: UpdateEventUseCase,
    private val deleteEventUseCase: DeleteEventUseCase,
    private val deleteRecurringEventUseCase: DeleteRecurringEventUseCase,
    private val generateRecurringEventsUseCase: GenerateRecurringEventsUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    // Deserialise navigation args using type-safe Navigation 2.8+.
    private val route: Screen.EventEditor  = savedStateHandle.toRoute()
    private val eventId: String            = route.eventId
    private val prefillMilli: Long?        = route.prefillDateEpochMilli

    /** True when the screen was opened with an existing event ID. */
    val isEditing: Boolean = eventId.isNotBlank()

    /** Stores the full original event so immutable fields are preserved on update. */
    private var originalEvent: Event? = null

    /**
     * Tracks whether the user has explicitly chosen an end time.
     *
     * Starts as `false` for new events so that changes to start time auto-adjust
     * end to start + 1 h. Set to `true` the moment the user picks a custom end time,
     * or when an existing event is loaded (its end time must not be overwritten).
     */
    private var isEndTimeManuallySet: Boolean = false

    private val _uiState = MutableStateFlow(buildInitialState())
    val uiState: StateFlow<EventEditorUiState> = _uiState.asStateFlow()

    private val _events = Channel<EventEditorEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        if (isEditing) loadEvent()
    }

    // ── Initialization helpers ────────────────────────────────────────────────

    private fun buildInitialState(): EventEditorUiState {
        val start = if (prefillMilli != null) {
            prefillMilli.toLocalDateTime().withMinute(0).withSecond(0).withNano(0)
        } else {
            defaultStart()
        }
        return EventEditorUiState(
            isLoading     = isEditing,   // show spinner while loading the event
            startDateTime = start,
            endDateTime   = start.plusHours(1),
        )
    }

    private fun loadEvent() {
        viewModelScope.launch {
            val event = getEventByIdUseCase(eventId)
            if (event != null) {
                originalEvent        = event
                isEndTimeManuallySet = true
                _uiState.update {
                    it.copy(
                        isLoading        = false,
                        title            = event.title,
                        description      = event.description,
                        startDateTime    = event.startDateTime,
                        endDateTime      = event.endDateTime,
                        color            = event.color,
                        reminder         = event.reminder,
                        recurrence       = event.recurrence,
                        isRecurringEvent = event.recurrenceGroupId != null,
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
                _events.send(EventEditorEvent.ShowError("Событие не найдено"))
            }
        }
    }

    // ── Form field updaters ───────────────────────────────────────────────────

    fun onTitleChange(value: String) {
        _uiState.update { it.copy(title = value, titleError = null) }
    }

    fun onDescriptionChange(value: String) {
        _uiState.update { it.copy(description = value) }
    }

    fun onStartDateTimeChange(value: LocalDateTime) {
        val currentEnd       = _uiState.value.endDateTime
        val endIsInvalid     = !value.isBefore(currentEnd)
        val shouldAutoAdjust = !isEndTimeManuallySet || endIsInvalid
        _uiState.update {
            it.copy(
                startDateTime  = value,
                endDateTime    = if (shouldAutoAdjust) value.plusHours(1) else currentEnd,
                dateRangeError = null,
            )
        }
    }

    fun onEndDateTimeChange(value: LocalDateTime) {
        isEndTimeManuallySet = true
        _uiState.update { it.copy(endDateTime = value, dateRangeError = null) }
    }

    fun onColorChange(color: EventColor) {
        _uiState.update { it.copy(color = color) }
    }

    fun onReminderChange(reminder: Reminder?) {
        _uiState.update { it.copy(reminder = reminder) }
    }

    fun onRecurrenceTypeChange(type: RecurrenceType?) {
        val existing        = _uiState.value.recurrence
        val keepDays        = type == RecurrenceType.WEEKLY || type == RecurrenceType.CUSTOM_DAYS
        val keepInterval    = type == RecurrenceType.WEEKLY
        val keepCustomDates = type == RecurrenceType.CUSTOM_DATES
        _uiState.update {
            it.copy(
                recurrence = type?.let { t ->
                    Recurrence(
                        type          = t,
                        intervalWeeks = if (keepInterval) existing?.intervalWeeks ?: 1 else 1,
                        repeatDays    = if (keepDays) existing?.repeatDays ?: emptySet() else emptySet(),
                        customDates   = if (keepCustomDates) existing?.customDates ?: emptyList() else emptyList(),
                        endDate       = existing?.endDate,
                    )
                }
            )
        }
    }

    fun onRepeatDayToggled(day: DayOfWeek) {
        val current = _uiState.value.recurrence
            ?.takeIf { it.type == RecurrenceType.WEEKLY || it.type == RecurrenceType.CUSTOM_DAYS }
            ?: return
        val updated = if (day in current.repeatDays) current.repeatDays - day else current.repeatDays + day
        _uiState.update { it.copy(recurrence = current.copy(repeatDays = updated)) }
    }

    fun onRecurrenceEndDateChange(date: LocalDate?) {
        val current = _uiState.value.recurrence ?: return
        _uiState.update { it.copy(recurrence = current.copy(endDate = date)) }
    }

    /** Changes the "every N weeks" interval for [RecurrenceType.WEEKLY]. */
    fun onRecurrenceIntervalChange(weeks: Int) {
        val current = _uiState.value.recurrence
            ?.takeIf { it.type == RecurrenceType.WEEKLY }
            ?: return
        _uiState.update { it.copy(recurrence = current.copy(intervalWeeks = weeks.coerceAtLeast(1))) }
    }

    /** Appends [date] to the custom-dates list (no duplicates, kept sorted). */
    fun onCustomDateAdded(date: LocalDate) {
        val current = _uiState.value.recurrence
            ?.takeIf { it.type == RecurrenceType.CUSTOM_DATES }
            ?: return
        if (date in current.customDates) return
        val sorted = (current.customDates + date).sorted()
        _uiState.update { it.copy(recurrence = current.copy(customDates = sorted)) }
    }

    /** Removes [date] from the custom-dates list. */
    fun onCustomDateRemoved(date: LocalDate) {
        val current = _uiState.value.recurrence
            ?.takeIf { it.type == RecurrenceType.CUSTOM_DATES }
            ?: return
        _uiState.update { it.copy(recurrence = current.copy(customDates = current.customDates - date)) }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun saveEvent() {
        val state = _uiState.value
        var hasError = false

        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Название обязательно") }
            hasError = true
        }
        if (!state.startDateTime.isBefore(state.endDateTime)) {
            _uiState.update { it.copy(dateRangeError = "Начало должно быть раньше конца") }
            hasError = true
        }
        if (hasError) return

        _uiState.update { it.copy(isSaving = true, titleError = null, dateRangeError = null) }

        viewModelScope.launch {
            val userId = authRepository.getCurrentUser()?.id
            if (userId == null) {
                _uiState.update { it.copy(isSaving = false) }
                _events.send(EventEditorEvent.ShowError("Вы не авторизованы"))
                return@launch
            }

            // Pre-assign IDs for new recurring events so the same ID can be handed
            // to GenerateRecurringEventsUseCase after a successful insert.
            val newId      = if (!isEditing && state.recurrence != null) UUID.randomUUID().toString() else ""
            val newGroupId = if (!isEditing && state.recurrence != null) UUID.randomUUID().toString() else null

            val event  = buildEventToSave(state, userId, newId, newGroupId)
            val result = if (isEditing) updateEventUseCase(event) else addEventUseCase(event)

            _uiState.update { it.copy(isSaving = false) }
            when (result) {
                is Result.Success -> {
                    // Kick off occurrence generation for brand-new recurring events.
                    // Errors here are non-fatal — the master event is already saved.
                    if (!isEditing && event.recurrence != null && event.recurrenceGroupId != null) {
                        generateRecurringEventsUseCase(event)
                    }
                    _events.send(EventEditorEvent.SaveSuccess)
                }
                is Result.Error -> _events.send(
                    EventEditorEvent.ShowError(result.message ?: "Не удалось сохранить событие")
                )
            }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /** Deletes only the currently-open event occurrence. */
    fun deleteThisOnly() {
        viewModelScope.launch {
            when (val result = deleteEventUseCase(eventId)) {
                is Result.Success -> _events.send(EventEditorEvent.DeleteSuccess)
                is Result.Error   -> _events.send(
                    EventEditorEvent.ShowError(result.message ?: "Не удалось удалить событие")
                )
            }
        }
    }

    /** Deletes this occurrence AND all following events in the same recurrence chain. */
    fun deleteThisAndFollowing() {
        val event   = originalEvent ?: return
        val groupId = event.recurrenceGroupId ?: run {
            // No group ID: treat as a plain single-event delete.
            deleteThisOnly()
            return
        }
        viewModelScope.launch {
            when (val result = deleteRecurringEventUseCase(groupId, event.startDateTime)) {
                is Result.Success -> _events.send(EventEditorEvent.DeleteSuccess)
                is Result.Error   -> _events.send(
                    EventEditorEvent.ShowError(result.message ?: "Не удалось удалить события")
                )
            }
        }
    }

    /** Convenience alias — kept so any existing call sites still compile. */
    fun deleteEvent() = deleteThisOnly()

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Builds the [Event] to persist.
     *
     * When editing, [originalEvent] is the base so that immutable fields
     * ([Event.ownerId], [Event.isImported], [Event.sourceScheduleId], [Event.createdAt],
     * [Event.recurrenceGroupId], [Event.isRecurrenceGenerated]) are not overwritten.
     *
     * When creating, [preAssignedId] and [preAssignedGroupId] allow the caller
     * to fix the IDs upfront so they can be reused for occurrence generation.
     */
    private fun buildEventToSave(
        state: EventEditorUiState,
        userId: String,
        preAssignedId: String = "",
        preAssignedGroupId: String? = null,
    ): Event {
        val base = originalEvent
        return if (base != null) {
            base.copy(
                title         = state.title.trim(),
                description   = state.description.trim(),
                startDateTime = state.startDateTime,
                endDateTime   = state.endDateTime,
                color         = state.color,
                reminder      = state.reminder,
                recurrence    = state.recurrence,
            )
        } else {
            Event(
                id                = preAssignedId,
                title             = state.title.trim(),
                description       = state.description.trim(),
                startDateTime     = state.startDateTime,
                endDateTime       = state.endDateTime,
                color             = state.color,
                reminder          = state.reminder,
                recurrence        = state.recurrence,
                ownerId           = userId,
                recurrenceGroupId = preAssignedGroupId,
            )
        }
    }
}
