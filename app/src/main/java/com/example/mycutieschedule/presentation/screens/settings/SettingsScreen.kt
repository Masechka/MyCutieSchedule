package com.example.mycutieschedule.presentation.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mycutieschedule.domain.model.ThemeMode

/**
 * Settings screen — currently exposes the theme picker.
 *
 * Theme selection writes through [SettingsViewModel.setThemeMode] →
 * [com.example.mycutieschedule.domain.usecase.settings.ChangeThemeUseCase] →
 * DataStore. The root [com.example.mycutieschedule.presentation.AppContent]
 * observes the same DataStore key via [com.example.mycutieschedule.presentation.AppViewModel],
 * so the new theme is applied to the whole app instantly without recreating
 * the Activity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ThemeSection(
                current = uiState.themeMode,
                onSelect = viewModel::setThemeMode,
            )
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Sections
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ThemeSection(
    current: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
) {
    SectionCard(
        title = "Тема приложения",
        icon  = Icons.Outlined.Palette,
    ) {
        ThemeOption(
            mode      = ThemeMode.SYSTEM,
            label     = "Как в системе",
            subtitle  = "Следовать настройкам устройства",
            icon      = Icons.Outlined.PhoneAndroid,
            selected  = current == ThemeMode.SYSTEM,
            onClick   = { onSelect(ThemeMode.SYSTEM) },
        )
        ThemeOption(
            mode      = ThemeMode.LIGHT,
            label     = "Светлая",
            subtitle  = "Розовая пастель",
            icon      = Icons.Filled.LightMode,
            selected  = current == ThemeMode.LIGHT,
            onClick   = { onSelect(ThemeMode.LIGHT) },
        )
        ThemeOption(
            mode      = ThemeMode.DARK,
            label     = "Тёмная",
            subtitle  = "Глубокий розовый ночью",
            icon      = Icons.Filled.DarkMode,
            selected  = current == ThemeMode.DARK,
            onClick   = { onSelect(ThemeMode.DARK) },
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Building blocks
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = MaterialTheme.colorScheme.primary,
                    modifier           = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text  = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
            }
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

@Composable
private fun ThemeOption(
    mode: ThemeMode,
    label: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // `selectable` adds proper semantics so screen readers announce
    // "selected" / "not selected" — better than a plain clickable Row.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick  = onClick,
                role     = androidx.compose.ui.semantics.Role.RadioButton,
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text  = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        RadioButton(selected = selected, onClick = null)
    }
}

