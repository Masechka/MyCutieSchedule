package com.example.mycutieschedule.presentation.screens.event

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mycutieschedule.domain.model.EventColor
import com.example.mycutieschedule.domain.model.RecurrenceType
import com.example.mycutieschedule.domain.model.Reminder
import com.example.mycutieschedule.presentation.theme.AllEventColors
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Locale / formatters (shared across the file) ──────────────────────────────

private val RussianLocale = Locale("ru")
private val DateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", RussianLocale)
private val TimeFmt = DateTimeFormatter.ofPattern("HH:mm")

private fun LocalDateTime.fmtDate(): String = DateFmt.format(toLocalDate())
private fun LocalDateTime.fmtTime(): String = TimeFmt.format(this)

// ── Reminder options ──────────────────────────────────────────────────────────

private val ReminderOptions: List<Pair<Reminder?, String>> = listOf(
    null                                    to "Без напоминания",
    Reminder(minutesBefore = 0)             to "В момент события",
    Reminder(minutesBefore = 5)             to "За 5 минут",
    Reminder(minutesBefore = 10)            to "За 10 минут",
    Reminder(minutesBefore = 15)            to "За 15 минут",
    Reminder(minutesBefore = 30)            to "За 30 минут",
    Reminder(minutesBefore = 60)            to "За 1 час",
    Reminder(minutesBefore = 120)           to "За 2 часа",
    Reminder(minutesBefore = 1440)          to "За 1 день",
)

private fun Reminder?.label(): String =
    ReminderOptions.firstOrNull { it.first?.minutesBefore == this?.minutesBefore && it.first?.type == this?.type }?.second
        ?: ReminderOptions.firstOrNull { it.first == null && this == null }?.second
        ?: "Без напоминания"

// ── Recurrence type options ───────────────────────────────────────────────────

private val RecurrenceOptions: List<Pair<RecurrenceType?, String>> = listOf(
    null                    to "Без повторения",
    RecurrenceType.DAILY    to "Ежедневно",
    RecurrenceType.WEEKLY   to "Еженедельно",
    RecurrenceType.MONTHLY  to "Ежемесячно",
    RecurrenceType.YEARLY   to "Ежегодно",
)

private fun RecurrenceType?.label(): String =
    RecurrenceOptions.firstOrNull { it.first == this }?.second ?: "Без повторения"

// ── Day-of-week labels (Russian short) ───────────────────────────────────────

private val WeekDayLabels: List<Pair<DayOfWeek, String>> = listOf(
    DayOfWeek.MONDAY    to "Пн",
    DayOfWeek.TUESDAY   to "Вт",
    DayOfWeek.WEDNESDAY to "Ср",
    DayOfWeek.THURSDAY  to "Чт",
    DayOfWeek.FRIDAY    to "Пт",
    DayOfWeek.SATURDAY  to "Сб",
    DayOfWeek.SUNDAY    to "Вс",
)

// ── Color helpers ─────────────────────────────────────────────────────────────

