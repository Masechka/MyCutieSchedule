package com.example.mycutieschedule.presentation.screens.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Month-grid calendar — the app's home screen.
 * Displays a scrollable calendar with colored event tiles.
 *
 * TODO Stage 8: implement ViewModel, month grid, event tiles, FAB.
 *
 * @param onDayClick              Called with [java.time.LocalDate.toEpochDay()] of the tapped day.
 * @param onWeekViewClick         Switches to the week view.
 * @param onAddEventClick         Opens [EventEditorScreen]; passes prefill date epoch millis.
 * @param onSharedSchedulesClick  Opens the shared schedules list.
 * @param onProfileClick          Opens the profile screen.
 * @param onSettingsClick         Opens settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthCalendarScreen(
    onDayClick: (Long) -> Unit = {},
    onWeekViewClick: () -> Unit = {},
    onAddEventClick: (Long?) -> Unit = {},
    onSharedSchedulesClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("My Cutie Schedule 🩷") })
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text("Month Calendar — coming in Stage 8 🩷")
        }
    }
}
