package com.example.mycutieschedule.presentation.screens.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mycutieschedule.core.extensions.toEpochMilli
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.presentation.theme.toBackgroundColor
import com.example.mycutieschedule.presentation.theme.toOnBackgroundColor
import com.example.mycutieschedule.presentation.theme.toSecondaryTextColor
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Layout constants ──────────────────────────────────────────────────────────

private val HOUR_HEIGHT      = 48.dp   // taller than week view — more room for event detail
private val TIME_COL_WIDTH   = 48.dp
/**
 * Hard floor so every card is at least tappable (~20 min equivalent).
 * Intentionally low — the goal is timeline accuracy over comfort padding.
 */
private val MIN_EVENT_HEIGHT = 20.dp
/**
 * Events shorter than this threshold use compact rendering (single-line title,
 * tighter vertical padding, no description).  Equals ~50 min at HOUR_HEIGHT=48 dp.
 */
private val COMPACT_THRESHOLD = 40.dp

// ── Formatters ────────────────────────────────────────────────────────────────

private val RussianLocale  = Locale("ru")
private val ScreenTitleFmt = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", RussianLocale)

private fun LocalDate.toScreenTitle(): String =
    ScreenTitleFmt.format(this).replaceFirstChar { it.uppercaseChar() }

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * Hourly day timeline for a single date.
 *
 * Shows a full-height time grid with event chips, a current-time indicator
 * (when viewing today), and a free-text notes section at the bottom.
 *
 * @param dateEpochDay   [java.time.LocalDate.toEpochDay] — the day to display.
 * @param onNavigateBack Called when the user taps the back arrow.
 * @param onEventClick   Called with the tapped event's ID (opens EventEditor for edit).
 * @param onAddEventClick Called with a prefill epoch-milli (opens EventEditor for create).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayViewScreen(
    dateEpochDay: Long,
    onNavigateBack: () -> Unit = {},
    onEventClick: (String) -> Unit = {},
    onAddEventClick: (Long?) -> Unit = {},
    viewModel: DayViewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect one-shot errors and show as snackbar
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is DayViewEvent.ShowError -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val scrollState = rememberScrollState()
    val density     = LocalDensity.current
    val today       = remember { LocalDate.now() }
    val isToday     = viewModel.date == today

    // Auto-scroll: jump to ~2 h before current time (today) or to top (other days).
    //
    // Key = isLoadingEvents so this fires AFTER the first event batch loads and
    // visibleHourStart has been updated by computeVisibleHourRange().
    // Firing on the default visibleHourStart = 7 (before events load) would place the
    // scroll offset incorrectly when early events later expand the top of the grid.
    // The guard prevents re-running once the initial scroll has been applied.
    LaunchedEffect(uiState.isLoadingEvents) {
        if (uiState.isLoadingEvents) return@LaunchedEffect   // wait for first load
        val targetMinutes = if (isToday) {
            val nowMinutes = LocalTime.now().hour * 60 + LocalTime.now().minute
            (nowMinutes - 2 * 60).coerceAtLeast(0)
        } else {
            // Non-today days: scroll to top of the (potentially expanded) visible range.
            0
        }
        val relativeMinutes = (targetMinutes - uiState.visibleHourStart * 60).coerceAtLeast(0)
        val targetPx = with(density) { (HOUR_HEIGHT * (relativeMinutes / 60f)).toPx().toInt() }
        scrollState.scrollTo(targetPx)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text  = viewModel.date.toScreenTitle(),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isToday) {
                            Text(
                                text  = "Сегодня",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Prefill with noon of the current day — EventEditor rounds to hour
                    val prefill = viewModel.date.atTime(9, 0).toEpochMilli()
                    onAddEventClick(prefill)
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor   = MaterialTheme.colorScheme.onPrimary,
                elevation      = FloatingActionButtonDefaults.elevation(4.dp),
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить событие")
            }
        },
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState),
        ) {

            // ── Time grid ─────────────────────────────────────────────────────
            DayTimeline(
                positionedEvents = uiState.positionedEvents,
                sourceLabels     = uiState.sourceLabels,
                isLoading        = uiState.isLoadingEvents,
                isToday          = isToday,
                visibleHourStart = uiState.visibleHourStart,
                visibleHourEnd   = uiState.visibleHourEnd,
                onEventClick     = onEventClick,
            )

            HorizontalDivider(Modifier.padding(horizontal = 16.dp))

            // ── Day notes ─────────────────────────────────────────────────────
            DayNotesSection(
                draft      = uiState.noteDraft,
                isModified = uiState.isNoteModified,
                isSaving   = uiState.isSavingNote,
                onChange   = viewModel::onNoteDraftChange,
                onSave     = viewModel::saveNote,
                modifier   = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(96.dp))  // breathing room above system nav bar
        }
    }
}

