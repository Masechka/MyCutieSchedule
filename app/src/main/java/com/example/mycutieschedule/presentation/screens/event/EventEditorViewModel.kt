package com.example.mycutieschedule.presentation.screens.event

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycutieschedule.core.extensions.toLocalDateTime
import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.presentation.navigation.Screen
import com.example.mycutieschedule.domain.model.EventColor
import com.example.mycutieschedule.domain.model.Recurrence
import com.example.mycutieschedule.domain.model.RecurrenceType
import com.example.mycutieschedule.domain.model.Reminder
import com.example.mycutieschedule.domain.repository.AuthRepository
import com.example.mycutieschedule.domain.usecase.event.AddEventUseCase
import com.example.mycutieschedule.domain.usecase.event.DeleteEventUseCase
import com.example.mycutieschedule.domain.usecase.event.GetEventByIdUseCase
import com.example.mycutieschedule.domain.usecase.event.UpdateEventUseCase
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
import java.time.LocalDateTime
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
    val isLoading: Boolean       = false,
    /** True while a save / delete network call is in progress. */
    val isSaving: Boolean        = false,
    // ── Form fields ──────────────────────────────────────────────────────────
    val title: String            = "",
    val titleError: String?      = null,
    val description: String      = "",
    val startDateTime: LocalDateTime = defaultStart(),
    val endDateTime: LocalDateTime   = defaultStart().plusHours(1),
    val dateRangeError: String?  = null,
    val color: EventColor        = EventColor.PEONY,
    val reminder: Reminder?      = null,
    val recurrence: Recurrence?  = null,
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
    private val authRepository: AuthRepository,
) : ViewModel() {

    // Deserialise navigation args from SavedStateHandle using type-safe Navigation 2.8+.
    private val route: Screen.EventEditor = savedStateHandle.toRoute()
    private val eventId: String = route.eventId
    private val prefillMilli: Long? = route.prefillDateEpochMilli

    /** True when the screen was opened with an existing event ID. */
    val isEditing: Boolean = eventId.isNotBlank()

    /** Stores the full original event so immutable fields are preserved on update. */
    private var originalEvent: Event? = null

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
                originalEvent = event
                _uiState.update {
                    it.copy(
                        isLoading     = false,
                        title         = event.title,
                        description   = event.description,
                        startDateTime = event.startDateTime,
                        endDateTime   = event.endDateTime,
                        color         = event.color,
                        reminder      = event.reminder,
                        recurrence    = event.recurrence,
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
        _uiState.update { it.copy(startDateTime = value, dateRangeError = null) }
    }

    fun onEndDateTimeChange(value: LocalDateTime) {
        _uiState.update { it.copy(endDateTime = value, dateRangeError = null) }
    }

    fun onColorChange(color: EventColor) {
        _uiState.update { it.copy(color = color) }
    }

    fun onReminderChange(reminder: Reminder?) {
        _uiState.update { it.copy(reminder = reminder) }
    }

    fun onRecurrenceTypeChange(type: RecurrenceType?) {
        val existingDays = _uiState.value.recurrence?.repeatDays ?: emptySet()
        _uiState.update {
            it.copy(
                recurrence = type?.let { t ->
                    Recurrence(
                        type       = t,
                        repeatDays = if (t == RecurrenceType.WEEKLY) existingDays else emptySet(),
                    )
                }
            )
        }
    }

    /**
     * Toggles a day of week in the WEEKLY recurrence's [repeatDays] set.
     * No-op if the current recurrence type is not WEEKLY.
     */
    fun onRepeatDayToggled(day: DayOfWeek) {
        val current = _uiState.value.recurrence
            ?.takeIf { it.type == RecurrenceType.WEEKLY } ?: return
        val updated = current.repeatDays.let { days ->
            if (day in days) days - day else days + day
        }
        _uiState.update { it.copy(recurrence = current.copy(repeatDays = updated)) }
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

            val event = buildEventToSave(state, userId)
            val result = if (isEditing) updateEventUseCase(event) else addEventUseCase(event)

            _uiState.update { it.copy(isSaving = false) }
            when (result) {
                is Result.Success -> _events.send(EventEditorEvent.SaveSuccess)
                is Result.Error   -> _events.send(
                    EventEditorEvent.ShowError(result.message ?: "Не удалось сохранить событие")
                )
            }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun deleteEvent() {
        viewModelScope.launch {
            when (val result = deleteEventUseCase(eventId)) {
                is Result.Success -> _events.send(EventEditorEvent.DeleteSuccess)
                is Result.Error   -> _events.send(
                    EventEditorEvent.ShowError(result.message ?: "Не удалось удалить событие")
                )
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Builds the [Event] to persist.
     *
     * When editing, [originalEvent] is used as the base so that immutable fields
     * ([Event.ownerId], [Event.isImported], [Event.sourceScheduleId], [Event.createdAt])
     * are not accidentally overwritten.
     */
    private fun buildEventToSave(state: EventEditorUiState, userId: String): Event {
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
                title         = state.title.trim(),
                description   = state.description.trim(),
                startDateTime = state.startDateTime,
                endDateTime   = state.endDateTime,
                color         = state.color,
                reminder      = state.reminder,
                recurrence    = state.recurrence,
                ownerId       = userId,
            )
        }
    }
}
