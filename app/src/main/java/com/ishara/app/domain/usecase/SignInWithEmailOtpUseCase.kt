package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.repository.AuthRepository

/**
 * Use case to verify email OTP and sign in.
 */
class SignInWithEmailOtpUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, otp: String): IshaaraResult<AuthSession> {
        val trimmedEmail = email.trim()
        val trimmedOtp = otp.trim()

        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            return IshaaraResult.failure(
                IshaaraError.Validation("email", "Please provide a valid email address.")
            )
        }
        if (trimmedOtp.length != 6 || !trimmedOtp.all { it.isDigit() }) {
            return IshaaraResult.failure(
                IshaaraError.Validation("otp", "OTP code must be exactly 6 digits.")
            )
        }
        return authRepository.signInWithEmailOtp(trimmedEmail, trimmedOtp)
    }
}