// ── DayTimeline ───────────────────────────────────────────────────────────────

/**
 * A vertically laid-out time grid with event chips overlaid using absolute offsets.
 *
 * Layout:
 *   [TimeColumn] | [Canvas grid lines + current-time line + EventCards]
 *
 * Event overlap is handled via [DayPositionedEvent.columnIndex] /
 * [DayPositionedEvent.columnCount] — overlapping events share the available width.
 */
@Composable
private fun DayTimeline(
    positionedEvents: List<DayPositionedEvent>,
    sourceLabels: Map<String, String>,
    isLoading: Boolean,
    isToday: Boolean,
    visibleHourStart: Int,
    visibleHourEnd: Int,
    onEventClick: (String) -> Unit,
) {
    val visibleHours    = (visibleHourEnd - visibleHourStart).coerceAtLeast(1)
    val totalGridHeight = HOUR_HEIGHT * visibleHours
    val primaryColor    = MaterialTheme.colorScheme.primary
    val gridLineColor   = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    if (isLoading) {
        Box(
            modifier         = Modifier.fillMaxWidth().height(totalGridHeight),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(totalGridHeight),
    ) {
        // ── Time labels ───────────────────────────────────────────────────────
        DayTimeColumn(
            visibleHourStart = visibleHourStart,
            visibleHourEnd   = visibleHourEnd,
        )

        // ── Grid + events area ────────────────────────────────────────────────
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(end = 8.dp),
        ) {
            val colWidth = maxWidth

            // Hour grid lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0..visibleHours) {
                    val y = size.height / visibleHours * i
                    drawLine(
                        color       = gridLineColor,
                        start       = Offset(0f, y),
                        end         = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                // Half-hour dashed lines
                for (i in 0 until visibleHours) {
                    val y = size.height / visibleHours * i + size.height / visibleHours / 2f
                    drawLine(
                        color       = gridLineColor.copy(alpha = 0.4f),
                        start       = Offset(0f, y),
                        end         = Offset(size.width, y),
                        strokeWidth = 0.5.dp.toPx(),
                        pathEffect  = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())),
                    )
                }

                // Current time indicator — only visible when viewing today
                if (isToday) {
                    val now        = LocalTime.now()
                    val nowMinutes = now.hour * 60 + now.minute
                    val gridStart  = visibleHourStart * 60
                    val gridEnd    = visibleHourEnd   * 60

                    if (nowMinutes in gridStart..gridEnd) {
                        val fraction = (nowMinutes - gridStart).toFloat() / (visibleHours * 60f)
                        val y        = size.height * fraction

                        drawCircle(
                            color  = primaryColor,
                            radius = 5.dp.toPx(),
                            center = Offset(0f, y),
                        )
                        drawLine(
                            color       = primaryColor,
                            start       = Offset(0f, y),
                            end         = Offset(size.width, y),
                            strokeWidth = 2.dp.toPx(),
                        )
                    }
                }
            }

            // Event chips — absolutely positioned inside the grid
            positionedEvents.forEach { positioned ->
                val event       = positioned.event
                val gridOriginMin = visibleHourStart * 60
                val gridEndMin    = visibleHourEnd   * 60

                val startMin = (event.startDateTime.hour * 60 + event.startDateTime.minute)
                    .coerceIn(gridOriginMin, gridEndMin)
                val endMin   = (event.endDateTime.hour   * 60 + event.endDateTime.minute)
                    .coerceIn(gridOriginMin, gridEndMin)

                val topDp     = HOUR_HEIGHT * ((startMin - gridOriginMin) / 60f)
                val rawHeight = HOUR_HEIGHT * ((endMin - startMin) / 60f)
                val heightDp  = rawHeight.coerceAtLeast(MIN_EVENT_HEIGHT)
                // Cards shorter than COMPACT_THRESHOLD get condensed layout so
                // they don't need extra height to fit multi-line / padded content.
                val isCompact = heightDp < COMPACT_THRESHOLD

                val chipWidth: Dp = colWidth / positioned.columnCount
                val chipLeft: Dp  = chipWidth * positioned.columnIndex

                val sourceLabel = if (event.isImported)
                    sourceLabels[event.sourceScheduleId] else null

                DayEventCard(
                    event       = positioned.event,
                    sourceLabel = sourceLabel,
                    isCompact   = isCompact,
                    onClick     = { onEventClick(event.id) },
                    modifier    = Modifier
                        .offset(x = chipLeft, y = topDp)
                        .width(chipWidth)
                        .height(heightDp),
                )
            }

            // Empty-day hint
            if (positionedEvents.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text  = "Событий нет",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    )
                }
            }
        }
    }
}

