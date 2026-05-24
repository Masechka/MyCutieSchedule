package com.example.mycutieschedule.presentation.screens.event

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
 * Create or edit an event.
 *
 * Uses a Close icon (×) instead of Back arrow because this screen slides up
 * from the bottom as a modal — closing it feels different from navigating back.
 *
 * TODO Stage 9: implement ViewModel, form fields (title, time, color, reminder).
 *
 * @param eventId               Existing event ID. Empty string = new event.
 * @param prefillDateEpochMilli Pre-filled start date/time when opening from a specific slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorScreen(
    eventId: String = "",
    prefillDateEpochMilli: Long? = null,
    onNavigateBack: () -> Unit = {},
) {
    val isEditing = eventId.isNotBlank()
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isEditing) "Edit Event" else "New Event") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text("Event Editor — coming in Stage 9 🩷")
        }
    }
}
