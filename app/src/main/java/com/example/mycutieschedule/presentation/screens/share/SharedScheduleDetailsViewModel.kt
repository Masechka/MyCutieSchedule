package com.example.mycutieschedule.presentation.screens.share

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.SharedSchedule
import com.example.mycutieschedule.domain.repository.AuthRepository
import com.example.mycutieschedule.domain.repository.ShareRepository
import com.example.mycutieschedule.domain.usecase.share.RemoveImportedScheduleUseCase
import com.example.mycutieschedule.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── One-shot UI events ────────────────────────────────────────────────────────

sealed class SharedScheduleDetailsEvent {
    /** Schedule was successfully removed — caller should pop back stack. */
    data object Removed : SharedScheduleDetailsEvent()
    data class ShowSnackbar(val message: String) : SharedScheduleDetailsEvent()
}

// ── UI state ──────────────────────────────────────────────────────────────────

/**
 * UI state for [SharedScheduleDetailsScreen].
 *
 * @param schedule     The imported schedule with its events. Null until loaded
 *                     or when the schedule has been removed.
 * @param isLoading    True until the first emission from the Room flow arrives.
 * @param isRemoving   True while a removal is in flight (disables the delete button).
 */
data class SharedScheduleDetailsUiState(
    val isLoading: Boolean = true,
    val schedule: SharedSchedule? = null,
    val isRemoving: Boolean = false,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * Owns the state for a single imported schedule's details screen.
 *
 * Filters [ShareRepository.observeImportedSchedules] by the schedule ID passed
 * via navigation arguments. When the schedule is removed (either via this
 * screen's [removeSchedule] or externally), the flow emits null and the screen
 * navigates back via [SharedScheduleDetailsEvent.Removed].
 */
@HiltViewModel
class SharedScheduleDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val shareRepository: ShareRepository,
    private val removeImportedScheduleUseCase: RemoveImportedScheduleUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val scheduleId: String =
        savedStateHandle.toRoute<Screen.SharedScheduleDetails>().scheduleId

    private val _uiState = MutableStateFlow(SharedScheduleDetailsUiState())
    val uiState: StateFlow<SharedScheduleDetailsUiState> = _uiState.asStateFlow()

    private val _events = Channel<SharedScheduleDetailsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val userId: String? get() = authRepository.getCurrentUser()?.id

    init {
        val uid = userId
        if (uid != null) {
            observeSchedule(uid)
        } else {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun observeSchedule(userId: String) {
        viewModelScope.launch {
            shareRepository.observeImportedSchedules(userId)
                .map { list -> list.find { it.id == scheduleId } }
                .collect { schedule ->
                    _uiState.update {
                        it.copy(isLoading = false, schedule = schedule)
                    }
                }
        }
    }

    /**
     * Deletes the schedule + all its imported events from Room.
     * On success, emits [SharedScheduleDetailsEvent.Removed] so the screen
     * can pop the back stack.
     */
    fun removeSchedule() {
        val uid = userId ?: return
        if (_uiState.value.isRemoving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isRemoving = true) }
            when (val result = removeImportedScheduleUseCase(scheduleId, uid)) {
                is Result.Success -> _events.send(SharedScheduleDetailsEvent.Removed)
                is Result.Error -> {
                    _uiState.update { it.copy(isRemoving = false) }
                    _events.send(
                        SharedScheduleDetailsEvent.ShowSnackbar(
                            "Ошибка при удалении: ${result.message ?: "неизвестная ошибка"}"
                        )
                    )
                }
            }
        }
    }
}
