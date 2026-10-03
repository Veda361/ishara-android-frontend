package com.ishara.app.domain.usecase

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.domain.model.ApplicationDestination
import com.ishara.app.domain.model.UserProfile
import com.ishara.app.domain.model.UserRole

/**
 * Centralized policy to resolve application destination from a UserProfile.
 * Prevents destination logic from scattering across ViewModels or UI composables.
 */
class ResolveApplicationDestinationUseCase {

    operator fun invoke(userProfile: UserProfile?): ApplicationDestination {
        if (userProfile == null) {
            return ApplicationDestination.Authentication
        }

        // If user has not onboarded or has no role, onboarding is strictly required
        if (!userProfile.isOnboarded || userProfile.role == null) {
            return ApplicationDestination.Onboarding
        }

        return when (userProfile.role) {
            UserRole.USER, UserRole.ADMIN -> ApplicationDestination.StudentHome
            UserRole.DRIVER_CONDUCTOR -> ApplicationDestination.DriverDashboard
        }
    }
}
