package com.example.mycutieschedule.presentation.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Sign-up screen.
 *
 * Fields (in order, all keyboard-navigable via ImeAction.Next):
 *   1. Имя (display name) — auto-capitalize words
 *   2. Email
 *   3. Пароль  — show/hide toggle
 *   4. Повторите пароль — show/hide toggle, ImeAction.Done triggers submit
 *
 * Validation:
 *   • Client-side in [SignUpViewModel.validateLocally]: blank checks, password match,
 *     min length — shown as field-level errors so the user sees exactly what's wrong.
 *   • Server-side errors (Firebase / use-case) appear as a bottom Snackbar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: SignUpViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // FocusRequesters to chain fields with keyboard ImeAction.Next
    val emailFocus           = remember { FocusRequester() }
    val passwordFocus        = remember { FocusRequester() }
    val confirmPasswordFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessages.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Header ────────────────────────────────────────────────────────
            Text(
                text = "Создайте аккаунт",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Заполните данные ниже",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(8.dp))

            // ── Display name ──────────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = viewModel::onNameChange,
                label = { Text("Имя") },
                placeholder = { Text("Как тебя зовут?") },
                isError = uiState.nameError != null,
                supportingText = uiState.nameError?.let { error -> { Text(error) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { emailFocus.requestFocus() },
                ),
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Email ─────────────────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = { Text("Email") },
                placeholder = { Text("you@example.com") },
                isError = uiState.emailError != null,
                supportingText = uiState.emailError?.let { error -> { Text(error) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { passwordFocus.requestFocus() },
                ),
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(emailFocus),
            )

            // ── Password ──────────────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                label = { Text("Пароль") },
                visualTransformation = if (uiState.isPasswordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = viewModel::onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (uiState.isPasswordVisible)
                                Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (uiState.isPasswordVisible)
                                "Скрыть пароль" else "Показать пароль",
                        )
                    }
                },
                isError = uiState.passwordError != null,
                supportingText = uiState.passwordError?.let { error ->
                    { Text(error) }
                } ?: { Text("Не менее 6 символов", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(
                    onNext = { confirmPasswordFocus.requestFocus() },
                ),
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(passwordFocus),
            )

            // ── Confirm password ──────────────────────────────────────────────
            OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = { Text("Повторите пароль") },
                visualTransformation = if (uiState.isConfirmPasswordVisible)
                    VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = viewModel::onToggleConfirmPasswordVisibility) {
                        Icon(
                            imageVector = if (uiState.isConfirmPasswordVisible)
                                Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (uiState.isConfirmPasswordVisible)
                                "Скрыть пароль" else "Показать пароль",
                        )
                    }
                },
                isError = uiState.confirmPasswordError != null,
                supportingText = uiState.confirmPasswordError?.let { error -> { Text(error) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        viewModel.onSignUp()
                    },
                ),
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(confirmPasswordFocus),
            )

            Spacer(Modifier.height(8.dp))

            // ── Submit ────────────────────────────────────────────────────────
            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.onSignUp()
                },
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = "Зарегистрироваться",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            // ── Switch to sign-in ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Уже есть аккаунт?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onNavigateBack) {
                    Text(
                        text = "Войти",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
