package com.ishara.app.domain.model

import com.ishara.app.core.result.IshaaraError

/**
 * Authoritative lifecycle state machine for driver profile resolution.
 * Strictly separates:
 * - Loading: Driver profile resolution is actively in progress from backend; never assume INDIVIDUAL.
 * - NeedsOperatingTypeSelection: Driver has not yet selected their operational classification.
 * - Ready: Driver profile exists with confirmed backend operatingType ("INDIVIDUAL" or "AGENCY").
 * - Error: Backend profile resolution failed. Never defaults to INDIVIDUAL.
 */
sealed interface DriverProfileState {
    object Loading : DriverProfileState

    object NeedsOperatingTypeSelection : DriverProfileState

    data class Ready(
        val profile: DriverProfile,
        val operatingType: String
    ) : DriverProfileState

    data class Error(
        val error: IshaaraError,
        val canRetry: Boolean = true
    ) : DriverProfileState
}
