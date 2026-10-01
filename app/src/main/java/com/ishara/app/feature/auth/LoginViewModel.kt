package com.ishara.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.UiState
import com.ishara.app.domain.usecase.SignInWithGoogleUseCase
import com.ishara.app.domain.repository.AuthRepository
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val authRepository: AuthRepository,
    private val navigationManager: NavigationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onOtpChanged(otp: String) {
        _uiState.update { it.copy(otp = otp) }
    }

    fun sendOtp() {
        val email = _uiState.value.email
        if (email.isBlank()) return

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.sendEmailOtp(email)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, isOtpSent = true) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun verifyOtp() {
        val email = _uiState.value.email
        val otp = _uiState.value.otp
        if (email.isBlank() || otp.isBlank()) return

        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.signInWithEmailOtp(email, otp)
                .onSuccess { session ->
                    _uiState.update { it.copy(isLoading = false) }
                    navigationManager.navigateForRole(session.role)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            signInWithGoogleUseCase(idToken)
                .onSuccess { session ->
                    _uiState.update { it.copy(isLoading = false) }
                    navigationManager.navigateForRole(session.role)
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }
}

data class LoginUiState(
    val email: String = "",
    val otp: String = "",
    val isLoading: Boolean = false,
    val isOtpSent: Boolean = false,
    val error: String? = null
)