// ── DayTimeColumn ─────────────────────────────────────────────────────────────

/**
 * Left column showing hour labels.
 * Labels are offset upward by 8 dp so they visually align with the grid line above them.
 */
@Composable
private fun DayTimeColumn(
    visibleHourStart: Int,
    visibleHourEnd: Int,
) {
    val timeFmt = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Box(
        modifier = Modifier
            .width(TIME_COL_WIDTH)
            .fillMaxHeight(),
    ) {
        // Start one hour in (skips the label that would sit at y=0, the top edge).
        // Stop before visibleHourEnd so the final label isn't clipped at the bottom.
        for (hour in (visibleHourStart + 1) until visibleHourEnd) {
            val relativeHour = hour - visibleHourStart
            val label = LocalTime.of(hour % 24, 0).format(timeFmt)

            Text(
                text     = label,
                style    = MaterialTheme.typography.labelSmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .offset(y = HOUR_HEIGHT * relativeHour - 8.dp)
                    .padding(end = 4.dp)
                    .align(Alignment.TopEnd),
            )
        }
    }
}

// ── DayEventCard ──────────────────────────────────────────────────────────────

/**
 * Event chip for the day view.
 *
 * Time-range text is intentionally omitted: the card height already encodes
 * duration on the timeline, and displaying "HH:mm – HH:mm" inside short cards
 * was the primary reason the old [MIN_EVENT_HEIGHT] had to be inflated to 40 dp,
 * making the timeline visually misleading.
 *
 * **Compact mode** ([isCompact] = true, set when card height < [COMPACT_THRESHOLD]):
 *   - single-line title with `TextOverflow.Ellipsis`
 *   - tighter vertical padding (2 dp instead of 4 dp)
 *   - description hidden
 *
 * **Normal mode** ([isCompact] = false):
 *   - up to 2-line title
 *   - description shown if non-blank
 */
@Composable
private fun DayEventCard(
    event: Event,
    sourceLabel: String?,
    onClick: () -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val bgColor       = event.toBackgroundColor()
    val textColor     = event.toOnBackgroundColor()
    val secondaryText = event.toSecondaryTextColor()

    Column(
        modifier = modifier
            .padding(horizontal = 2.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(
                horizontal = 6.dp,
                vertical   = if (isCompact) 2.dp else 4.dp,
            ),
        verticalArrangement = Arrangement.Top,
    ) {
        Text(
            text     = event.title,
            style    = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color    = textColor,
            maxLines = if (isCompact) 1 else 2,
            overflow = TextOverflow.Ellipsis,
        )
        // For imported events, prefer the source schedule label over description —
        // it identifies WHERE the event came from, which is more useful in mixed views.
        if (sourceLabel != null) {
            Text(
                text     = "↗ $sourceLabel",
                style    = MaterialTheme.typography.labelSmall,
                color    = secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else if (!isCompact && event.description.isNotBlank()) {
            Text(
                text     = event.description,
                style    = MaterialTheme.typography.labelSmall,
                color    = secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

// ── DayNotesSection ───────────────────────────────────────────────────────────

/**
 * Free-text note editor for the day.
 * The "Сохранить" button appears only when the draft differs from the saved note.
 */
@Composable
private fun DayNotesSection(
    draft: String,
    isModified: Boolean,
    isSaving: Boolean,
    onChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector        = Icons.Default.Description,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.primary,
                modifier           = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text     = "Заметки на день",
                style    = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            // Save action — visible only when there are unsaved changes
            if (isModified) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    TextButton(onClick = onSave) {
                        Text("Сохранить")
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value         = draft,
            onValueChange = onChange,
            placeholder   = { Text("Добавьте заметки на этот день…") },
            minLines      = 3,
            maxLines      = 10,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction      = ImeAction.Default,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// Color helpers moved to presentation/theme/EventColors.kt — central, with
// imported-event palette support.
