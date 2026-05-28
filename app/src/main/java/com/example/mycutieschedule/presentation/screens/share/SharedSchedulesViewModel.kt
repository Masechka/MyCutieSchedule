package com.example.mycutieschedule.presentation.screens.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycutieschedule.core.util.Result
import com.example.mycutieschedule.domain.model.SharedSchedule
import com.example.mycutieschedule.domain.repository.AuthRepository
import com.example.mycutieschedule.domain.repository.ShareRepository
import com.example.mycutieschedule.domain.usecase.share.ImportSharedScheduleUseCase
import com.example.mycutieschedule.domain.usecase.share.RemoveImportedScheduleUseCase
import com.example.mycutieschedule.domain.usecase.share.ShareScheduleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── One-shot UI events ────────────────────────────────────────────────────────

sealed class SharedSchedulesEvent {
    data class NavigateToDetails(val scheduleId: String) : SharedSchedulesEvent()
    data class ShowSnackbar(val message: String) : SharedSchedulesEvent()
    data class CopyToClipboard(val text: String) : SharedSchedulesEvent()
}

// ── UI state ──────────────────────────────────────────────────────────────────

/**
 * UI state for [SharedSchedulesScreen].
 *
 * @param isLoadingSchedules   True until the first emission from
 *                             [ShareRepository.observeImportedSchedules] arrives.
 * @param importedSchedules    Live list of schedules the viewer has imported.
 * @param myShareCode          The current user's own share code, if loaded.
 * @param isLoadingShareCode   True while the code is being fetched or generated.
 * @param showEnterCodeDialog  Whether the "Add by code" dialog is visible.
 * @param codeInput            Live text-field value inside the dialog (auto-uppercased).
 * @param codeInputError       Inline error shown below the dialog's text field.
 * @param isImporting          True while an import operation is in flight.
 */
