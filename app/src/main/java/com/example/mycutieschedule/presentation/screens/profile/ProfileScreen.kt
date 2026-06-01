package com.example.mycutieschedule.presentation.screens.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.mycutieschedule.domain.model.User

/**
 * Profile screen — read-only account info + logout.
 *
 * Account data comes from Firebase Auth's cached `currentUser`; no network
 * is required to display it. Logout goes through [ProfileViewModel] →
 * `LogoutUseCase` → Firebase Auth. The root [com.example.mycutieschedule.presentation.AppContent]
 * observes auth state and routes to the auth graph automatically, so we just
 * pop back when the one-shot logged-out event fires.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                ProfileEvent.LoggedOut       -> onNavigateBack()  // AppContent reroutes the graph
                is ProfileEvent.ShowError    -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData    = data,
                    containerColor  = MaterialTheme.colorScheme.inverseSurface,
                    contentColor    = MaterialTheme.colorScheme.inverseOnSurface,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val user = uiState.user
            if (user != null) {
                ProfileHeader(user = user)
                AccountInfoCard(user = user)
            } else {
                // Edge case: brief gap during logout, or unexpected null.
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Logout button ────────────────────────────────────────────────
            Button(
                onClick = viewModel::requestLogout,
                enabled = !uiState.isLoggingOut,
                colors  = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor   = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (uiState.isLoggingOut) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Выход…")
                } else {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null,
                        modifier           = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Выйти из аккаунта")
                }
            }
        }
    }

    if (uiState.showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = viewModel::dismissLogoutConfirm,
            title            = { Text("Выйти из аккаунта?") },
            text = {
                Text(
                    "Вы вернётесь к экрану входа. Локально сохранённые расписания " +
                        "и события останутся на устройстве."
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmLogout) {
                    Text("Выйти", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissLogoutConfirm) {
                    Text("Отмена")
                }
            },
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
//  Sub-composables
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProfileHeader(user: User) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            // Initial-letter avatar. Cheap, deterministic, no asset needed.
            Text(
                text  = user.displayName.firstOrNull()?.uppercase()
                    ?: user.email.firstOrNull()?.uppercase()
                    ?: "?",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text  = user.displayName.ifBlank { "Без имени" },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        )
        if (user.email.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text  = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AccountInfoCard(user: User) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            InfoRow(
                icon  = Icons.Outlined.Person,
                label = "Имя",
                value = user.displayName.ifBlank { "—" },
            )
            InfoRow(
                icon  = Icons.Outlined.Email,
                label = "Email",
                value = user.email.ifBlank { "—" },
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            tint               = MaterialTheme.colorScheme.primary,
            modifier           = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text  = value,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
