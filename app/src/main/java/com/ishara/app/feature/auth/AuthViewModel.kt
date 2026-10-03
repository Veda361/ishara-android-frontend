package com.ishara.app.feature.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.auth.DefaultGoogleAuthClient
import com.ishara.app.core.auth.GoogleAuthClient
import com.ishara.app.core.auth.GoogleAuthResult
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.common.UiState
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.domain.model.AuthState
import com.ishara.app.domain.usecase.GetAuthSessionUseCase
import com.ishara.app.domain.usecase.ObserveAuthStateUseCase
import com.ishara.app.domain.usecase.RestoreSessionUseCase
import com.ishara.app.domain.usecase.SignInWithGoogleUseCase
import com.ishara.app.domain.usecase.SignOutUseCase
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Production ViewModel managing authentication lifecycle, session restoration,
 * and role-based navigation gating.
 */
class AuthViewModel(
    private val getAuthSessionUseCase: GetAuthSessionUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val navigationManager: NavigationManager,
    private val restoreSessionUseCase: RestoreSessionUseCase? = null,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase? = null,
    private val googleAuthClient: GoogleAuthClient = DefaultGoogleAuthClient(),
    private val sendEmailOtpUseCase: com.ishara.app.domain.usecase.SendEmailOtpUseCase? = null,
    private val signInWithEmailOtpUseCase: com.ishara.app.domain.usecase.SignInWithEmailOtpUseCase? = null,
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val effectiveScope: CoroutineScope = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeAuthenticationState()
        restoreInitialSession()
    }

    private fun observeAuthenticationState() {
        if (observeAuthStateUseCase != null) {
            effectiveScope.launch {
                observeAuthStateUseCase().collect { authState ->
                    _uiState.update { currentState ->
                        when (authState) {
                            is AuthState.Authenticated -> {
                                currentState.copy(
                                    authState = authState,
                                    session = authState.session,
                                    role = authState.session.role,
                                    isAuthenticating = false,
                                    errorMessage = null,
                                    sessionExpiredMessage = null
                                )
                            }
                            is AuthState.SessionExpired -> {
                                currentState.copy(
                                    authState = authState,
                                    session = null,
                                    role = null,
                                    isAuthenticating = false,
                                    sessionExpiredMessage = authState.message
                                )
                            }
                            is AuthState.Unauthenticated -> {
                                currentState.copy(
                                    authState = authState,
                                    session = null,
                                    role = null,
                                    isAuthenticating = currentState.isAuthenticating
                                )
                            }
                            is AuthState.Error -> {
                                currentState.copy(
                                    authState = authState,
                                    isAuthenticating = false,
                                    errorMessage = mapErrorToUserMessage(authState.error)
                                )
                            }
                            is AuthState.Unknown -> {
                                currentState.copy(authState = authState)
                            }
                        }
                    }
                }
            }
        } else {
            // Backward compatibility fallback to observe session
            effectiveScope.launch {
                getAuthSessionUseCase().collect { session ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            session = session,
                            role = session?.role,
                            authState = if (session != null && !session.isExpired) {
                                AuthState.Authenticated(session)
                            } else {
                                AuthState.Unauthenticated
                            }
                        )
                    }
                }
            }
        }
    }

    private fun restoreInitialSession() {
        effectiveScope.launch {
            if (restoreSessionUseCase != null) {
                IshaaraLogger.d(TAG, "Restoring persisted session on startup...")
                restoreSessionUseCase()
            }
        }
    }

    /**
     * Initiates Google Sign-In with duplicate tap protection.
     */
    fun onContinueWithGoogleClicked(context: Context) {
        if (_uiState.value.isAuthenticating) {
            IshaaraLogger.d(TAG, "Sign-in already in progress; duplicate tap ignored.")
            return
        }

        _uiState.update {
            it.copy(
                isAuthenticating = true,
                errorMessage = null,
                sessionExpiredMessage = null
            )
        }

        effectiveScope.launch {
            when (val result = googleAuthClient.signIn(context)) {
                is GoogleAuthResult.Success -> {
                    executeBackendSignIn(result.idToken)
                }
                is GoogleAuthResult.Cancelled -> {
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            errorMessage = "Sign-in was cancelled."
                        )
                    }
                }
                is GoogleAuthResult.ConfigurationMissing -> {
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            errorMessage = result.message
                        )
                    }
                }
                is GoogleAuthResult.NoCredentialsAvailable -> {
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            errorMessage = "No Google account found on this device."
                        )
                    }
                }
                is GoogleAuthResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            errorMessage = mapErrorToUserMessage(result.error)
                        )
                    }
                }
            }
        }
    }

    /**
     * Executes backend authentication via Google ID token.
     */
    fun signInWithGoogle(idToken: String) {
        if (_uiState.value.isAuthenticating) {
            return
        }
        executeBackendSignIn(idToken)
    }

    private fun executeBackendSignIn(idToken: String) {
        _uiState.update {
            it.copy(
                isAuthenticating = true,
                operationState = UiState.Loading,
                errorMessage = null,
                sessionExpiredMessage = null
            )
        }

        effectiveScope.launch {
            signInWithGoogleUseCase(idToken)
                .onSuccess { session ->
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            operationState = UiState.Success(Unit),
                            session = session,
                            role = session.role
                        )
                    }
                    session.role?.let { navigationManager.navigateForRole(it) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            operationState = UiState.Error(error),
                            errorMessage = mapErrorToUserMessage(error)
                        )
                    }
                }
        }
    }

    fun selectAuthMethod(method: AuthMethod) {
        _uiState.update { it.copy(authMethod = method, errorMessage = null) }
    }

    fun onEmailInputChanged(email: String) {
        _uiState.update { it.copy(emailInput = email, errorMessage = null) }
    }

    fun onOtpInputChanged(otp: String) {
        if (otp.length <= 6 && otp.all { it.isDigit() }) {
            _uiState.update { it.copy(otpInput = otp, errorMessage = null) }
        }
    }

    fun sendVerificationOtp() {
        val email = _uiState.value.emailInput.trim()
        if (email.isBlank() || !email.contains("@")) {
            _uiState.update { it.copy(errorMessage = "Please enter a valid email address.") }
            return
        }
        if (_uiState.value.isSendingOtp) return

        _uiState.update { it.copy(isSendingOtp = true, errorMessage = null) }
        effectiveScope.launch {
            if (sendEmailOtpUseCase != null) {
                sendEmailOtpUseCase(email)
                    .onSuccess {
                        _uiState.update {
                            it.copy(
                                isSendingOtp = false,
                                isOtpSent = true,
                                errorMessage = null
                            )
                        }
                        IshaaraLogger.i(TAG, "OTP sent successfully to email")
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isSendingOtp = false,
                                errorMessage = mapErrorToUserMessage(error)
                            )
                        }
                    }
            } else {
                _uiState.update {
                    it.copy(
                        isSendingOtp = false,
                        errorMessage = "Email OTP authentication service is not configured."
                    )
                }
            }
        }
    }

    fun verifyEmailOtp() {
        val email = _uiState.value.emailInput.trim()
        val otp = _uiState.value.otpInput.trim()
        if (otp.length != 6) {
            _uiState.update { it.copy(errorMessage = "Please enter the 6-digit OTP code.") }
            return
        }
        if (_uiState.value.isVerifyingOtp) return

        _uiState.update {
            it.copy(
                isVerifyingOtp = true,
                isAuthenticating = true,
                errorMessage = null
            )
        }

        effectiveScope.launch {
            if (signInWithEmailOtpUseCase != null) {
                signInWithEmailOtpUseCase(email, otp)
                    .onSuccess { session ->
                        _uiState.update {
                            it.copy(
                                isVerifyingOtp = false,
                                isAuthenticating = false,
                                session = session,
                                role = session.role
                            )
                        }
                        session.role?.let { navigationManager.navigateForRole(it) }
                    }
                    .onFailure { error ->
                        _uiState.update {
                            it.copy(
                                isVerifyingOtp = false,
                                isAuthenticating = false,
                                errorMessage = mapErrorToUserMessage(error)
                            )
                        }
                    }
            } else {
                _uiState.update {
                    it.copy(
                        isVerifyingOtp = false,
                        isAuthenticating = false,
                        errorMessage = "Email OTP authentication service is not configured."
                    )
                }
            }
        }
    }

    /**
     * Signs out the user, resets state, and clears navigation backstack.
     */
    fun signOut() {
        effectiveScope.launch {
            signOutUseCase()
            _uiState.update {
                it.copy(
                    session = null,
                    role = null,
                    authState = AuthState.Unauthenticated,
                    isAuthenticating = false,
                    isSendingOtp = false,
                    isVerifyingOtp = false,
                    isOtpSent = false,
                    emailInput = "",
                    otpInput = "",
                    errorMessage = null,
                    sessionExpiredMessage = null
                )
            }
            navigationManager.navigate("auth/login", popUpToRoute = "auth/login", inclusive = true)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, sessionExpiredMessage = null) }
    }

    private fun mapErrorToUserMessage(error: IshaaraError): String {
        val msg = error.message
        return when {
            msg.contains("INVALID_OTP", ignoreCase = true) || msg.contains("invalid otp", ignoreCase = true) ->
                "Invalid OTP code. Please check and try again."
            msg.contains("EXPIRED_OTP", ignoreCase = true) || msg.contains("expired", ignoreCase = true) ->
                "OTP code has expired. Please request a new code."
            msg.contains("RATE_LIMIT", ignoreCase = true) || error is IshaaraError.RateLimited ->
                "Too many requests. Please wait a moment before trying again."
            error is IshaaraError.Conflict ->
                "An account conflict occurred. Please try signing in again."
            error is IshaaraError.Network ->
                "You're offline. Check your internet connection."
            error is IshaaraError.Timeout ->
                "Connection timed out. Please try again."
            error is IshaaraError.Authentication ->
                error.message.ifBlank { "Sign-in could not be completed." }
            error is IshaaraError.NotFound ->
                "Authentication service endpoint not found."
            error is IshaaraError.Server ->
                "Server is temporarily unavailable. Please try again shortly."
            else ->
                error.message.ifBlank { "An unexpected error occurred. Please try again." }
        }
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }
}
