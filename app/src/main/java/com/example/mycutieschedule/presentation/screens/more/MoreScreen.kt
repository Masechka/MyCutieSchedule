package com.example.mycutieschedule.presentation.screens.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * "Ещё" tab — placeholder hub for secondary destinations.
 *
 * Items:
 *   - Профиль       → navigates to [ProfileScreen] (outer nav)
 *   - Настройки     → navigates to [SettingsScreen] (outer nav)
 *   - Расписания    → navigates to [SharedSchedulesScreen] (outer nav)
 *   - Выйти         → calls [MoreViewModel.logout]; auth state change handles navigation
 *
 * Full implementations for each section are added in later stages.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSharedSchedulesClick: () -> Unit = {},
    viewModel: MoreViewModel = hiltViewModel(),
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text  = "Ещё",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Main items ────────────────────────────────────────────────────

            ListItem(
                headlineContent = { Text("Профиль") },
                leadingContent  = {
                    Icon(
                        imageVector        = Icons.Default.Person,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                    )
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.clickable(onClick = onProfileClick),
            )

            ListItem(
                headlineContent = { Text("Настройки") },
                leadingContent  = {
                    Icon(
                        imageVector        = Icons.Default.Settings,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                    )
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.clickable(onClick = onSettingsClick),
            )

            ListItem(
                headlineContent = { Text("Общие расписания") },
                leadingContent  = {
                    Icon(
                        imageVector        = Icons.Default.Share,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.primary,
                    )
                },
                trailingContent = {
                    Icon(Icons.Default.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                modifier = Modifier.clickable(onClick = onSharedSchedulesClick),
            )

            // ── Divider ───────────────────────────────────────────────────────

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color    = MaterialTheme.colorScheme.outlineVariant,
            )

            // ── Logout ────────────────────────────────────────────────────────

            ListItem(
                headlineContent = {
                    Text(
                        text  = "Выйти",
                        color = MaterialTheme.colorScheme.error,
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null,
                        tint               = MaterialTheme.colorScheme.error,
                    )
                },
                modifier = Modifier.clickable(onClick = viewModel::logout),
            )
        }
    }
}
