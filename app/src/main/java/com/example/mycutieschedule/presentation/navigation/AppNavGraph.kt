package com.example.mycutieschedule.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.example.mycutieschedule.presentation.screens.auth.AuthScreen
import com.example.mycutieschedule.presentation.screens.auth.SignInScreen
import com.example.mycutieschedule.presentation.screens.auth.SignUpScreen
import com.example.mycutieschedule.presentation.screens.calendar.DayViewScreen
import com.example.mycutieschedule.presentation.screens.calendar.MonthCalendarScreen
import com.example.mycutieschedule.presentation.screens.calendar.WeekCalendarScreen
import com.example.mycutieschedule.presentation.screens.event.EventEditorScreen
import com.example.mycutieschedule.presentation.screens.profile.ProfileScreen
import com.example.mycutieschedule.presentation.screens.settings.SettingsScreen
import com.example.mycutieschedule.presentation.screens.share.SharedSchedulesScreen

/**
 * Root NavHost for the app.
 *
 * Two nested graphs handle the auth gate automatically:
 *   - [Screen.AuthGraph]  →  Auth → SignIn / SignUp
 *   - [Screen.MainGraph]  →  MonthCalendar (home) → all other screens
 *
 * Auth-driven navigation (switching between graphs) is handled externally
 * by [com.example.mycutieschedule.presentation.AppContent] via LaunchedEffect
 * on the auth state, so this composable stays purely declarative.
 *
 * Transition conventions (see [NavTransitions]):
 *   - Auth screens      → horizontal slide (standard push/pop)
 *   - Main screens      → horizontal slide
 *   - EventEditor       → slide up from bottom (modal feel)
 *   - Settings/Profile  → horizontal slide
 *   - Graph switches    → fade (less jarring than a directional slide)
 */
@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController       = navController,
        startDestination    = Screen.AuthGraph,
        // Graph-level default transitions (overridden per composable below)
        enterTransition     = NavTransitions.fadeIn,
        exitTransition      = NavTransitions.fadeOut,
        popEnterTransition  = NavTransitions.fadeIn,
        popExitTransition   = NavTransitions.fadeOut,
    ) {

        // ════════════════════════════════════════════════════════════════════
        //  AUTH GRAPH
        // ════════════════════════════════════════════════════════════════════
        navigation<Screen.AuthGraph>(startDestination = Screen.Auth) {

            composable<Screen.Auth>(
                enterTransition    = NavTransitions.slideFromRight,
                exitTransition     = NavTransitions.slideToLeft,
                popEnterTransition = NavTransitions.slideFromLeft,
                popExitTransition  = NavTransitions.slideToRight,
            ) {
                AuthScreen(
                    onSignInClick  = { navController.navigate(Screen.SignIn) },
                    onSignUpClick  = { navController.navigate(Screen.SignUp) },
                )
            }

            composable<Screen.SignIn>(
                enterTransition    = NavTransitions.slideFromRight,
                exitTransition     = NavTransitions.slideToLeft,
                popEnterTransition = NavTransitions.slideFromLeft,
                popExitTransition  = NavTransitions.slideToRight,
            ) {
                SignInScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSignUp = {
                        navController.navigate(Screen.SignUp) {
                            popUpTo<Screen.SignIn> { inclusive = true }
                        }
                    },
                )
            }

            composable<Screen.SignUp>(
                enterTransition    = NavTransitions.slideFromRight,
                exitTransition     = NavTransitions.slideToLeft,
                popEnterTransition = NavTransitions.slideFromLeft,
                popExitTransition  = NavTransitions.slideToRight,
            ) {
                SignUpScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }

        // ════════════════════════════════════════════════════════════════════
        //  MAIN GRAPH
        // ════════════════════════════════════════════════════════════════════
        navigation<Screen.MainGraph>(startDestination = Screen.MonthCalendar) {

            composable<Screen.MonthCalendar>(
                enterTransition    = NavTransitions.slideFromRight,
                exitTransition     = NavTransitions.slideToLeft,
                popEnterTransition = NavTransitions.slideFromLeft,
                popExitTransition  = NavTransitions.slideToRight,
            ) {
                MonthCalendarScreen(
                    onDayClick = { epochDay ->
                        navController.navigate(Screen.DayView(dateEpochDay = epochDay))
                    },
                    onWeekViewClick = { navController.navigate(Screen.WeekCalendar) },
                    onAddEventClick = { prefillMilli ->
                        navController.navigate(Screen.EventEditor(prefillDateEpochMilli = prefillMilli))
                    },
                    onSharedSchedulesClick = { navController.navigate(Screen.SharedSchedules) },
                    onProfileClick         = { navController.navigate(Screen.Profile) },
                    onSettingsClick        = { navController.navigate(Screen.Settings) },
                )
            }

            composable<Screen.WeekCalendar>(
                enterTransition    = NavTransitions.slideFromRight,
                exitTransition     = NavTransitions.slideToLeft,
                popEnterTransition = NavTransitions.slideFromLeft,
                popExitTransition  = NavTransitions.slideToRight,
            ) {
                WeekCalendarScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onDayClick = { epochDay ->
                        navController.navigate(Screen.DayView(dateEpochDay = epochDay))
                    },
                    onAddEventClick = { prefillMilli ->
                        navController.navigate(Screen.EventEditor(prefillDateEpochMilli = prefillMilli))
                    },
                )
            }

            composable<Screen.DayView>(
                enterTransition    = NavTransitions.slideFromRight,
                exitTransition     = NavTransitions.slideToLeft,
                popEnterTransition = NavTransitions.slideFromLeft,
                popExitTransition  = NavTransitions.slideToRight,
            ) { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.DayView>()
                DayViewScreen(
                    dateEpochDay    = args.dateEpochDay,
                    onNavigateBack  = { navController.popBackStack() },
                    onEventClick    = { eventId ->
                        navController.navigate(Screen.EventEditor(eventId = eventId))
                    },
                    onAddEventClick = { prefillMilli ->
                        navController.navigate(Screen.EventEditor(prefillDateEpochMilli = prefillMilli))
                    },
                )
            }

            composable<Screen.EventEditor>(
                // Modal: slides up from the bottom regardless of direction
                enterTransition    = NavTransitions.slideUp,
                exitTransition     = NavTransitions.slideDown,
                popEnterTransition = NavTransitions.slideUp,
                popExitTransition  = NavTransitions.slideDown,
            ) { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.EventEditor>()
                EventEditorScreen(
                    eventId              = args.eventId,
                    prefillDateEpochMilli = args.prefillDateEpochMilli,
                    onNavigateBack       = { navController.popBackStack() },
                )
            }

            composable<Screen.SharedSchedules>(
                enterTransition    = NavTransitions.slideFromRight,
                exitTransition     = NavTransitions.slideToLeft,
                popEnterTransition = NavTransitions.slideFromLeft,
                popExitTransition  = NavTransitions.slideToRight,
            ) {
                SharedSchedulesScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable<Screen.Profile>(
                enterTransition    = NavTransitions.slideFromRight,
                exitTransition     = NavTransitions.slideToLeft,
                popEnterTransition = NavTransitions.slideFromLeft,
                popExitTransition  = NavTransitions.slideToRight,
            ) {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable<Screen.Settings>(
                enterTransition    = NavTransitions.slideFromRight,
                exitTransition     = NavTransitions.slideToLeft,
                popEnterTransition = NavTransitions.slideFromLeft,
                popExitTransition  = NavTransitions.slideToRight,
            ) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }
}
