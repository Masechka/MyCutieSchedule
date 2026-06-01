package com.example.mycutieschedule.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.mycutieschedule.domain.model.AuthState
import com.example.mycutieschedule.domain.model.ThemeMode
import com.example.mycutieschedule.presentation.navigation.AppNavGraph
import com.example.mycutieschedule.presentation.navigation.Screen
import com.example.mycutieschedule.presentation.theme.MyCutieScheduleTheme

/**
 * Root composable. Wraps the theme and the NavHost, and reacts to auth-state
 * changes to switch between the auth graph and the main app graph.
 *
 * **Why auth navigation lives here (not inside the NavGraph):**
 * The NavGraph is purely declarative — it describes where you *can* go.
 * Who you currently *are* (authenticated or not) is state, and state-driven
 * side effects belong in a [LaunchedEffect], not in the graph definition.
 *
 * **How the LaunchedEffect works:**
 * 1. [AppViewModel.authState] is a [kotlinx.coroutines.flow.StateFlow] with an
 *    initial value computed synchronously from [FirebaseAuth.currentUser].
 * 2. On first composition [LaunchedEffect] runs immediately and navigates to the
 *    correct graph — no visible flash.
 * 3. If the user signs in or out mid-session, [authState] emits a new value,
 *    [LaunchedEffect] re-runs, and the graph switches with a fade animation.
 * 4. `popUpTo(0) { inclusive = true }` clears the entire back stack so the user
 *    can't press Back to reach the other graph after switching.
 * 5. `launchSingleTop = true` prevents stacking the same graph twice if the
 *    [LaunchedEffect] somehow fires multiple times with the same state.
 */
@Composable
fun AppContent(
    viewModel: AppViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> navController.navigate(Screen.MainGraph) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
            AuthState.Unauthenticated  -> navController.navigate(Screen.AuthGraph) {
                popUpTo(0) { inclusive = true }
                launchSingleTop = true
            }
        }
    }

    // Resolve user preference → concrete dark/light flag. SYSTEM defers to the
    // device setting; LIGHT/DARK override. Reading isSystemInDarkTheme() inside
    // the composable means the theme reacts live to OS dark-mode toggles too.
    val systemDark = isSystemInDarkTheme()
    val useDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }

    MyCutieScheduleTheme(darkTheme = useDarkTheme) {
        AppNavGraph(navController = navController)
    }
}
