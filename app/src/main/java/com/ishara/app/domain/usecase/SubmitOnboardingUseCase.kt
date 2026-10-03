package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.UserProfile
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.repository.OnboardingRepository

/**
 * Submits role selection to complete user onboarding.
 */
class SubmitOnboardingUseCase(
    private val onboardingRepository: OnboardingRepository
) {
    suspend operator fun invoke(role: UserRole): IshaaraResult<UserProfile> {
        return onboardingRepository.submitOnboarding(role)
    }
}
