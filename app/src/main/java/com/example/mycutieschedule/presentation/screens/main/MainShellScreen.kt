package com.example.mycutieschedule.presentation.screens.main

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mycutieschedule.presentation.navigation.AppBottomNavigationBar
import com.example.mycutieschedule.presentation.navigation.BottomTab
import com.example.mycutieschedule.presentation.screens.calendar.MonthCalendarScreen
import com.example.mycutieschedule.presentation.screens.calendar.WeekCalendarScreen
import com.example.mycutieschedule.presentation.screens.more.MoreScreen

/**
 * Authenticated app shell: Scaffold with a bottom navigation bar + inner NavHost.
 *
 * ┌──────────────────────────────────────────┐
 * │  Tab content (Month / Week / More)       │
 * │  (fills the area above the nav bar)      │
 * ├──────────────────────────────────────────┤
 * │  AppBottomNavigationBar                  │
 * └──────────────────────────────────────────┘
 *
 * ## Two NavController pattern
 * Full-page destinations (DayView, EventEditor, Profile, Settings, SharedSchedules)
 * live in the **outer** AppNavGraph and are reached via the callback lambdas below.
 * The **inner** NavController only manages tab switching within this shell.
 *
 * ## Inset handling (nested Scaffold)
 * [Modifier.consumeWindowInsets] is applied to the inner NavHost so that screens
 * with their own Scaffold (e.g. WeekCalendarScreen) don't double-count the status-
 * bar or system-navigation-bar insets that the outer Scaffold already consumed.
 */
@Composable
fun MainShellScreen(
    onDayClick: (Long) -> Unit = {},
    onAddEventClick: (Long?) -> Unit = {},
    onEventClick: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onSharedSchedulesClick: () -> Unit = {},
) {
    val innerNavController = rememberNavController()

    Scaffold(
        bottomBar = {
            AppBottomNavigationBar(navController = innerNavController)
        },
    ) { innerPadding ->
        NavHost(
            navController    = innerNavController,
            startDestination = BottomTab.MONTH.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            // ── Месяц ─────────────────────────────────────────────────────────
            composable(BottomTab.MONTH.route) {
                MonthCalendarScreen(
                    onDayClick             = onDayClick,
                    onWeekViewClick        = { innerNavController.navigate(BottomTab.WEEK.route) },
                    onAddEventClick        = onAddEventClick,
                    onSharedSchedulesClick = onSharedSchedulesClick,
                    onProfileClick         = onProfileClick,
                    onSettingsClick        = onSettingsClick,
                )
            }

            // ── Неделя ────────────────────────────────────────────────────────
            composable(BottomTab.WEEK.route) {
                WeekCalendarScreen(
                    onNavigateBack  = {},   // root tab — back arrow is a no-op here
                    onDayClick      = onDayClick,
                    onAddEventClick = onAddEventClick,
                    onEventClick    = onEventClick,
                )
            }

            // ── Ещё ───────────────────────────────────────────────────────────
            composable(BottomTab.MORE.route) {
                MoreScreen(
                    onProfileClick         = onProfileClick,
                    onSettingsClick        = onSettingsClick,
                    onSharedSchedulesClick = onSharedSchedulesClick,
                )
            }
        }
    }
}
