package com.example.mycutieschedule.presentation.screens.more

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mycutieschedule.domain.usecase.auth.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for [MoreScreen].
 *
 * Responsibilities:
 *   - Delegate logout to [LogoutUseCase].
 *
 * Navigation after logout is handled automatically:
 * Firebase sign-out → [AuthRepository.observeAuthState] emits [AuthState.Unauthenticated]
 * → [AppViewModel.authState] updates → [AppContent.LaunchedEffect] navigates to AuthGraph.
 * This ViewModel does NOT navigate directly.
 */
@HiltViewModel
class MoreViewModel @Inject constructor(
    private val logoutUseCase: LogoutUseCase,
) : ViewModel() {

    fun logout() {
        viewModelScope.launch { logoutUseCase() }
    }
}
