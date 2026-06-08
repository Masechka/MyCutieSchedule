package com.example.mycutieschedule.presentation.screens.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.model.SharedSchedule
import com.example.mycutieschedule.presentation.theme.ImportedDark
import com.example.mycutieschedule.presentation.theme.ImportedLight
import com.example.mycutieschedule.presentation.theme.ImportedMedium
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Read-only details for a single imported schedule.
 *
 * Shows all events from [SharedSchedule.events], grouped by date, in
 * gray-pink imported colors. The user can remove the entire imported
 * schedule via the top-bar trash icon (after confirmation).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedScheduleDetailsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SharedScheduleDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                SharedScheduleDetailsEvent.Removed         -> onNavigateBack()
                is SharedScheduleDetailsEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        // Prefer the viewer's local alias over the owner-provided name —
                        // the user explicitly chose how to label this schedule.
                        text     = uiState.schedule?.displayName?.takeIf { it.isNotBlank() }
                            ?.let { "Расписание $it" }
                            ?: "Расписание",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (uiState.schedule != null) {
                        val visible = uiState.schedule!!.isVisibleInCalendar
                        IconButton(
                            onClick = viewModel::toggleVisibility,
                            enabled = !uiState.isRemoving,
                        ) {
                            Icon(
                                imageVector        = if (visible) Icons.Filled.Visibility
                                                     else         Icons.Filled.VisibilityOff,
                                contentDescription = if (visible) "Скрыть из календаря"
                                                     else         "Показать в календаре",
                            )
                        }
                        IconButton(
                            onClick = viewModel::showRenameDialog,
                            enabled = !uiState.isRemoving,
                        ) {
                            Icon(
                                imageVector        = Icons.Filled.Edit,
                                contentDescription = "Переименовать",
                            )
                        }
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            enabled = !uiState.isRemoving,
                        ) {
                            Icon(
                                imageVector        = Icons.Filled.DeleteOutline,
                                contentDescription = "Удалить расписание",
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData    = data,
                    containerColor  = MaterialTheme.colorScheme.inverseSurface,
                    contentColor    = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier         = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    }
                }
                uiState.schedule == null -> {
                    NotFoundState()
                }
                else -> {
                    DetailsContent(
                        schedule          = uiState.schedule!!,
                        onToggleWorkload  = viewModel::toggleIncludeInWorkload,
                    )
                }
            }
        }
    }

    // ── Rename dialog ─────────────────────────────────────────────────────────
    if (uiState.renameDialog.isVisible && uiState.schedule != null) {
        RenameImportedScheduleDialog(
            currentAlias = uiState.renameDialog.draft,
            ownerName    = uiState.schedule!!.ownerName,
            isSaving     = uiState.renameDialog.isSaving,
            onChange     = viewModel::onRenameDraftChange,
            onConfirm    = viewModel::confirmRename,
            onDismiss    = viewModel::dismissRenameDialog,
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title            = { Text("Удалить расписание?") },
            text = {
                Text(
                    "Все импортированные события из этого расписания будут удалены из вашего календаря. " +
                            "Оригинальное расписание владельца не пострадает."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.removeSchedule()
                }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена")
                }
            },
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Content
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DetailsContent(
    schedule: SharedSchedule,
    onToggleWorkload: () -> Unit,
) {
    val sortedEvents = remember(schedule.events) {
        schedule.events.sortedBy { it.startDateTime }
    }
    val grouped = remember(sortedEvents) {
        sortedEvents.groupBy { it.startDateTime.toLocalDate() }
            .toSortedMap()
    }

    LazyColumn(
        modifier            = Modifier.fillMaxSize(),
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── Header card ─────────────────────────────────────────────────────
        item(key = "header") {
            HeaderCard(schedule = schedule)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── Per-schedule preferences ────────────────────────────────────────
        item(key = "preferences") {
            WorkloadInclusionCard(
                schedule = schedule,
                onToggle = onToggleWorkload,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (sortedEvents.isEmpty()) {
            item(key = "empty") { EmptyEventsState() }
        } else {
            grouped.forEach { (date, events) ->
                item(key = "header-${date.toEpochDay()}") {
                    DateHeader(date = date)
                }
                events.forEach { event ->
                    item(key = "event-${event.id}") {
                        ImportedEventCard(event = event)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(schedule: SharedSchedule) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.elevatedCardColors(
            containerColor = ImportedLight,
            contentColor   = ImportedDark,
        ),
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment   = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector        = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint               = ImportedDark,
                modifier           = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = schedule.displayName.ifBlank { "Без имени" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = ImportedDark,
                )
                // If the viewer renamed it, show the original owner name underneath
                // as a small reminder of who actually owns the schedule.
                if (schedule.localAliasName != null && schedule.ownerName.isNotBlank()) {
                    Text(
                        text  = "от ${schedule.ownerName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ImportedMedium,
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text  = "Код: ${schedule.shareCode} • ${schedule.events.size} ${eventCountWord(schedule.events.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = ImportedMedium,
                )
                if (!schedule.isVisibleInCalendar) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector        = Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            tint               = ImportedDark,
                            modifier           = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text  = "Скрыто из календаря",
                            style = MaterialTheme.typography.labelSmall,
                            color = ImportedDark,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkloadInclusionCard(
    schedule: SharedSchedule,
    onToggle: () -> Unit,
) {
    // Hidden schedules can never affect workload regardless of the flag, so the
    // toggle is locked off while invisible — with a hint explaining why.
    val isHidden = !schedule.isVisibleInCalendar
    val effective = !isHidden && schedule.includeInWorkload

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = ImportedLight,
            contentColor   = ImportedDark,
        ),
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment   = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = "Учитывать в нагрузке",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = ImportedDark,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text  = when {
                        isHidden ->
                            "Скрытые расписания никогда не учитываются в нагрузке"
                        schedule.includeInWorkload ->
                            "События добавляются к интенсивности дня в месячном виде"
                        else ->
                            "События отображаются, но не влияют на цвет дня"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ImportedMedium,
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked         = effective,
                enabled         = !isHidden,
                onCheckedChange = { onToggle() },
            )
        }
    }
}

@Composable
private fun DateHeader(date: LocalDate) {
    Text(
        text     = formatDateHeader(date),
        style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color    = ImportedDark,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun ImportedEventCard(event: Event) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(
            containerColor = ImportedLight,
            contentColor   = ImportedDark,
        ),
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment   = Alignment.Top,
        ) {
            // Left accent stripe
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(ImportedDark),
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = event.title.ifBlank { "Без названия" },
                    style      = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color      = ImportedDark,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text  = "${TimeFmt.format(event.startDateTime.toLocalTime())} – " +
                            TimeFmt.format(event.endDateTime.toLocalTime()),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                    ),
                    color = ImportedMedium,
                )
                if (event.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text     = event.description,
                        style    = MaterialTheme.typography.bodySmall,
                        color    = ImportedDark.copy(alpha = 0.75f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyEventsState() {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text      = "В этом расписании пока нет событий",
            style     = MaterialTheme.typography.bodyMedium,
            color     = ImportedMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun NotFoundState() {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text      = "Расписание не найдено",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Rename dialog
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun RenameImportedScheduleDialog(
    currentAlias: String,
    ownerName: String,
    isSaving: Boolean,
    onChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title            = { Text("Переименовать расписание") },
        text = {
            Column {
                Text(
                    text  = "Это название видно только вам. Владелец расписания " +
                            "по-прежнему называется «$ownerName».",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value         = currentAlias,
                    onValueChange = onChange,
                    label         = { Text("Локальное имя") },
                    singleLine    = true,
                    enabled       = !isSaving,
                    modifier      = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSaving,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Сохраняется…")
                } else {
                    Text("Сохранить")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text("Отмена")
            }
        },
    )
}

// ══════════════════════════════════════════════════════════════════════════════
//  Formatters & helpers
// ══════════════════════════════════════════════════════════════════════════════

private val TimeFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm")

private val DateHeaderFmt: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale("ru"))

private fun formatDateHeader(date: LocalDate): String =
    DateHeaderFmt.format(date).replaceFirstChar { it.uppercase() }

// Локальный алиас на общую утилиту склонения (core/util/RussianPlurals.kt),
// чтобы не менять call-sites внутри файла. Логика склонения раньше
// дублировалась в SharedSchedulesScreen — теперь источник один.
private fun eventCountWord(count: Int): String =
    com.example.mycutieschedule.core.util.eventCountWordRu(count)
