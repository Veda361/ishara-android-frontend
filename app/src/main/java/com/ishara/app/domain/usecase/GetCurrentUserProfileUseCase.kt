package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.UserProfile
import com.ishara.app.domain.repository.UserRepository

/**
 * Retrieves the current authenticated user's profile.
 */
class GetCurrentUserProfileUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): IshaaraResult<UserProfile> {
        return userRepository.getCurrentUserProfile()
    }
}
