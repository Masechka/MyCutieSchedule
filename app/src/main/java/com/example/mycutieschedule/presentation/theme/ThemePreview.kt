package com.example.mycutieschedule.presentation.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp

// ── Preview parameter ─────────────────────────────────────────────────────────

private class DarkThemeProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

// ── Main theme showcase ───────────────────────────────────────────────────────

@Preview(name = "Theme — Light", showBackground = true, widthDp = 380)
@Preview(name = "Theme — Dark",  showBackground = true, widthDp = 380)
@Composable
private fun ThemeShowcasePreview(
    @PreviewParameter(DarkThemeProvider::class) darkTheme: Boolean,
) {
    MyCutieScheduleTheme(darkTheme = darkTheme) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(MaterialTheme.spacing.medium),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.large),
            ) {
                SectionHeader(if (darkTheme) "Dark Theme" else "Light Theme")

                ColorPaletteSection()
                HorizontalDivider()
                TypographySection()
                HorizontalDivider()
                EventColorsSection()
                HorizontalDivider()
                ComponentsSection()
                HorizontalDivider()
                SpacingSection()
                HorizontalDivider()
                ShapesSection()
            }
        }
    }
}

// ── Sections ──────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun SubSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPaletteSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SubSectionLabel("Color Roles")

        val roles = listOf(
            "primary"          to MaterialTheme.colorScheme.primary,
            "primaryContainer" to MaterialTheme.colorScheme.primaryContainer,
            "secondary"        to MaterialTheme.colorScheme.secondary,
            "secondaryContainer" to MaterialTheme.colorScheme.secondaryContainer,
            "tertiary"         to MaterialTheme.colorScheme.tertiary,
            "tertiaryContainer" to MaterialTheme.colorScheme.tertiaryContainer,
            "background"       to MaterialTheme.colorScheme.background,
            "surface"          to MaterialTheme.colorScheme.surface,
            "surfaceVariant"   to MaterialTheme.colorScheme.surfaceVariant,
            "error"            to MaterialTheme.colorScheme.error,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            roles.forEach { (name, color) ->
                ColorChip(name = name, color = color)
            }
        }
    }
}

@Composable
private fun ColorChip(name: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small)
                .background(color)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = MaterialTheme.shapes.small,
                )
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun TypographySection() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SubSectionLabel("Typography")

        val styles = listOf(
            "Display Small"     to MaterialTheme.typography.displaySmall,
            "Headline Medium"   to MaterialTheme.typography.headlineMedium,
            "Headline Small"    to MaterialTheme.typography.headlineSmall,
            "Title Large"       to MaterialTheme.typography.titleLarge,
            "Title Medium"      to MaterialTheme.typography.titleMedium,
            "Body Large"        to MaterialTheme.typography.bodyLarge,
            "Body Medium"       to MaterialTheme.typography.bodyMedium,
            "Label Large"       to MaterialTheme.typography.labelLarge,
            "Label Medium"      to MaterialTheme.typography.labelMedium,
            "Label Small"       to MaterialTheme.typography.labelSmall,
        )

        styles.forEach { (name, style) ->
            Text(
                text = name,
                style = style,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EventColorsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SubSectionLabel("Event Colors (predefined shades)")

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            val eventLabels = listOf(
                "Peony", "Rose", "Raspberry", "Coral",
                "Blossom", "Mauve", "Lilac", "Sakura",
            )
            MaterialTheme.extended.eventColors.zip(eventLabels).forEach { (color, label) ->
                ColorChip(name = label, color = color)
            }
        }

        SubSectionLabel("Imported Schedule Color")
        ColorChip(name = "Imported", color = MaterialTheme.extended.eventImported)
    }
}

@Composable
private fun ComponentsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SubSectionLabel("Buttons & FAB")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {}) { Text("Filled") }
            FilledTonalButton(onClick = {}) { Text("Tonal") }
            OutlinedButton(onClick = {}) { Text("Outlined") }
            TextButton(onClick = {}) { Text("Text") }
        }

        FloatingActionButton(
            onClick = {},
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }

        SubSectionLabel("Card")
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = MaterialTheme.elevation.card),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(modifier = Modifier.padding(MaterialTheme.spacing.medium)) {
                Text("Встреча с командой", style = MaterialTheme.typography.titleMedium)
                Text("10:00 — 11:30", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SpacingSection() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SubSectionLabel("Spacing Tokens")

        val tokens = listOf(
            "extraSmall 4dp"  to MaterialTheme.spacing.extraSmall,
            "small 8dp"       to MaterialTheme.spacing.small,
            "medium 16dp"     to MaterialTheme.spacing.medium,
            "large 24dp"      to MaterialTheme.spacing.large,
            "extraLarge 32dp" to MaterialTheme.spacing.extraLarge,
            "huge 48dp"       to MaterialTheme.spacing.huge,
        )

        tokens.forEach { (label, size) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(size)
                        .height(16.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            shape = MaterialTheme.shapes.extraSmall,
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShapesSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SubSectionLabel("Shapes (Corner Radius)")

        val shapes = listOf(
            "extraSmall 8dp"  to MaterialTheme.shapes.extraSmall,
            "small 12dp"      to MaterialTheme.shapes.small,
            "medium 16dp"     to MaterialTheme.shapes.medium,
            "large 24dp"      to MaterialTheme.shapes.large,
            "extraLarge 32dp" to MaterialTheme.shapes.extraLarge,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            shapes.forEach { (label, shape) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(shape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(label, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
