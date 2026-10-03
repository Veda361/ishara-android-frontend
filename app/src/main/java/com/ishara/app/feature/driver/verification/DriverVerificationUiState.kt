package com.ishara.app.feature.driver.verification

import com.ishara.app.domain.model.DriverOnboardingState
import com.ishara.app.domain.model.DriverProfile
import com.ishara.app.domain.model.DriverVerificationDetails

/**
 * UI state for Driver Verification screen following UDF.
 */
data class DriverVerificationUiState(
    val onboardingState: DriverOnboardingState = DriverOnboardingState.Loading,
    val profile: DriverProfile? = null,
    val verificationDetails: DriverVerificationDetails? = null,
    val isRefreshing: Boolean = false,
    val isSubmitting: Boolean = false,
    val resubmissionNotes: String = "",
    val userFacingMessage: String? = null,
    val userFacingError: String? = null
)
