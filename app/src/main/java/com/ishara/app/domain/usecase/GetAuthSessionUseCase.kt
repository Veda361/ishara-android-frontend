package com.ishara.app.domain.usecase

import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use case to observe the current active user authentication session.
 */
class GetAuthSessionUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<AuthSession?> {
        return authRepository.observeSession()
    }
}
