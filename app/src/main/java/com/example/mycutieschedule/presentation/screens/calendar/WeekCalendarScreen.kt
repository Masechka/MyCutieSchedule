package com.example.mycutieschedule.presentation.screens.calendar

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 7-column week view with time slots.
 *
 * TODO Stage 8: implement ViewModel, week grid, hour rows, event blocks.
 *
 * @param onDayClick      Called with [java.time.LocalDate.toEpochDay()] of the tapped column header.
 * @param onAddEventClick Opens EventEditor; passes prefill date epoch millis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekCalendarScreen(
    onNavigateBack: () -> Unit = {},
    onDayClick: (Long) -> Unit = {},
    onAddEventClick: (Long?) -> Unit = {},
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Week View") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text("Week Calendar — coming in Stage 8 🩷")
        }
    }
}
