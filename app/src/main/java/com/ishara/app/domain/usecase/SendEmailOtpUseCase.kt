package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.repository.AuthRepository

/**
 * Use case to request a verification OTP for email-based authentication.
 */
class SendEmailOtpUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): IshaaraResult<Unit> {
        val trimmedEmail = email.trim()
        if (trimmedEmail.isBlank() || !trimmedEmail.contains("@")) {
            return IshaaraResult.failure(
                IshaaraError.Validation("email", "Please provide a valid email address.")
            )
        }
        return authRepository.sendEmailOtp(trimmedEmail)
    }
}
