package com.ishara.app.feature.driver.onboarding

import com.ishara.app.domain.model.DriverProfile

/**
 * Explicit operational classification choices for driver onboarding.
 * Strictly avoids silent defaulting to INDIVIDUAL.
 */
enum class DriverOperatingTypeChoice {
    NONE,
    INDIVIDUAL,
    AGENCY
}

/**
 * UI state for Driver Onboarding (profile creation) flow.
 */
data class DriverOnboardingUiState(
    val licenseNumber: String = "",
    val yearsOfExperience: String = "",
    val emergencyContactName: String = "",
    val emergencyContactPhone: String = "",
    val emergencyContactRelationship: String = "",
    val selectedOperatingType: DriverOperatingTypeChoice = DriverOperatingTypeChoice.NONE,
    val isLoading: Boolean = false,
    val licenseNumberError: String? = null,
    val yearsOfExperienceError: String? = null,
    val emergencyPhoneError: String? = null,
    val operatingTypeError: String? = null,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val createdProfile: DriverProfile? = null
) {
    val operatingTypeString: String
        get() = when (selectedOperatingType) {
            DriverOperatingTypeChoice.INDIVIDUAL -> "INDIVIDUAL"
            DriverOperatingTypeChoice.AGENCY -> "AGENCY"
            DriverOperatingTypeChoice.NONE -> ""
        }

    /**
     * Backward-compatibility accessor for tests and UI labels.
     */
    val operatingType: String
        get() = operatingTypeString

    val isOperatingTypeSelected: Boolean
        get() = selectedOperatingType != DriverOperatingTypeChoice.NONE

    val isFormValid: Boolean
        get() = licenseNumber.trim().length in 3..30 &&
                licenseNumberError == null &&
                yearsOfExperienceError == null &&
                emergencyPhoneError == null &&
                isOperatingTypeSelected &&
                !isLoading
}
