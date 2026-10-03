package com.ishara.app.feature.auth

import com.ishara.app.core.common.UiState
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.AuthState
import com.ishara.app.domain.model.UserRole

enum class AuthMethod {
    GOOGLE, EMAIL_OTP
}

/**
 * UI State for the Authentication feature.
 * Consolidates all authentication and session flags into a single, predictable state model.
 */
data class AuthUiState(
    val authState: AuthState = AuthState.Unknown,
    val isAuthenticating: Boolean = false,
    val errorMessage: String? = null,
    val sessionExpiredMessage: String? = null,
    val operationState: UiState<Unit> = UiState.Idle,
    val session: AuthSession? = null,
    val role: UserRole? = null,
    val authMethod: AuthMethod = AuthMethod.GOOGLE,
    val emailInput: String = "",
    val otpInput: String = "",
    val isOtpSent: Boolean = false,
    val isSendingOtp: Boolean = false,
    val isVerifyingOtp: Boolean = false
) {
    val isAuthenticated: Boolean
        get() = authState is AuthState.Authenticated || (session != null && !session.isExpired)

    val isSessionExpired: Boolean
        get() = authState is AuthState.SessionExpired || sessionExpiredMessage != null

    val canInitiateSignIn: Boolean
        get() = !isAuthenticating && !isSendingOtp && !isVerifyingOtp

    val canSendOtp: Boolean
        get() = emailInput.isNotBlank() && emailInput.contains("@") && !isSendingOtp && !isVerifyingOtp && !isAuthenticating

    val canVerifyOtp: Boolean
        get() = otpInput.length == 6 && !isSendingOtp && !isVerifyingOtp && !isAuthenticating
}
