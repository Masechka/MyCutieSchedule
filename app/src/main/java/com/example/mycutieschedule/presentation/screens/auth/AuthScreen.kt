package com.example.mycutieschedule.presentation.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Welcome / landing screen — entry point of the auth graph.
 * Offers two paths: sign in (existing account) or sign up (new account).
 *
 * TODO Stage 7: replace placeholder with final design.
 */
@Composable
fun AuthScreen(
    onSignInClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {},
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier            = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "My Cutie Schedule 🩷", style = MaterialTheme.typography.displaySmall)
            Spacer(modifier = Modifier.height(48.dp))
            Button(onClick = onSignInClick) { Text("Sign In") }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(onClick = onSignUpClick) { Text("Create Account") }
        }
    }
}
