package com.ishara.app.domain.usecase

import com.ishara.app.domain.model.AuthState
import com.ishara.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case to observe the application-wide authentication state machine reactively.
 */
class ObserveAuthStateUseCase(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): StateFlow<AuthState> {
        return authRepository.observeAuthState()
    }
}