data class SharedSchedulesUiState(
    val isLoadingSchedules: Boolean = true,
    val importedSchedules: List<SharedSchedule> = emptyList(),
    val myShareCode: String? = null,
    val isLoadingShareCode: Boolean = false,
    val showEnterCodeDialog: Boolean = false,
    val codeInput: String = "",
    val codeInputError: String? = null,
    val isImporting: Boolean = false,
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

/**
 * Owns the state for the Shared Schedules list screen.
 *
 * Pulls four pieces of data:
 *   1. The current user's own share code (one-shot Firestore lookup via
 *      [ShareScheduleUseCase], idempotent — returns existing code if any).
 *   2. A live Room-backed list of imported schedules + their events
 *      ([ShareRepository.observeImportedSchedules]).
 *
 * Exposes one-shot events for the screen to handle:
 *   - Navigation to details
 *   - Snackbar messages
 *   - Clipboard copies of the share code
 */
@HiltViewModel
class SharedSchedulesViewModel @Inject constructor(
    private val shareRepository: ShareRepository,
    private val shareScheduleUseCase: ShareScheduleUseCase,
    private val importSharedScheduleUseCase: ImportSharedScheduleUseCase,
    private val removeImportedScheduleUseCase: RemoveImportedScheduleUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SharedSchedulesUiState())
    val uiState: StateFlow<SharedSchedulesUiState> = _uiState.asStateFlow()

    private val _events = Channel<SharedSchedulesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val userId: String? get() = authRepository.getCurrentUser()?.id

    init {
        val uid = userId
        if (uid != null) {
            observeSchedules(uid)
            loadShareCode(uid)
        } else {
            _uiState.update { it.copy(isLoadingSchedules = false) }
        }
    }

    // ── Data observers ────────────────────────────────────────────────────────

    private fun observeSchedules(userId: String) {
        viewModelScope.launch {
            shareRepository.observeImportedSchedules(userId).collect { schedules ->
                _uiState.update {
                    it.copy(
                        isLoadingSchedules = false,
                        importedSchedules  = schedules,
                    )
                }
            }
        }
    }

    /**
     * Silently loads the user's existing share code, if any.
     * [ShareScheduleUseCase] is idempotent — calling it returns the existing
     * code without regenerating, so this is safe to call on init.
     * On error (offline, etc.) we just leave [myShareCode] as null;
     * the user can retry explicitly via [generateShareCode].
     */
    private fun loadShareCode(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingShareCode = true) }
            val result = shareScheduleUseCase(userId)
            _uiState.update {
                it.copy(
                    isLoadingShareCode = false,
                    myShareCode = (result as? Result.Success)?.data,
                )
            }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    /** Generates a share code (or returns existing). User-triggered retry. */
    fun generateShareCode() {
        val uid = userId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingShareCode = true) }
            when (val result = shareScheduleUseCase(uid)) {
                is Result.Success -> _uiState.update {
                    it.copy(isLoadingShareCode = false, myShareCode = result.data)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoadingShareCode = false) }
                    _events.send(
                        SharedSchedulesEvent.ShowSnackbar(
                            "Не удалось создать код: ${result.message ?: "неизвестная ошибка"}"
                        )
                    )
                }
            }
        }
    }

    fun copyShareCode() {
        val code = _uiState.value.myShareCode ?: return
        viewModelScope.launch {
            _events.send(SharedSchedulesEvent.CopyToClipboard(code))
        }
    }

    fun showEnterCodeDialog() {
        _uiState.update {
            it.copy(
                showEnterCodeDialog = true,
                codeInput           = "",
                codeInputError      = null,
            )
        }
    }

    fun dismissEnterCodeDialog() {
        _uiState.update {
            it.copy(
                showEnterCodeDialog = false,
                codeInput           = "",
                codeInputError      = null,
                isImporting         = false,
            )
        }
    }

    fun onCodeInputChange(value: String) {
        // Restrict to alphanumeric, force uppercase, cap at 6 chars.
        val sanitized = value
            .uppercase()
            .filter { it.isLetterOrDigit() }
            .take(SHARE_CODE_LENGTH)
        _uiState.update {
            it.copy(
                codeInput      = sanitized,
                codeInputError = null,
            )
        }
    }

    fun importSchedule() {
        val state = _uiState.value
        if (state.isImporting) return
        val uid = userId ?: return

        val code = state.codeInput.trim()
        if (code.length < SHARE_CODE_LENGTH) {
            _uiState.update {
                it.copy(codeInputError = "Введите 6-значный код")
            }
            return
        }

        // Block re-importing a schedule that's already in the list.
        if (state.importedSchedules.any { it.shareCode.equals(code, ignoreCase = true) }) {
            _uiState.update {
                it.copy(codeInputError = "Это расписание уже добавлено")
            }
            return
        }

        // Block importing your own schedule.
        if (state.myShareCode != null && state.myShareCode.equals(code, ignoreCase = true)) {
            _uiState.update {
                it.copy(codeInputError = "Нельзя добавить собственный код")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, codeInputError = null) }
            when (val result = importSharedScheduleUseCase(code, uid)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isImporting         = false,
                            showEnterCodeDialog = false,
                            codeInput           = "",
                        )
                    }
                    _events.send(SharedSchedulesEvent.ShowSnackbar("Расписание добавлено"))
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isImporting    = false,
                            codeInputError = result.message?.let { msg ->
                                if (msg.contains("not found", ignoreCase = true)) {
                                    "Код не найден"
                                } else {
                                    "Ошибка: $msg"
                                }
                            } ?: "Не удалось добавить расписание",
                        )
                    }
                }
            }
        }
    }

    fun removeSchedule(scheduleId: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            when (val result = removeImportedScheduleUseCase(scheduleId, uid)) {
                is Result.Success -> _events.send(
                    SharedSchedulesEvent.ShowSnackbar("Расписание удалено")
                )
                is Result.Error -> _events.send(
                    SharedSchedulesEvent.ShowSnackbar(
                        "Ошибка при удалении: ${result.message ?: "неизвестная ошибка"}"
                    )
                )
            }
        }
    }

    fun onScheduleClick(scheduleId: String) {
        viewModelScope.launch {
            _events.send(SharedSchedulesEvent.NavigateToDetails(scheduleId))
        }
    }

    private companion object {
        const val SHARE_CODE_LENGTH = 6
    }
}
