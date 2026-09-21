package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.repository.AuthRepository

/**
 * Use case to sign out the active user and invalidate session.
 */
class SignOutUseCase(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): IshaaraResult<Unit> {
        return authRepository.signOut()
    }
}
