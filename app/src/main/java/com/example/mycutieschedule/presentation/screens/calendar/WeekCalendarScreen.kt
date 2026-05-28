package com.example.mycutieschedule.presentation.screens.calendar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mycutieschedule.core.extensions.toEpochMilli
import com.example.mycutieschedule.core.extensions.toWeekDays
import com.example.mycutieschedule.core.extensions.toWeekStart
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.presentation.theme.toBackgroundColor
import com.example.mycutieschedule.presentation.theme.toOnBackgroundColor
import com.example.mycutieschedule.presentation.theme.toSecondaryTextColor
import com.example.mycutieschedule.presentation.theme.extended
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Layout constants ──────────────────────────────────────────────────────────
private val HOUR_HEIGHT      = 32.dp   // compact: ~14 hours visible on a typical phone
private val TIME_COL_WIDTH   = 42.dp
private val MIN_EVENT_HEIGHT = 30.dp
private const val BASE_PAGE  = 5000

/**
 * Week calendar screen.
 *
 * Layout:
 *   • [CenterAlignedTopAppBar] — month/year title + prev/next/today navigation
 *   • [DayHeaderRow]          — fixed Mon-Sun headers above the scrollable grid
 *   • [HorizontalPager]       — infinite week pager; each page is a scrollable
 *                               time grid ([WeekPageContent])
 *   • [FloatingActionButton]  — opens EventEditor with the current week's Monday
 *                               as the prefill date
 *
 * Event positioning:
 *   Overlap layout is computed in [WeekCalendarViewModel.computeOverlapLayout].
 *   [DayEventsOverlay] turns [PositionedEvent] into absolute Dp offsets using
 *   [BoxWithConstraints] so each event chip occupies the correct time slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekCalendarScreen(
    onNavigateBack: () -> Unit = {},
    onDayClick: (Long) -> Unit = {},
    onAddEventClick: (Long?) -> Unit = {},
    onEventClick: (String) -> Unit = {},
    viewModel: WeekCalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }

    // Reference Monday anchors the pager to page numbers (never changes after launch).
    val referenceMonday = remember { LocalDate.now().toWeekStart() }

    val pagerState = rememberPagerState(initialPage = BASE_PAGE) { BASE_PAGE * 2 }

    // Pager → ViewModel: update loaded week when the user finishes swiping.
    LaunchedEffect(pagerState.settledPage) {
        val offset = (pagerState.settledPage - BASE_PAGE).toLong()
        viewModel.onWeekSwiped(referenceMonday.plusWeeks(offset))
    }

    // ViewModel → Pager: animate to the target page when prev/next/today is tapped.
    LaunchedEffect(uiState.currentWeekMonday) {
        val weekOffset = ((uiState.currentWeekMonday.toEpochDay() - referenceMonday.toEpochDay()) / 7).toInt()
        val targetPage = BASE_PAGE + weekOffset
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Shared vertical scroll state — all pages use the same offset (Google Calendar style).
    val verticalScrollState = rememberScrollState()
    val density = LocalDensity.current

    // Auto-scroll to ~2 hours before current time on first open.
    // Uses the default visible start (07:00) as the grid origin for the initial calculation.
    LaunchedEffect(Unit) {
        val nowMinutes      = LocalTime.now().hour * 60 + LocalTime.now().minute
        val relativeMinutes = (nowMinutes - 7 * 60 - 120).coerceAtLeast(0)
        val targetPx        = with(density) { (HOUR_HEIGHT * (relativeMinutes / 60f)).toPx().toInt() }
        verticalScrollState.scrollTo(targetPx)
    }

    // Derive visible days from pagerState.currentPage (live during swipe animation).
    val visibleMonday = remember(pagerState.currentPage) {
        referenceMonday.plusWeeks((pagerState.currentPage - BASE_PAGE).toLong())
    }
    val visibleDays = remember(visibleMonday) { visibleMonday.toWeekDays() }

    // Top bar title: month + year of the visible week's Monday.
    val topBarTitle = remember(visibleMonday) {
        visibleMonday
            .format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
            .replaceFirstChar { it.uppercase() }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                title = {
                    Text(
                        text = topBarTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                },
                actions = {
                    TextButton(onClick = viewModel::goToToday) {
                        Text(
                            text = "Сегодня",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = viewModel::goToPrevWeek) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущая неделя")
                    }
                    IconButton(onClick = viewModel::goToNextWeek) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Следующая неделя")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    val prefill = uiState.currentWeekMonday.atStartOfDay().toEpochMilli()
                    onAddEventClick(prefill)
                },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить событие", tint = MaterialTheme.colorScheme.onPrimary)
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Fixed day-name headers ──────────────────────────────────────
            DayHeaderRow(
                days = visibleDays,
                today = today,
                onDayClick = onDayClick,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Swipeable week grid ─────────────────────────────────────────
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 1,
            ) { page ->
                val pageMonday    = referenceMonday.plusWeeks((page - BASE_PAGE).toLong())
                val isCurrentWeek = pageMonday == uiState.currentWeekMonday
                // Adjacent pages show an empty default grid; data loads once the pager settles.
                val pageEvents    = if (isCurrentWeek) uiState.eventsByDay    else emptyMap()
                val pageStartHour = if (isCurrentWeek) uiState.visibleHourStart else 7
                val pageEndHour   = if (isCurrentWeek) uiState.visibleHourEnd   else 21
                val pageDays      = remember(pageMonday) { pageMonday.toWeekDays() }

                WeekPageContent(
                    days             = pageDays,
                    eventsByDay      = pageEvents,
                    sourceLabels     = uiState.sourceLabels,
                    today            = today,
                    verticalScrollState = verticalScrollState,
                    onEventClick     = onEventClick,
                    visibleHourStart = pageStartHour,
                    visibleHourEnd   = pageEndHour,
                )
            }
        }
    }
}

// ── DayHeaderRow ──────────────────────────────────────────────────────────────

@Composable
private fun DayHeaderRow(
    days: List<LocalDate>,
    today: LocalDate,
    onDayClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dayNameFmt = remember { DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) }
    Row(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.width(TIME_COL_WIDTH))
        days.forEach { day ->
            val isToday = day == today
            val todayColor = MaterialTheme.extended.todayIndicator
            val weekendColor = MaterialTheme.extended.weekendDayColor
            val dayOfWeek = day.dayOfWeek.value // 1=Mon … 7=Sun
            val isWeekend = dayOfWeek >= 6
            val nameText = day.format(dayNameFmt).replaceFirstChar { it.uppercase() }.trimEnd('.')
            val numberText = day.dayOfMonth.toString()

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onDayClick(day.toEpochDay()) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = nameText,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isToday   -> todayColor
                        isWeekend -> weekendColor
                        else      -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .then(
                            if (isToday) Modifier
                                .clip(CircleShape)
                                .background(todayColor)
                                .padding(6.dp)
                            else Modifier.padding(6.dp)
                        ),
                ) {
                    Text(
                        text = numberText,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = when {
                            isToday   -> MaterialTheme.colorScheme.onPrimary
                            isWeekend -> weekendColor
                            else      -> MaterialTheme.colorScheme.onBackground
                        },
                    )
                }
            }
        }
    }
}

// ── WeekPageContent ───────────────────────────────────────────────────────────

@Composable
private fun WeekPageContent(
    days: List<LocalDate>,
    eventsByDay: Map<LocalDate, List<PositionedEvent>>,
    sourceLabels: Map<String, String>,
    today: LocalDate,
    verticalScrollState: ScrollState,
    onEventClick: (String) -> Unit,
    visibleHourStart: Int,
    visibleHourEnd: Int,
    modifier: Modifier = Modifier,
) {
    val visibleHours    = visibleHourEnd - visibleHourStart
    val totalGridHeight = HOUR_HEIGHT * visibleHours

    Row(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(verticalScrollState),
    ) {
        // Left sidebar: hour labels
        TimeColumn(
            visibleHourStart = visibleHourStart,
            visibleHourEnd   = visibleHourEnd,
            modifier = Modifier.width(TIME_COL_WIDTH).height(totalGridHeight),
        )

        // Day columns with shared hour-grid backdrop
        Box(
            modifier = Modifier
                .weight(1f)
                .height(totalGridHeight),
        ) {
            val gridColor = MaterialTheme.colorScheme.outlineVariant
            // Hour grid lines — one per visible hour boundary
            Canvas(modifier = Modifier.fillMaxSize()) {
                val hourPx = size.height / visibleHours.toFloat()
                for (i in 0..visibleHours) {
                    val y            = i * hourPx
                    val absoluteHour = visibleHourStart + i
                    drawLine(
                        color       = gridColor,
                        start       = Offset(0f, y),
                        end         = Offset(size.width, y),
                        strokeWidth = if (absoluteHour % 6 == 0) 1.5f else 0.7f,
                    )
                }
            }

            // Current time indicator — only when now falls inside the visible range
            if (days.contains(today)) {
                val todayIdx   = days.indexOf(today)
                val nowMinutes = LocalTime.now().hour * 60 + LocalTime.now().minute
                val startMin   = visibleHourStart * 60
                val endMin     = visibleHourEnd   * 60
                if (nowMinutes in startMin..endMin) {
                    val yFraction     = (nowMinutes - startMin).toFloat() / (endMin - startMin)
                    val timeLineColor = MaterialTheme.colorScheme.primary
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val y        = size.height * yFraction
                        val colWidth = size.width / days.size
                        val xStart   = todayIdx * colWidth
                        drawCircle(color = timeLineColor, radius = 5f, center = Offset(xStart, y))
                        drawLine(
                            color       = timeLineColor,
                            start       = Offset(xStart, y),
                            end         = Offset(xStart + colWidth, y),
                            strokeWidth = 2f,
                        )
                    }
                }
            }

            // One column per day
            Row(modifier = Modifier.fillMaxSize()) {
                days.forEach { day ->
                    val sepColor = MaterialTheme.colorScheme.outlineVariant
                    DayEventsOverlay(
                        events           = eventsByDay[day] ?: emptyList(),
                        sourceLabels     = sourceLabels,
                        onEventClick     = onEventClick,
                        visibleHourStart = visibleHourStart,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .drawBehind {
                                drawLine(
                                    color = sepColor,
                                    start = Offset(0f, 0f),
                                    end   = Offset(0f, size.height),
                                    strokeWidth = 0.5f,
                                )
                            },
                    )
                }
            }
        }
    }
}

// ── TimeColumn ────────────────────────────────────────────────────────────────

@Composable
private fun TimeColumn(
    visibleHourStart: Int,
    visibleHourEnd: Int,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        // Start one hour in (skips the label that would sit at y=0, the top edge).
        // Stop before visibleHourEnd so the final label isn't clipped at the bottom.
        for (hour in (visibleHourStart + 1) until visibleHourEnd) {
            val relativeHour = hour - visibleHourStart
            Text(
                text = "%02d:00".format(hour),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 6.dp)
                    .offset(y = HOUR_HEIGHT * relativeHour - 8.dp),
            )
        }
    }
}

// ── DayEventsOverlay ──────────────────────────────────────────────────────────

/**
 * Renders all events for a single day as absolutely-positioned chips.
 *
 * Vertical positions are relative to [visibleHourStart], not midnight, so events
 * align correctly with the visible portion of the time grid regardless of which
 * hour range is currently shown.
 */
