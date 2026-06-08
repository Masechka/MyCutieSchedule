package com.example.mycutieschedule

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.mycutieschedule.presentation.AppContent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * Request handle for the POST_NOTIFICATIONS runtime permission (Android 13+).
     * Result is intentionally ignored — denial is non-fatal, the app still works
     * but reminders silently no-op until the user grants the permission via
     * system Settings.
     */
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ignore */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        maybeRequestNotificationPermission()
        setContent {
            AppContent()
        }
    }

    /**
     * On Android 13+ apps must request POST_NOTIFICATIONS at runtime to show
     * any notification. We ask on first launch; if the user denies we don't
     * pester them again — they can re-enable from system Settings.
     *
     * Pre-13 devices auto-grant via the manifest declaration, so no prompt
     * is shown.
     */
    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ActivityCompat.checkSelfPermission(
            this, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
