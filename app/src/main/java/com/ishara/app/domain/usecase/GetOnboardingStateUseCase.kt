package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.OnboardingState
import com.ishara.app.domain.repository.OnboardingRepository
import kotlinx.coroutines.flow.Flow

/**
 * Evaluates and observes onboarding state.
 */
class GetOnboardingStateUseCase(
    private val onboardingRepository: OnboardingRepository
) {
    suspend operator fun invoke(): IshaaraResult<OnboardingState> {
        return onboardingRepository.getOnboardingState()
    }

    fun observe(): Flow<OnboardingState> {
        return onboardingRepository.observeOnboardingState()
    }
}
