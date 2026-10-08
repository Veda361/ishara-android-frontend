package com.ishara.app.feature.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.auth.GoogleSignInManager
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
    private val navigationManager: NavigationManager,
    private val googleSignInManager: GoogleSignInManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signInWithGoogle(context: Context) {
        Log.d("AUTH_DEBUG", "LoginViewModel: signInWithGoogle() clicked")
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            Log.d("AUTH_DEBUG", "LoginViewModel: Requesting Google ID Token from GoogleSignInManager")
            googleSignInManager.getGoogleIdToken(context)
                .onSuccess { idToken ->
                    Log.d("AUTH_DEBUG", "LoginViewModel: Google ID Token received. Length: ${idToken.length}")
                    Log.d("AUTH_DEBUG", "LoginViewModel: Executing SignInWithGoogleUseCase")
                    signInWithGoogleUseCase(idToken)
                        .onSuccess { session ->
                            Log.d("AUTH_DEBUG", "LoginViewModel: Sign-in success. Session user: ${session.userId}, Role: ${session.role}")
                            _uiState.update { it.copy(isLoading = false) }
                            Log.d("AUTH_DEBUG", "LoginViewModel: Navigating to role destination")
                            navigationManager.navigateForRole(session.role)
                        }
                        .onFailure { error ->
                            Log.e("AUTH_DEBUG", "LoginViewModel: Better Auth sign-in failed: ${error.message}")
                            _uiState.update { it.copy(isLoading = false, error = error.message) }
                        }
                }
                .onFailure { error ->
                    Log.e("AUTH_DEBUG", "LoginViewModel: Google ID Token retrieval failed: ${error.message}")
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onOtpChanged(otp: String) {
        _uiState.update { it.copy(otp = otp) }
    }

    fun sendOtp() {
        val email = _uiState.value.email
        if (email.isBlank()) return

        Log.d("AUTH_DEBUG", "LoginViewModel: sendOtp() called for $email")
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.sendEmailOtp(email)
                .onSuccess {
                    Log.d("AUTH_DEBUG", "LoginViewModel: OTP sent successfully")
                    _uiState.update { it.copy(isLoading = false, isOtpSent = true) }
                }
                .onFailure { error ->
                    Log.e("AUTH_DEBUG", "LoginViewModel: OTP send failed: ${error.message}")
                    _uiState.update { it.copy(isLoading = false, error = error.message) }
                }
        }
    }

    fun verifyOtp() {
        val email = _uiState.value.email
        val otp = _uiState.value.otp
        if (email.isBlank() || otp.isBlank()) return

        Log.d("AUTH_DEBUG", "LoginViewModel: verifyOtp() called")
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            authRepository.signInWithEmailOtp(email, otp)
                .onSuccess { session ->
                    Log.d("AUTH_DEBUG", "LoginViewModel: Email sign-in successful. Role: ${session.role}")
                    _uiState.update { it.copy(isLoading = false) }
                    navigationManager.navigateForRole(session.role)
                }
                .onFailure { error ->
                    Log.e("AUTH_DEBUG", "LoginViewModel: Email sign-in failed: ${error.message}")
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
