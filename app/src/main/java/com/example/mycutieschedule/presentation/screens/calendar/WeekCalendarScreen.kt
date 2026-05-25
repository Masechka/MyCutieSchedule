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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mycutieschedule.core.extensions.toEpochMilli
import com.example.mycutieschedule.core.extensions.toWeekDays
import com.example.mycutieschedule.core.extensions.toWeekStart
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.domain.model.EventColor
import com.example.mycutieschedule.presentation.theme.AllEventColors
import com.example.mycutieschedule.presentation.theme.extended
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Layout constants ──────────────────────────────────────────────────────────
private val HOUR_HEIGHT     = 64.dp
private val TIME_COL_WIDTH  = 52.dp
private val TOTAL_GRID_HEIGHT = HOUR_HEIGHT * 24
private val MIN_EVENT_HEIGHT  = 30.dp
private const val BASE_PAGE   = 5000

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

    // Auto-scroll to show current time on first open.
    LaunchedEffect(Unit) {
        val nowMinutes = LocalTime.now().hour * 60 + LocalTime.now().minute
        // Aim to show ~2 hours above current time.
        val targetDp = HOUR_HEIGHT * ((nowMinutes / 60f) - 2f).coerceAtLeast(0f)
        // Convert Dp → px naively using typical density (actual density inaccessible here;
        // close enough for initial scroll — ComposeView will refine on first measure pass).
        verticalScrollState.scrollTo((targetDp.value * 3).toInt())
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
                val pageMonday = referenceMonday.plusWeeks((page - BASE_PAGE).toLong())
                // Show data only for the settled week — adjacent pages show empty grids
                // and populate once the pager settles and the ViewModel loads them.
                val pageEvents = if (pageMonday == uiState.currentWeekMonday)
                    uiState.eventsByDay else emptyMap()
                val pageDays = remember(pageMonday) { pageMonday.toWeekDays() }

                WeekPageContent(
                    days = pageDays,
                    eventsByDay = pageEvents,
                    today = today,
                    verticalScrollState = verticalScrollState,
                    onEventClick = onEventClick,
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
    today: LocalDate,
    verticalScrollState: ScrollState,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(verticalScrollState),
    ) {
        // Left sidebar: hour labels
        TimeColumn(modifier = Modifier.width(TIME_COL_WIDTH).height(TOTAL_GRID_HEIGHT))

        // Day columns with shared hour-grid backdrop
        Box(
            modifier = Modifier
                .weight(1f)
                .height(TOTAL_GRID_HEIGHT),
        ) {
            val gridColor = MaterialTheme.colorScheme.outlineVariant
            // Hour grid lines
            Canvas(modifier = Modifier.fillMaxSize()) {
                val hourPx = size.height / 24f
                for (hour in 0..24) {
                    val y = hour * hourPx
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end   = Offset(size.width, y),
                        strokeWidth = if (hour % 6 == 0) 1.5f else 0.7f,
                    )
                }
            }

            // Current time indicator on today's column position
            if (days.contains(today)) {
                val todayIdx = days.indexOf(today)
                val nowMinutes = LocalTime.now().hour * 60 + LocalTime.now().minute
                val yFraction = nowMinutes / (24f * 60f)
                val timeLineColor = MaterialTheme.colorScheme.primary
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val y = size.height * yFraction
                    val colWidth = size.width / days.size
                    val xStart = todayIdx * colWidth
                    drawCircle(
                        color = timeLineColor,
                        radius = 5f,
                        center = Offset(xStart, y),
                    )
                    drawLine(
                        color = timeLineColor,
                        start = Offset(xStart, y),
                        end   = Offset(xStart + colWidth, y),
                        strokeWidth = 2f,
                    )
                }
            }

            // One column per day
            Row(modifier = Modifier.fillMaxSize()) {
                days.forEach { day ->
                    val sepColor = MaterialTheme.colorScheme.outlineVariant
                    DayEventsOverlay(
                        events = eventsByDay[day] ?: emptyList(),
                        onEventClick = onEventClick,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .drawBehind {
                                drawLine(
                                    color = sepColor,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
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
private fun TimeColumn(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        // Skip "00:00" — it sits right at the top edge and looks crowded.
        for (hour in 1..23) {
            Text(
                text = "%02d:00".format(hour),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 6.dp)
                    .offset(y = HOUR_HEIGHT * hour - 8.dp),
            )
        }
    }
}

// ── DayEventsOverlay ──────────────────────────────────────────────────────────

/**
 * Renders all events for a single day as absolutely-positioned chips within
 * a [BoxWithConstraints] whose height equals [TOTAL_GRID_HEIGHT].
 *
 * Each chip's vertical position and height are derived from the event's
 * start/end minutes relative to midnight.
 */
@Composable
private fun DayEventsOverlay(
    events: List<PositionedEvent>,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val totalWidth = maxWidth
        events.forEach { pe ->
            val startMin = pe.event.startDateTime.hour * 60 + pe.event.startDateTime.minute
            val endMin   = pe.event.endDateTime.hour * 60 + pe.event.endDateTime.minute
            val durMin   = (endMin - startMin).coerceIn(30, 24 * 60)

            val topDp    = HOUR_HEIGHT * (startMin / 60f)
            val heightDp = (HOUR_HEIGHT * (durMin / 60f)).coerceAtLeast(MIN_EVENT_HEIGHT)
            val widthFrac  = 1f / pe.columnCount
            val leftFrac   = pe.columnIndex.toFloat() / pe.columnCount

            EventChip(
                event   = pe.event,
                onClick = { onEventClick(pe.event.id) },
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor   = event.color.toComposeColor()
    val textColor = event.color.onComposeColor()
    val timeFmt   = remember { DateTimeFormatter.ofPattern("HH:mm") }

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
        Text(
            text = event.startDateTime.format(timeFmt),
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ── Color helpers ─────────────────────────────────────────────────────────────

/** Maps [EventColor] to its Compose [Color] by ordinal (matches [AllEventColors] order). */
private fun EventColor.toComposeColor(): Color = AllEventColors[ordinal]

/** Returns a legible text color (white or dark pink) for text drawn on this event color. */
private fun EventColor.onComposeColor(): Color = when (this) {
    EventColor.PEONY,
    EventColor.ROSE,
    EventColor.RASPBERRY -> Color.White
    else                 -> Color(0xFF4A0028) // Pink900 — readable on pale shades
}
