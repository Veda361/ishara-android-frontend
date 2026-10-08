package com.ishara.app.domain.usecase

import android.util.Log
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
        Log.d("AUTH_DEBUG", "SignInWithGoogleUseCase: invoke() called")
        if (idToken.isBlank()) {
            Log.e("AUTH_DEBUG", "SignInWithGoogleUseCase: idToken is blank")
            return IshaaraResult.failure(
                IshaaraError.Validation("idToken", "Google ID token must not be empty.")
            )
        }
        return authRepository.signInWithGoogle(idToken).onSuccess {
            Log.d("AUTH_DEBUG", "SignInWithGoogleUseCase: Success")
        }.onFailure {
            Log.e("AUTH_DEBUG", "SignInWithGoogleUseCase: Failure - ${it.message}")
        }
    }
}
