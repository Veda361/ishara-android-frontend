package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.repository.AuthRepository

/**
 * Use case to restore persisted session on app launch or process recreation.
 */
class RestoreSessionUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): IshaaraResult<AuthSession?> {
        return authRepository.restoreSession()
    }
}
