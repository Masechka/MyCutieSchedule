package com.example.mycutieschedule.presentation.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mycutieschedule.core.extensions.toDisplayString
import com.example.mycutieschedule.core.extensions.toEpochMilli
import com.example.mycutieschedule.domain.model.Event
import com.example.mycutieschedule.presentation.theme.AllEventColors
import com.example.mycutieschedule.presentation.theme.Pink200
import com.example.mycutieschedule.presentation.theme.Pink400
import com.example.mycutieschedule.presentation.theme.Pink50
import com.example.mycutieschedule.presentation.theme.Pink500
import com.example.mycutieschedule.presentation.theme.Pink700
import com.example.mycutieschedule.presentation.theme.White
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.ceil

// ── Constants ─────────────────────────────────────────────────────────────────

private const val BASE_PAGE = 500

private val RussianLocale = Locale("ru", "RU")
private val WeekDayNames  = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

// Events area shows the same workload hue at this fraction of the full alpha.
// 0.5 gives a noticeably softer band while keeping the hue recognisable.
private const val EVENTS_AREA_ALPHA = 0.5f

// ── Workload color mapper ─────────────────────────────────────────────────────
//
// Maps a normalized intensity (0f–1f, where 1f = 12 h of events) to a cell
// background color. Uses four stops from the existing Pink scale so the
// gradient stays on-brand and never looks muddy.
//
//   0.00 (  0 h) → transparent      (no events — no tint)
//   0.00 (  0 h) → Pink50  #FFF0F5  (first tint — barely blush)
//   0.33 (  4 h) → Pink200 #FFADD8  (light pastel pink)
//   0.67 (  8 h) → Pink400 #FF4DA6  (warm hot pink)
//   1.00 ( 12 h) → Pink500 #E91E8C  (brand pink — vivid magenta-pink)
//
// lerp() blends smoothly between consecutive stops with no jumps.

private val WorkloadStops = listOf(
    0.00f to Pink50,
    0.33f to Pink200,
    0.67f to Pink400,
    1.00f to Pink500,
)

private fun workloadColor(intensity: Float): Color {
    if (intensity <= 0f) return Color.Transparent
    val t = intensity.coerceAtMost(1f)
    for (i in 0 until WorkloadStops.lastIndex) {
        val (t0, c0) = WorkloadStops[i]
        val (t1, c1) = WorkloadStops[i + 1]
        if (t <= t1) return lerp(c0, c1, ((t - t0) / (t1 - t0)).coerceIn(0f, 1f))
    }
    return WorkloadStops.last().second
}

// ── Cell content color ────────────────────────────────────────────────────────
//
// Three-stop text color scale that adapts to the workload background:
//
//   0.00 → Pink700  #C2185B  dark rose/raspberry   — light/empty cells
//   0.45 → onSurface         neutral dark           — medium-pink cells
//   0.65 → White             near-white             — hot-pink/raspberry cells
//
// Starting from Pink700 instead of onSurface gives light-load days a
// noticeably deeper rose text that pops against the pale pink background
// and stays on-brand. onSurface takes over for the medium range where
// neutral dark achieves better luminance contrast. White closes the scale.

private fun cellContentColor(intensity: Float, onSurface: Color): Color = when {
    intensity < 0.45f -> lerp(Pink700, onSurface, (intensity / 0.45f).coerceIn(0f, 1f))
    intensity > 0.65f -> White
    else              -> lerp(onSurface, White, (intensity - 0.45f) / 0.20f)
}

// Extremely subtle depth shadow — nearly invisible on light days, slightly
// enhances separation on saturated backgrounds.
private val CellTextShadow = Shadow(
    color      = Color.Black.copy(alpha = 0.12f),
    offset     = Offset(0f, 0.8f),
    blurRadius = 2.5f,
)

// Composites a semi-transparent foreground color over an opaque background.
// Used to find the actual on-screen color of the events area so we can
// choose a contrasting text color independent of how alpha is applied.
private fun compositeOverBg(fg: Color, bg: Color): Color {
    val a = fg.alpha
    return Color(
        red   = fg.red   * a + bg.red   * (1f - a),
        green = fg.green * a + bg.green * (1f - a),
        blue  = fg.blue  * a + bg.blue  * (1f - a),
    )
}

// Perceived brightness using the standard luma coefficients (ITU-R BT.601).
// Returns 0f (black) … 1f (white).
private fun Color.perceivedBrightness(): Float =
    red * 0.299f + green * 0.587f + blue * 0.114f

