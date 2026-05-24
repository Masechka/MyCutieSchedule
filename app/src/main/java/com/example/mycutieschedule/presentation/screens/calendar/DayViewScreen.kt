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
import java.time.LocalDate

/**
 * Hourly day timeline for a specific date.
 *
 * TODO Stage 8: implement ViewModel, time-slot grid, event blocks.
 *
 * @param dateEpochDay [java.time.LocalDate.toEpochDay()] — the day to display.
 * @param onEventClick Called with the tapped event's ID.
 * @param onAddEventClick Opens EventEditor pre-filled with a time slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayViewScreen(
    dateEpochDay: Long,
    onNavigateBack: () -> Unit = {},
    onEventClick: (String) -> Unit = {},
    onAddEventClick: (Long?) -> Unit = {},
) {
    val date = LocalDate.ofEpochDay(dateEpochDay)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(date.toString()) },
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
            Text("Day View ($date) — coming in Stage 8 🩷")
        }
    }
}
