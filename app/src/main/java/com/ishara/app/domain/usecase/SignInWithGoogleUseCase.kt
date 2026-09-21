package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.repository.AuthRepository

/**
 * Use case to authenticate via Google ID token.
 */
class SignInWithGoogleUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(idToken: String): IshaaraResult<AuthSession> {
        if (idToken.isBlank()) {
            return IshaaraResult.failure(
                IshaaraError.Validation("idToken", "Google ID token must not be empty.")
            )
        }
        return authRepository.signInWithGoogle(idToken)
    }
}