/** Maps [EventColor] to its Compose [Color] by ordinal (matches [AllEventColors] order). */
private fun EventColor.toComposeColor(): Color = AllEventColors[ordinal]

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * Create or edit an event.
 *
 * Opens as a bottom-sheet modal (slides up), so it uses a Close (×) icon
 * instead of a back arrow. The Save action is a checkmark in the top bar.
 *
 * @param eventId               Existing event's ID. Empty string = create new.
 * @param prefillDateEpochMilli Pre-filled start epoch millis when creating from a slot.
 * @param onNavigateBack        Called on save success, delete success, or Close tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorScreen(
    eventId: String = "",
    prefillDateEpochMilli: Long? = null,
    onNavigateBack: () -> Unit = {},
    viewModel: EventEditorViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect one-shot events: navigation + snackbar
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is EventEditorEvent.SaveSuccess   -> onNavigateBack()
                is EventEditorEvent.DeleteSuccess -> onNavigateBack()
                is EventEditorEvent.ShowError     -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // ── Dialog visibility flags ───────────────────────────────────────────────
    var startDateDialogOpen   by remember { mutableStateOf(false) }
    var startTimeDialogOpen   by remember { mutableStateOf(false) }
    var endDateDialogOpen     by remember { mutableStateOf(false) }
    var endTimeDialogOpen     by remember { mutableStateOf(false) }
    var reminderDialogOpen    by remember { mutableStateOf(false) }
    var recurrenceDialogOpen  by remember { mutableStateOf(false) }
    var deleteConfirmOpen     by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                },
                title = {
                    Text(if (viewModel.isEditing) "Редактирование" else "Новое событие")
                },
                actions = {
                    // Delete button — only visible when editing an existing event
                    if (viewModel.isEditing) {
                        IconButton(onClick = { deleteConfirmOpen = true }) {
                            Icon(
                                imageVector   = Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint          = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    // Save button — shows spinner while saving
                    IconButton(
                        onClick  = viewModel::saveEvent,
                        enabled  = !uiState.isSaving,
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color       = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "Сохранить")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->

        // ── Loading state ─────────────────────────────────────────────────────
        if (uiState.isLoading) {
            Box(
                modifier         = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        // ── Form ──────────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {

            Spacer(Modifier.height(8.dp))

            // ── Title field ───────────────────────────────────────────────────
            OutlinedTextField(
                value           = uiState.title,
                onValueChange   = viewModel::onTitleChange,
                label           = { Text("Название *") },
                singleLine      = true,
                isError         = uiState.titleError != null,
                supportingText  = uiState.titleError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction      = ImeAction.Next,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(8.dp))

            // ── Description / notes field ──────────────────────────────────
            OutlinedTextField(
                value           = uiState.description,
                onValueChange   = viewModel::onDescriptionChange,
                label           = { Text("Описание") },
                minLines        = 2,
                maxLines        = 5,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction      = ImeAction.Done,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))

            // ── Start date / time ─────────────────────────────────────────────
            ListItem(
                leadingContent = {
                    Icon(
                        imageVector        = Icons.Default.Schedule,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                    )
                },
                headlineContent = {
                    Text("Начало", style = MaterialTheme.typography.bodyMedium)
                },
                trailingContent = {
                    DateTimeButtons(
                        dateText    = uiState.startDateTime.fmtDate(),
                        timeText    = uiState.startDateTime.fmtTime(),
                        onDateClick = { startDateDialogOpen = true },
                        onTimeClick = { startTimeDialogOpen = true },
                    )
                },
            )

            // ── End date / time ───────────────────────────────────────────────
            ListItem(
                leadingContent = { Spacer(Modifier.size(24.dp)) },  // aligns with icon above
                headlineContent = {
                    Text("Конец", style = MaterialTheme.typography.bodyMedium)
                },
                trailingContent = {
                    DateTimeButtons(
                        dateText    = uiState.endDateTime.fmtDate(),
                        timeText    = uiState.endDateTime.fmtTime(),
                        onDateClick = { endDateDialogOpen = true },
                        onTimeClick = { endTimeDialogOpen = true },
                    )
                },
            )

            // Date-range validation error
            uiState.dateRangeError?.let { err ->
                Text(
                    text  = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }

            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // ── Color picker ──────────────────────────────────────────────────
            Text(
                text     = "Цвет события",
                style    = MaterialTheme.typography.labelLarge,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding        = PaddingValues(horizontal = 16.dp),
                modifier              = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                items(EventColor.entries) { ec ->
                    ColorSwatch(
                        color    = ec.toComposeColor(),
                        selected = uiState.color == ec,
                        onClick  = { viewModel.onColorChange(ec) },
                    )
                }
            }

            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            // ── Reminder ──────────────────────────────────────────────────────
            ListItem(
                leadingContent = {
                    Icon(
                        imageVector        = Icons.Default.Notifications,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                    )
                },
                headlineContent = { Text("Напоминание") },
                trailingContent = {
                    Text(
                        text  = uiState.reminder.label(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.clickable { reminderDialogOpen = true },
            )

            // ── Recurrence ────────────────────────────────────────────────────
            ListItem(
                leadingContent = {
                    Icon(
                        imageVector        = Icons.Default.Repeat,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                    )
                },
                headlineContent = { Text("Повторение") },
                trailingContent = {
                    Text(
                        text  = uiState.recurrence?.type.label(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.clickable { recurrenceDialogOpen = true },
            )

            // Day-of-week chips — visible only when recurrence type is WEEKLY
            if (uiState.recurrence?.type == RecurrenceType.WEEKLY) {
                val selectedDays = uiState.recurrence?.repeatDays ?: emptySet()
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    WeekDayLabels.forEach { (day, label) ->
                        FilterChip(
                            selected = day in selectedDays,
                            onClick  = { viewModel.onRepeatDayToggled(day) },
                            label    = { Text(label) },
                        )
                    }
                }
            }

            // Bottom padding so the last item isn't hidden behind the system nav bar
            Spacer(Modifier.height(32.dp))
        }
    }

    // ── Dialogs (rendered outside the scaffold content to avoid clipping) ─────

    if (startDateDialogOpen) {
        EventDatePickerDialog(
            initialDate = uiState.startDateTime.toLocalDate(),
            onDismiss   = { startDateDialogOpen = false },
            onConfirm   = { newDate ->
                viewModel.onStartDateTimeChange(
                    newDate.atTime(uiState.startDateTime.toLocalTime())
                )
                startDateDialogOpen = false
            },
        )
    }

    if (endDateDialogOpen) {
        EventDatePickerDialog(
            initialDate = uiState.endDateTime.toLocalDate(),
            onDismiss   = { endDateDialogOpen = false },
            onConfirm   = { newDate ->
                viewModel.onEndDateTimeChange(
                    newDate.atTime(uiState.endDateTime.toLocalTime())
                )
                endDateDialogOpen = false
            },
        )
    }

    if (startTimeDialogOpen) {
        EventTimePickerDialog(
            title        = "Время начала",
            initialHour  = uiState.startDateTime.hour,
            initialMinute = uiState.startDateTime.minute,
            onDismiss    = { startTimeDialogOpen = false },
            onConfirm    = { hour, minute ->
                viewModel.onStartDateTimeChange(
                    uiState.startDateTime.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
                )
                startTimeDialogOpen = false
            },
        )
    }

    if (endTimeDialogOpen) {
        EventTimePickerDialog(
            title        = "Время окончания",
            initialHour  = uiState.endDateTime.hour,
            initialMinute = uiState.endDateTime.minute,
            onDismiss    = { endTimeDialogOpen = false },
            onConfirm    = { hour, minute ->
                viewModel.onEndDateTimeChange(
                    uiState.endDateTime.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
                )
                endTimeDialogOpen = false
            },
        )
    }

    if (reminderDialogOpen) {
        ReminderPickerDialog(
            current   = uiState.reminder,
            onDismiss = { reminderDialogOpen = false },
            onSelect  = { reminder ->
                viewModel.onReminderChange(reminder)
                reminderDialogOpen = false
            },
        )
    }

    if (recurrenceDialogOpen) {
        RecurrencePickerDialog(
            current   = uiState.recurrence?.type,
            onDismiss = { recurrenceDialogOpen = false },
            onSelect  = { type ->
                viewModel.onRecurrenceTypeChange(type)
                recurrenceDialogOpen = false
            },
        )
    }

    if (deleteConfirmOpen) {
        AlertDialog(
            onDismissRequest = { deleteConfirmOpen = false },
            title   = { Text("Удалить событие?") },
            text    = { Text("Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(
                    onClick = { deleteConfirmOpen = false; viewModel.deleteEvent() },
                    // Red-ish tint for a destructive action
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmOpen = false }) { Text("Отмена") }
            },
        )
    }
}

// ── Reusable sub-composables ──────────────────────────────────────────────────

/**
 * Two inline text buttons: one for the date, one for the time.
 * Tapped independently to open separate pickers.
 */
@Composable
private fun DateTimeButtons(
    dateText: String,
    timeText: String,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onDateClick, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
            Text(dateText, style = MaterialTheme.typography.bodyMedium)
        }
        Text(
            text  = "·",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = onTimeClick, contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)) {
            Text(timeText, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Filled circle swatch for the event color picker.
 * Shows a ring border when selected.
 */
@Composable
private fun ColorSwatch(
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (selected) Modifier.border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.onSurface,
                    shape = CircleShape,
                ) else Modifier
            )
            .clickable(onClick = onClick),
    )
}

/**
 * Material 3 [DatePickerDialog] wrapper.
 * Initial selection defaults to the provided [initialDate].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val initialMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        onConfirm(
                            Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate()
                        )
                    } else {
                        onDismiss()
                    }
                },
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    ) {
        DatePicker(state = state)
    }
}

/**
 * [TimePicker] wrapped inside an [AlertDialog].
 * Uses 24-hour format to match the rest of the Russian-locale UI.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour   = initialHour,
        initialMinute = initialMinute,
        is24Hour      = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title   = { Text(title) },
        text    = {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

/**
 * AlertDialog with a radio-button list for picking a reminder interval.
 */
@Composable
private fun ReminderPickerDialog(
    current: Reminder?,
    onDismiss: () -> Unit,
    onSelect: (Reminder?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Напоминание") },
        text  = {
            Column {
                ReminderOptions.forEach { (option, label) ->
                    val isSelected = when {
                        option == null && current == null -> true
                        option != null && current != null &&
                            option.minutesBefore == current.minutesBefore -> true
                        else -> false
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}

/**
 * AlertDialog with a radio-button list for picking a recurrence type.
 */
@Composable
private fun RecurrencePickerDialog(
    current: RecurrenceType?,
    onDismiss: () -> Unit,
    onSelect: (RecurrenceType?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Повторение") },
        text  = {
            Column {
                RecurrenceOptions.forEach { (option, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 8.dp),
                    ) {
                        RadioButton(selected = option == current, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        },
    )
}