// ── Cell data model ───────────────────────────────────────────────────────────

private data class MonthDayCell(
    val date:           LocalDate,
    val isCurrentMonth: Boolean,
    val isToday:        Boolean,
    val events:         List<Event>,
    val intensity:      Float,          // 0f..1f — occupied-time saturation
)

// ── Cell builder ──────────────────────────────────────────────────────────────

private fun buildMonthCells(
    yearMonth:   YearMonth,
    eventsByDay: Map<LocalDate, List<Event>>,
    dayIntensity: Map<LocalDate, Float>,
    today:       LocalDate,
): List<MonthDayCell> {
    val firstDay    = yearMonth.atDay(1)
    val startOffset = firstDay.dayOfWeek.value - 1  // Mon=0 … Sun=6
    val daysInMonth = yearMonth.lengthOfMonth()
    val totalCells  = ceil((startOffset + daysInMonth) / 7f).toInt() * 7
    val gridStart   = firstDay.minusDays(startOffset.toLong())

    return (0 until totalCells).map { i ->
        val date = gridStart.plusDays(i.toLong())
        MonthDayCell(
            date           = date,
            isCurrentMonth = date.month == yearMonth.month && date.year == yearMonth.year,
            isToday        = date == today,
            events         = eventsByDay[date] ?: emptyList(),
            intensity      = dayIntensity[date] ?: 0f,
        )
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * Month-grid calendar — the app's home screen.
 *
 * Layout:
 *   • [CenterAlignedTopAppBar] — month/year title + prev/next/today navigation
 *   • [WeekDayHeaderRow]       — fixed Пн–Вс column labels
 *   • [HorizontalPager]        — infinite month pager (BASE_PAGE ± offset)
 *       └── [MonthGrid]        — 7-column day grid with intensity shading
 *           └── [DayCell]      — day number, event dots, overflow count
 *
 * @param onDayClick  Called with [LocalDate.toEpochDay] of the tapped day.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthCalendarScreen(
    onDayClick:             (Long) -> Unit = {},
    onWeekViewClick:        () -> Unit = {},
    onAddEventClick:        (Long?) -> Unit = {},
    onSharedSchedulesClick: () -> Unit = {},
    onProfileClick:         () -> Unit = {},
    onSettingsClick:        () -> Unit = {},
    viewModel: MonthCalendarViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val today   = remember { LocalDate.now() }

    val referenceYearMonth = remember { YearMonth.now() }
    val pagerState = rememberPagerState(initialPage = BASE_PAGE) { BASE_PAGE * 2 }

    // Pager → ViewModel: update the loaded month when the user finishes swiping.
    LaunchedEffect(pagerState.settledPage) {
        val offset = (pagerState.settledPage - BASE_PAGE).toLong()
        viewModel.onMonthSwiped(referenceYearMonth.plusMonths(offset))
    }

    // ViewModel → Pager: animate to the target month when prev/next/today is tapped.
    LaunchedEffect(uiState.currentYearMonth) {
        val monthOffset = ChronoUnit.MONTHS.between(referenceYearMonth, uiState.currentYearMonth).toInt()
        val targetPage  = BASE_PAGE + monthOffset
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // Title is derived from the live pager page — updates during swipe animation.
    val visibleYearMonth = remember(pagerState.currentPage) {
        referenceYearMonth.plusMonths((pagerState.currentPage - BASE_PAGE).toLong())
    }
    val topBarTitle = remember(visibleYearMonth) {
        visibleYearMonth.toDisplayString(RussianLocale)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text  = topBarTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                },
                actions = {
                    TextButton(onClick = viewModel::goToToday) {
                        Text(
                            text  = "Сегодня",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = viewModel::goToPrevMonth) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущий месяц")
                    }
                    IconButton(onClick = viewModel::goToNextMonth) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Следующий месяц")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick        = { onAddEventClick(visibleYearMonth.atDay(1).atStartOfDay().toEpochMilli()) },
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector        = Icons.Default.Add,
                    contentDescription = "Добавить событие",
                    tint               = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            WeekDayHeaderRow()
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            HorizontalPager(
                state                  = pagerState,
                modifier               = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
            ) { page ->
                val pageYearMonth = referenceYearMonth.plusMonths((page - BASE_PAGE).toLong())
                // Only supply event data when this page's month is currently loaded.
                val evByDay   = if (pageYearMonth == uiState.currentYearMonth) uiState.eventsByDay   else emptyMap()
                val intensity = if (pageYearMonth == uiState.currentYearMonth) uiState.dayIntensity  else emptyMap()

                val cells = remember(pageYearMonth, evByDay, intensity) {
                    buildMonthCells(pageYearMonth, evByDay, intensity, today)
                }

                MonthGrid(cells = cells, onDayClick = onDayClick)
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun WeekDayHeaderRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        WeekDayNames.forEach { name ->
            Text(
                text      = name,
                modifier  = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style     = MaterialTheme.typography.labelMedium,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
private fun MonthGrid(
    cells:      List<MonthDayCell>,
    onDayClick: (Long) -> Unit,
    modifier:   Modifier = Modifier,
) {
    val rowCount     = cells.size / 7
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxSize()) {
        repeat(rowCount) { weekIndex ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                repeat(7) { dayIndex ->
                    val cell = cells[weekIndex * 7 + dayIndex]
                    DayCell(
                        cell     = cell,
                        onClick  = { onDayClick(cell.date.toEpochDay()) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
            if (weekIndex < rowCount - 1) {
                HorizontalDivider(color = dividerColor)
            }
        }
    }
}

@Composable
private fun DayCell(
    cell:     MonthDayCell,
    onClick:  () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (!cell.isCurrentMonth) Color.Transparent else workloadColor(cell.intensity)
    // Events area uses the same hue at reduced alpha so the workload tint stays
    // recognisable but provides a softer reading surface for event labels.
    val eventsBgColor = when {
        !cell.isCurrentMonth -> Color.Transparent
        cell.intensity <= 0f -> Color.Transparent
        else                 -> bgColor.copy(alpha = EVENTS_AREA_ALPHA)
    }

    val onSurface         = MaterialTheme.colorScheme.onSurface
    // Date area: adapts with workload — dark rose on light days, white on saturated days.
    val dateContentColor  = if (cell.isCurrentMonth) cellContentColor(cell.intensity, onSurface)
                            else onSurface.copy(alpha = 0.3f)
    // Events area: contrast-aware — composite the events bg over the actual surface,
    // then pick dark-rose for light composites and white for dark composites.
    val surface           = MaterialTheme.colorScheme.surface
    val effectiveEventsBg = if (eventsBgColor == Color.Transparent) surface
                            else compositeOverBg(eventsBgColor, surface)
    val eventContentColor = when {
        !cell.isCurrentMonth                            -> onSurface.copy(alpha = 0.3f)
        effectiveEventsBg.perceivedBrightness() > 0.5f -> Pink700
        else                                           -> White
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        // ── Date zone — full workload intensity ───────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(start = 2.dp, top = 3.dp, end = 2.dp, bottom = 2.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(26.dp)
                    .then(
                        if (cell.isToday) Modifier.background(Pink500, CircleShape) else Modifier,
                    ),
            ) {
                Text(
                    text       = cell.date.dayOfMonth.toString(),
                    style      = MaterialTheme.typography.bodySmall.copy(shadow = CellTextShadow),
                    fontWeight = when {
                        cell.isToday        -> FontWeight.Bold
                        cell.isCurrentMonth -> FontWeight.SemiBold
                        else                -> FontWeight.Normal
                    },
                    color      = if (cell.isToday) White else dateContentColor,
                )
            }
        }

        // ── Events zone — same hue, softened (EVENTS_AREA_ALPHA) ─────────
        val visible  = cell.events.take(2)
        val overflow = cell.events.size - visible.size

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(eventsBgColor)
                .padding(start = 2.dp, top = 1.dp, end = 2.dp, bottom = 3.dp),
        ) {
            visible.forEach { event ->
                EventDotLabel(event = event, textColor = eventContentColor)
            }

            if (overflow > 0) {
                Text(
                    text      = "+$overflow",
                    style     = MaterialTheme.typography.labelSmall.copy(shadow = CellTextShadow),
                    color     = eventContentColor.copy(alpha = 0.7f),
                    modifier  = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    textAlign = TextAlign.Start,
                )
            }
        }
    }
}

@Composable
private fun EventDotLabel(
    event:     Event,
    textColor: Color,
    modifier:  Modifier = Modifier,
) {
    val accentColor = AllEventColors.getOrElse(event.color.ordinal) { Pink500 }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 3.dp, vertical = 2.dp),
    ) {
        // Thin vertical accent line — more structured than a circle dot
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 11.dp)
                .background(accentColor, RoundedCornerShape(1.dp)),
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text     = event.title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style    = MaterialTheme.typography.labelSmall.copy(shadow = CellTextShadow),
            color    = textColor,
        )
    }
}
