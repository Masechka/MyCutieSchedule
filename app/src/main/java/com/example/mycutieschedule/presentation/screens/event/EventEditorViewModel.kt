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
import com.example.mycutieschedule.di.ApplicationScope
import com.example.mycutieschedule.presentation.navigation.Screen
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
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
    /**
     * True while a save / delete call is in progress.
     *
     * On a successful save this is intentionally NOT reset to false — the screen
     * will close immediately via [EventEditorEvent.SaveSuccess], so keeping the
     * button disabled prevents accidental double-saves during the navigation transition.
     * It is only reset to false on error so the user can retry.
     */
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
     * ([Event.recurrenceGroupId] is non-null).
     * Controls whether the Delete button shows the "this / this and following" dialog.
     */
    val isRecurringEvent: Boolean     = false,
    /**
     * True while a delete operation is in progress.
     *
     * Like [isSaving], this is intentionally NOT reset on success — the screen closes
     * immediately via [EventEditorEvent.DeleteSuccess], so keeping the button disabled
     * prevents accidental double-deletes during navigation. Reset only on error.
     */
    val isDeleting: Boolean           = false,
    /**
     * True when the loaded event belongs to an imported shared schedule
     * ([Event.isImported] = true).
     *
     * In this mode the screen acts as a view-only inspector:
     *   - save / delete actions are hidden entirely
     *   - form fields become read-only (editing wouldn't propagate anywhere
     *     useful anyway — the next sync from the owner would overwrite it)
     *
     * Always false for new events (we only know after loading an existing one).
     */
    val isReadOnly: Boolean           = false,
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
    /**
     * Application-scoped scope for fire-and-forget background work (recurrence generation).
     * Survives ViewModel clearing so generation completes even after the screen closes.
     */
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    // Deserialise navigation args using type-safe Navigation 2.8+.
    private val route: Screen.EventEditor = savedStateHandle.toRoute()
    private val eventId: String           = route.eventId
    private val prefillMilli: Long?       = route.prefillDateEpochMilli

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
            isLoading     = isEditing,
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
                        // Imported events belong to another user's schedule —
                        // the viewer can inspect but never mutate. Hides edit
                        // and delete actions in EventEditorScreen.
                        isReadOnly       = event.isImported,
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

        val newEnd = if (shouldAutoAdjust) {
            val proposed = value.plusHours(1)
            // Cross-day events are not supported. If plusHours(1) crosses midnight
            // (e.g., start = 23:30 → proposed = 00:30 next day), cap at 23:59 on
            // the same day so the form stays valid without a cross-day end time.
            if (proposed.toLocalDate().isAfter(value.toLocalDate())) {
                value.toLocalDate().atTime(23, 59, 0)
            } else {
                proposed
            }
        } else {
            currentEnd
        }

        _uiState.update {
            it.copy(
                startDateTime  = value,
                endDateTime    = newEnd,
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

    /**
     * Updates the recurrence type, preserving compatible settings from the previous rule:
     * - WEEKLY → keeps interval and any selected days
     * - Everything else → clears days and resets interval
     * - CUSTOM_DATES → keeps existing custom dates list
     * - All types → keeps the end date
     */
    fun onRecurrenceTypeChange(type: RecurrenceType?) {
        val existing        = _uiState.value.recurrence
        val keepDays        = type == RecurrenceType.WEEKLY
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

    /**
     * Toggles a weekday on/off.
     * Only active for WEEKLY (and the legacy CUSTOM_DAYS type for backward compat).
     */
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

    /** Changes the "every N weeks" interval for [RecurrenceType.WEEKLY] (and legacy CUSTOM_DAYS). Clamped to ≥ 1. */
    fun onRecurrenceIntervalChange(weeks: Int) {
        val current = _uiState.value.recurrence
            ?.takeIf { it.type == RecurrenceType.WEEKLY || it.type == RecurrenceType.CUSTOM_DAYS }
            ?: return
        _uiState.update { it.copy(recurrence = current.copy(intervalWeeks = weeks.coerceAtLeast(1))) }
    }

    /** Appends [date] to the custom-dates list (deduplicates, keeps sorted). */
    fun onCustomDateAdded(date: LocalDate) {
        val current = _uiState.value.recurrence
            ?.takeIf { it.type == RecurrenceType.CUSTOM_DATES }
            ?: return
        if (date in current.customDates) return
        _uiState.update { it.copy(recurrence = current.copy(customDates = (current.customDates + date).sorted())) }
    }

    /** Removes [date] from the custom-dates list. */
    fun onCustomDateRemoved(date: LocalDate) {
        val current = _uiState.value.recurrence
            ?.takeIf { it.type == RecurrenceType.CUSTOM_DATES }
            ?: return
        _uiState.update { it.copy(recurrence = current.copy(customDates = current.customDates - date)) }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    /**
     * Validates, persists, and — for recurring events — generates future occurrences.
     *
     * Flow:
     * 1. Validate form fields.
     * 2. Determine the `recurrenceGroupId` to use (preserve existing / assign new / null).
     * 3. Insert (new) or update (existing) the event.
     * 4. If editing: delete any stale generated occurrences for the group.
     * 5. If recurrence is active: call [GenerateRecurringEventsUseCase] (non-fatal).
     * 6. On success: emit [EventEditorEvent.SaveSuccess] — screen closes.
     *    On error: reset [EventEditorUiState.isSaving] to false so the user can retry.
     *
     * **Note:** [EventEditorUiState.isSaving] is intentionally NOT reset on success —
     * the save button stays disabled until the screen is removed from the back stack,
     * preventing accidental double-saves during the navigation transition.
     */
    fun saveEvent() {
        val state = _uiState.value
        if (state.isSaving) return   // guard: already in progress — ignore duplicate taps
        // Defence in depth: imported events are view-only — even if a stray
        // call gets here, refuse silently. UI hides the button to begin with.
        if (state.isReadOnly) return

        var hasError = false

        if (state.title.isBlank()) {
            _uiState.update { it.copy(titleError = "Название обязательно") }
            hasError = true
        }
        if (!state.startDateTime.isBefore(state.endDateTime)) {
            _uiState.update { it.copy(dateRangeError = "Начало должно быть раньше конца") }
            hasError = true
        }
        // Cross-day events break the Day/Week View timeline and are not supported.
        // This can happen if the user manually selects the next day as the end date
        // via the DatePicker — the auto-adjust in onStartDateTimeChange already prevents
        // the auto-generated case (e.g., start 23:30 → end 00:30 next day).
        if (state.startDateTime.toLocalDate() != state.endDateTime.toLocalDate()) {
            _uiState.update { it.copy(dateRangeError = "Событие не может переходить на следующий день") }
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

            val hasRecurrence   = state.recurrence != null
            val originalGroupId = originalEvent?.recurrenceGroupId

            // Determine the recurrenceGroupId for the saved event:
            //   • Removing recurrence       → null (chain ends)
            //   • Already in a chain        → preserve the existing group UUID
            //   • Adding recurrence to event that never had one → new UUID
            val groupId: String? = when {
                !hasRecurrence          -> null
                originalGroupId != null -> originalGroupId
                else                    -> UUID.randomUUID().toString()
            }

            val event  = buildEventToSave(state, userId, groupId)
            val result = if (isEditing) updateEventUseCase(event) else addEventUseCase(event)

            when (result) {
                is Result.Success -> {
                    // ── Step 1: clean up stale generated occurrences ──────────
                    // When editing: purge all generated rows belonging to the (old or new) group
                    // from masterDate+1 so the master event itself is never deleted.
                    // For new events there are no generated rows yet, so this is skipped.
                    // Non-fatal — if cleanup fails we still regenerate below.
                    if (isEditing) {
                        val cleanupGroupId = originalGroupId ?: groupId
                        if (cleanupGroupId != null) {
                            val cleanupFrom = event.startDateTime.toLocalDate()
                                .plusDays(1).atStartOfDay()
                            deleteRecurringEventUseCase(cleanupGroupId, cleanupFrom)
                        }
                    }

                    // ── Step 2: generate fresh occurrences — fire-and-forget ──
                    // Launched on applicationScope so it survives ViewModel clearing.
                    // The screen closes first; Room insertAll completes in the background
                    // and the calendar observes the DB update via Flow.
                    // Non-fatal: master event is already saved; generation errors are logged
                    // inside the use case and don't affect the user.
                    if (hasRecurrence && event.recurrenceGroupId != null) {
                        applicationScope.launch {
                            generateRecurringEventsUseCase(event)
                        }
                    }

                    // ── Step 3: close screen immediately ─────────────────────
                    // isSaving stays true → save button remains disabled until the
                    // composable is removed from the back stack and the ViewModel is cleared.
                    _events.send(EventEditorEvent.SaveSuccess)
                }

                is Result.Error -> {
                    // Allow the user to retry — re-enable the save button.
                    _uiState.update { it.copy(isSaving = false) }
                    _events.send(
                        EventEditorEvent.ShowError(result.message ?: "Не удалось сохранить событие")
                    )
                }
            }
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Deletes only the currently-open event occurrence.
     *
     * Sets [EventEditorUiState.isDeleting] = true immediately to disable the button
     * and prevent double-taps. Not reset on success — the screen closes via
     * [EventEditorEvent.DeleteSuccess]. Reset on error so the user can retry.
     */
    fun deleteThisOnly() {
        if (_uiState.value.isDeleting) return   // guard: ignore if already in progress
        if (_uiState.value.isReadOnly) return   // imported events are view-only
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            when (val result = deleteEventUseCase(eventId)) {
                is Result.Success -> _events.send(EventEditorEvent.DeleteSuccess)
                is Result.Error   -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    _events.send(EventEditorEvent.ShowError(result.message ?: "Не удалось удалить событие"))
                }
            }
        }
    }

    /**
     * Deletes this occurrence AND all following events in the same recurrence chain.
     *
     * Same [EventEditorUiState.isDeleting] guard as [deleteThisOnly].
     */
    fun deleteThisAndFollowing() {
        if (_uiState.value.isDeleting) return   // guard: ignore if already in progress
        if (_uiState.value.isReadOnly) return   // imported events are view-only
        val event   = originalEvent ?: return
        val groupId = event.recurrenceGroupId ?: run {
            // No group ID — fall back to single-event delete.
            deleteThisOnly()
            return
        }
        _uiState.update { it.copy(isDeleting = true) }
        viewModelScope.launch {
            when (val result = deleteRecurringEventUseCase(groupId, event.startDateTime)) {
                is Result.Success -> _events.send(EventEditorEvent.DeleteSuccess)
                is Result.Error   -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    _events.send(EventEditorEvent.ShowError(result.message ?: "Не удалось удалить события"))
                }
            }
        }
    }

    /** Convenience alias so any existing call sites still compile. */
    fun deleteEvent() = deleteThisOnly()

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Builds the [Event] to persist.
     *
     * When editing, [originalEvent] is the base so that immutable fields
     * ([Event.ownerId], [Event.isImported], [Event.sourceScheduleId], [Event.createdAt],
     * [Event.isRecurrenceGenerated]) are not accidentally overwritten.
     *
     * [groupId] is always written explicitly so that adding recurrence to a previously
     * non-recurring event propagates the new group UUID, and removing recurrence clears it.
     */
    private fun buildEventToSave(
        state: EventEditorUiState,
        userId: String,
        groupId: String?,
    ): Event {
        val base = originalEvent
        return if (base != null) {
            // Edit path — preserve immutable fields, update mutable ones.
            base.copy(
                title             = state.title.trim(),
                description       = state.description.trim(),
                startDateTime     = state.startDateTime,
                endDateTime       = state.endDateTime,
                color             = state.color,
                reminder          = state.reminder,
                recurrence        = state.recurrence,
                recurrenceGroupId = groupId,  // explicit so add/remove recurrence is reflected
            )
        } else {
            // Create path — AddEventUseCase assigns an ID if this is blank.
            Event(
                title             = state.title.trim(),
                description       = state.description.trim(),
                startDateTime     = state.startDateTime,
                endDateTime       = state.endDateTime,
                color             = state.color,
                reminder          = state.reminder,
                recurrence        = state.recurrence,
                ownerId           = userId,
                recurrenceGroupId = groupId,
            )
        }
    }
}
