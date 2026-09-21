package com.ishara.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.UiState
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.usecase.GetAuthSessionUseCase
import com.ishara.app.domain.usecase.SignInWithGoogleUseCase
import com.ishara.app.domain.usecase.SignOutUseCase
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Production ViewModel managing authentication lifecycle and role-based session state.
 */
class AuthViewModel(
    private val getAuthSessionUseCase: GetAuthSessionUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val navigationManager: NavigationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            getAuthSessionUseCase().collect { session ->
                _uiState.update { currentState ->
                    currentState.copy(
                        session = session,
                        role = session?.role
                    )
                }
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _uiState.update { it.copy(operationState = UiState.Loading) }
        viewModelScope.launch {
            signInWithGoogleUseCase(idToken)
                .onSuccess { session ->
                    _uiState.update { it.copy(operationState = UiState.Success(Unit)) }
                    navigationManager.navigateForRole(session.role)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(operationState = UiState.Error(error)) }
                }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            signOutUseCase()
            navigationManager.navigate("auth/login", popUpToRoute = "auth/splash", inclusive = true)
        }
    }
}