@Composable
private fun DayEventsOverlay(
    events: List<PositionedEvent>,
    sourceLabels: Map<String, String>,
    onEventClick: (String) -> Unit,
    visibleHourStart: Int,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val totalWidth    = maxWidth
        val gridOriginMin = visibleHourStart * 60   // minutes-from-midnight at the grid top
        events.forEach { pe ->
            val startMin = pe.event.startDateTime.hour * 60 + pe.event.startDateTime.minute - gridOriginMin
            val endMin   = pe.event.endDateTime.hour   * 60 + pe.event.endDateTime.minute   - gridOriginMin
            val durMin   = (endMin - startMin).coerceIn(30, 24 * 60)

            val topDp    = HOUR_HEIGHT * (startMin / 60f)
            val heightDp = (HOUR_HEIGHT * (durMin / 60f)).coerceAtLeast(MIN_EVENT_HEIGHT)
            val widthFrac = 1f / pe.columnCount
            val leftFrac  = pe.columnIndex.toFloat() / pe.columnCount

            val sourceLabel = if (pe.event.isImported)
                sourceLabels[pe.event.sourceScheduleId] else null

            EventChip(
                event       = pe.event,
                sourceLabel = sourceLabel,
                onClick     = { onEventClick(pe.event.id) },
                modifier = Modifier
                    .width(totalWidth * widthFrac - 2.dp)
                    .height(heightDp)
                    .offset(
                        x = totalWidth * leftFrac + 1.dp,
                        y = topDp,
                    ),
            )
        }
    }
}

// ── EventChip ─────────────────────────────────────────────────────────────────

@Composable
private fun EventChip(
    event: Event,
    sourceLabel: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor       = event.toBackgroundColor()
    val textColor     = event.toOnBackgroundColor()
    val secondaryText = event.toSecondaryTextColor()
    val timeFmt       = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 3.dp),
    ) {
        Text(
            text = event.title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = textColor,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        // For imported events show "from <schedule>" instead of the start time —
        // the time slot is already visually obvious from the chip's position.
        if (sourceLabel != null) {
            Text(
                text = "↗ $sourceLabel",
                style = MaterialTheme.typography.labelSmall,
                color = secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Text(
                text = event.startDateTime.format(timeFmt),
                style = MaterialTheme.typography.labelSmall,
                color = secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
