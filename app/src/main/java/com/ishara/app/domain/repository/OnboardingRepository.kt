package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.OnboardingState
import com.ishara.app.domain.model.UserProfile
import com.ishara.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for Onboarding state and role assignment.
 * Decoupled from AuthRepository.
 */
interface OnboardingRepository {
    /**
     * Evaluates current user profile to determine if onboarding is required.
     */
    suspend fun getOnboardingState(): IshaaraResult<OnboardingState>

    /**
     * Submits one-time role assignment to POST /api/v1/users/me/onboarding.
     */
    suspend fun submitOnboarding(role: UserRole): IshaaraResult<UserProfile>

    /**
     * Observes onboarding state reactively.
     */
    fun observeOnboardingState(): Flow<OnboardingState>

    /**
     * Resets onboarding state (e.g. on user logout).
     */
    suspend fun resetOnboardingState()
}
