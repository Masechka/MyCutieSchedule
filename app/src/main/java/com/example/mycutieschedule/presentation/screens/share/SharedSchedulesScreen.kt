package com.example.mycutieschedule.presentation.screens.share

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mycutieschedule.domain.model.SharedSchedule
import com.example.mycutieschedule.presentation.theme.ImportedDark
import com.example.mycutieschedule.presentation.theme.ImportedLight
import com.example.mycutieschedule.presentation.theme.ImportedMedium

/**
 * List of imported shared schedules + the current user's own share code.
 *
 * Layout:
 *   ┌─ TopAppBar "Общие расписания" ──────────────────────┐
 *   │ ┌─ Мой код расписания ─────────────────────────┐    │
 *   │ │  A1B2C3       [Копировать]                    │    │
 *   │ │  Поделитесь этим кодом…                       │    │
 *   │ └────────────────────────────────────────────────┘    │
 *   │  Добавленные расписания                              │
 *   │  ┌─ Анна Иванова — 12 событий ──── [×] ──────────┐   │
 *   │  └────────────────────────────────────────────────┘   │
 *   └─────────────────────────────────────────────  [+ Добавить]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedSchedulesScreen(
    onNavigateBack: () -> Unit = {},
    onScheduleClick: (scheduleId: String) -> Unit = {},
    viewModel: SharedSchedulesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager  = LocalClipboardManager.current

    // Handle one-shot events from the ViewModel
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SharedSchedulesEvent.NavigateToDetails -> onScheduleClick(event.scheduleId)
                is SharedSchedulesEvent.ShowSnackbar      -> snackbarHostState.showSnackbar(event.message)
                is SharedSchedulesEvent.CopyToClipboard   -> {
                    clipboardManager.setText(AnnotatedString(event.text))
                    snackbarHostState.showSnackbar("Код скопирован: ${event.text}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Общие расписания") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick     = viewModel::showEnterCodeDialog,
                icon        = { Icon(Icons.Filled.Add, contentDescription = null) },
                text        = { Text("Добавить") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
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
        LazyColumn(
            modifier            = Modifier.fillMaxSize(),
            contentPadding      = PaddingValues(
                start  = 16.dp,
                end    = 16.dp,
                top    = innerPadding.calculateTopPadding() + 12.dp,
                bottom = innerPadding.calculateBottomPadding() + 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── My share code card ───────────────────────────────────────────
            item(key = "my-share-code") {
                MyShareCodeCard(
                    code              = uiState.myShareCode,
                    isLoading         = uiState.isLoadingShareCode,
                    onGenerateClick   = viewModel::generateShareCode,
                    onCopyClick       = viewModel::copyShareCode,
                )
            }

            // ── Section header ───────────────────────────────────────────────
            item(key = "section-header") {
                Text(
                    text     = "Добавленные расписания",
                    style    = MaterialTheme.typography.titleMedium,
                    color    = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
            }

            // ── Empty / loading / list states ────────────────────────────────
            when {
                uiState.isLoadingSchedules -> {
                    item(key = "loading") {
                        Box(
                            modifier         = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                        }
                    }
                }
                uiState.importedSchedules.isEmpty() -> {
                    item(key = "empty") { EmptySchedulesState() }
                }
                else -> {
                    items(
                        items = uiState.importedSchedules,
                        key   = { it.id },
                    ) { schedule ->
                        ImportedScheduleCard(
                            schedule           = schedule,
                            onClick            = { viewModel.onScheduleClick(schedule.id) },
                            onRemove           = { viewModel.removeSchedule(schedule.id) },
                            onToggleVisibility = {
                                viewModel.toggleScheduleVisibility(
                                    scheduleId = schedule.id,
                                    visible    = !schedule.isVisibleInCalendar,
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    // ── Enter-code dialog ────────────────────────────────────────────────────
    if (uiState.showEnterCodeDialog) {
        EnterShareCodeDialog(
            code         = uiState.codeInput,
            error        = uiState.codeInputError,
            isImporting  = uiState.isImporting,
            onCodeChange = viewModel::onCodeInputChange,
            onConfirm    = viewModel::importSchedule,
            onDismiss    = viewModel::dismissEnterCodeDialog,
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Sub-composables
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun MyShareCodeCard(
    code: String?,
    isLoading: Boolean,
    onGenerateClick: () -> Unit,
    onCopyClick: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Outlined.Share,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text  = "Мой код расписания",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text  = "Загрузка кода…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                code != null -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) {
                            Text(
                                text       = code,
                                style      = MaterialTheme.typography.headlineSmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 4.sp,
                                ),
                                color      = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onCopyClick) {
                            Icon(
                                imageVector        = Icons.Filled.ContentCopy,
                                contentDescription = "Копировать код",
                                tint               = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text  = "Поделитесь этим кодом с друзьями, чтобы они могли добавить ваш календарь.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
                else -> {
                    Text(
                        text  = "Создайте код, чтобы поделиться своим расписанием.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onGenerateClick) {
                        Icon(
                            imageVector        = Icons.Filled.Refresh,
                            contentDescription = null,
                            modifier           = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Создать код")
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportedScheduleCard(
    schedule: SharedSchedule,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onToggleVisibility: () -> Unit,
) {
    // Hidden schedules render at reduced opacity so they're visually demoted in
    // the list without being mistaken for something the user lost or broken.
    val cardAlpha = if (schedule.isVisibleInCalendar) 1f else 0.55f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha),
        colors   = CardDefaults.cardColors(
            containerColor = ImportedLight,
            contentColor   = ImportedDark,
        ),
        onClick  = onClick,
    ) {
        Row(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 12.dp),
            verticalAlignment   = Alignment.CenterVertically,
        ) {
            // Left accent stripe — visually marks this as imported
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(ImportedDark),
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    // Prefer the viewer's local alias — if they renamed it,
                    // that's the name they want to see.
                    text       = schedule.displayName.ifBlank { "Без имени" },
                    style      = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color      = ImportedDark,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text  = buildString {
                        append("${schedule.events.size} ${eventCountWord(schedule.events.size)}")
                        append(" • Код: ${schedule.shareCode}")
                        if (!schedule.isVisibleInCalendar) append(" • скрыто")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = ImportedMedium,
                )
            }

            // Quick visibility toggle — saves a trip into details just to flip
            // the eye flag.
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector        = if (schedule.isVisibleInCalendar) Icons.Filled.Visibility
                                         else                              Icons.Filled.VisibilityOff,
                    contentDescription = if (schedule.isVisibleInCalendar) "Скрыть из календаря"
                                         else                              "Показать в календаре",
                    tint               = ImportedDark,
                )
            }

            IconButton(onClick = onRemove) {
                Icon(
                    imageVector        = Icons.Filled.DeleteOutline,
                    contentDescription = "Удалить расписание",
                    tint               = ImportedDark,
                )
            }
        }
    }
}

@Composable
private fun EmptySchedulesState() {
    Column(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector        = Icons.Outlined.Share,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier           = Modifier.size(56.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text      = "У вас пока нет добавленных расписаний",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text      = "Введите код, который вам прислали, чтобы добавить чьё-то расписание",
            style     = MaterialTheme.typography.bodySmall,
            color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EnterShareCodeDialog(
    code: String,
    error: String?,
    isImporting: Boolean,
    onCodeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isImporting) onDismiss() },
        title            = { Text("Добавить расписание") },
        text = {
            Column {
                Text(
                    text  = "Введите 6-значный код, который вам прислал владелец календаря.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value          = code,
                    onValueChange  = onCodeChange,
                    label          = { Text("Код расписания") },
                    singleLine     = true,
                    enabled        = !isImporting,
                    isError        = error != null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                    ),
                    modifier       = Modifier.fillMaxWidth(),
                    supportingText = {
                        AnimatedVisibility(visible = error != null) {
                            Text(
                                text  = error.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isImporting && code.isNotBlank(),
            ) {
                if (isImporting) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Добавляется…")
                } else {
                    Text("Добавить")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isImporting,
            ) {
                Text("Отмена")
            }
        },
    )
}

// ══════════════════════════════════════════════════════════════════════════════
//  Helpers
// ══════════════════════════════════════════════════════════════════════════════

/** Russian plural for "event" — 1 событие, 2-4 события, 5+ событий. */
private fun eventCountWord(count: Int): String {
    val mod100 = count % 100
    if (mod100 in 11..14) return "событий"
    return when (count % 10) {
        1       -> "событие"
        2, 3, 4 -> "события"
        else    -> "событий"
    }
}
