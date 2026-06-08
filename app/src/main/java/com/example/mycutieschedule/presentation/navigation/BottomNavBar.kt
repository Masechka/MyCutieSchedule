package com.example.mycutieschedule.presentation.navigation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * The three root tabs of the main app experience.
 *
 * Each tab's [route] is used as the destination key inside the inner
 * NavHost of [MainShellScreen]. Routes are intentionally simple strings
 * (not part of the outer type-safe [Screen] hierarchy) because tab
 * switching is pure bottom-nav state — it never appears in the outer
 * back stack.
 */
enum class BottomTab(
    val route: String,
    val labelRu: String,
    val icon: ImageVector,
) {
    MONTH("month", "Месяц",  Icons.Default.CalendarMonth),
    WEEK("week",   "Неделя", Icons.Default.ViewWeek),
    MORE("more",   "Ещё",    Icons.Default.MoreHoriz),
}

/**
 * Material 3 bottom navigation bar with the app's pink palette.
 *
 * Selected indicator uses [MaterialTheme.colorScheme.primaryContainer] (Blush)
 * so the active tab is clearly highlighted against the surface background.
 * Top corners are rounded (16 dp) to give the bar a card-like lift.
 */
@Composable
fun AppBottomNavigationBar(navController: NavController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute   = backStackEntry?.destination?.route

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier       = Modifier.clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
    ) {
        BottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick  = {
                    navController.navigate(tab.route) {
                        // Always pop back to the start destination so the back stack
                        // stays flat when switching tabs.
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState    = true
                    }
                },
                icon = {
                    Icon(
                        imageVector     = tab.icon,
                        contentDescription = tab.labelRu,
                    )
                },
                label = {
                    Text(
                        text  = tab.labelRu,
                        style = MaterialTheme.typography.labelSmall,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor    = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor    = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor      = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
