package com.ishara.app.domain.model

import com.ishara.app.core.result.IshaaraError

/**
 * Emergency contact for driver operations and regulatory compliance.
 * Validated against backend emergencyContactInputSchema.
 */
data class DriverEmergencyContact(
    val name: String,
    val phoneNumber: String,
    val relationship: String? = null
)

/**
 * Full DriverProfile domain model representing the authoritative backend driver document.
 * Aligned with CleanDriverProfileResponse from GET/POST/PATCH /api/v1/drivers/me.
 */
data class DriverProfile(
    val id: String,
    val userId: String,
    val verificationStatus: DriverVerificationStatus,
    val status: DriverProfileStatus,
    val licenseNumberMasked: String?,
    val licenseVerifiedAt: String? = null,
    val submittedAt: String? = null,
    val reviewedAt: String? = null,
    val reviewedBy: String? = null,
    val rejectionReason: String? = null,
    val yearsOfExperience: Int? = null,
    val emergencyContact: DriverEmergencyContact? = null,
    val operatingType: String = "",
    val isSuspended: Boolean = false,
    val suspensionReason: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
) {
    val isVerified: Boolean get() = verificationStatus == DriverVerificationStatus.VERIFIED && !isSuspended
    val isPending: Boolean get() = verificationStatus == DriverVerificationStatus.PENDING && !isSuspended
    val isRejected: Boolean get() = verificationStatus == DriverVerificationStatus.REJECTED && !isSuspended
    val isAgency: Boolean get() = operatingType.equals("AGENCY", ignoreCase = true)
    val isIndividual: Boolean get() = operatingType.equals("INDIVIDUAL", ignoreCase = true)
    val hasOperatingType: Boolean get() = isAgency || isIndividual
}

/**
 * Detailed verification response from GET/POST /api/v1/drivers/me/verification.
 * Aligned strictly with CleanDriverVerificationResponse.
 */
data class DriverVerificationDetails(
    val driverId: String,
    val userId: String,
    val verificationStatus: DriverVerificationStatus,
    val submittedAt: String?,
    val reviewedAt: String?,
    val reviewedBy: String?,
    val rejectionReason: String?,
    val licenseNumberMasked: String?,
    val operatingType: String = "",
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * Authoritative state machine for Driver Onboarding and Verification.
 * Strictly separates identity, role, onboarding, verification, and operational readiness.
 */
sealed interface DriverOnboardingState {
    /** Evaluating driver onboarding state from authoritative backend */
    object Loading : DriverOnboardingState

    /** Driver profile does not exist on backend (404 DRIVER_PROFILE_NOT_FOUND). Must complete onboarding. */
    object NeedsOnboarding : DriverOnboardingState

    /** Driver needs to select their operating type before continuing onboarding */
    object NeedsOperatingTypeSelection : DriverOnboardingState

    /** Submission of driver onboarding form is currently in progress */
    object Submitting : DriverOnboardingState

    /** Driver profile created and awaiting administrative verification review (PENDING) */
    data class PendingVerification(val profile: DriverProfile) : DriverOnboardingState

    /** Driver platform verification has been approved (VERIFIED) */
    data class Verified(val profile: DriverProfile) : DriverOnboardingState

    /** Driver verification was rejected by administrator with reason (REJECTED) */
    data class Rejected(val profile: DriverProfile, val reason: String?) : DriverOnboardingState

    /** Driver account has been administratively suspended */
    data class Suspended(val profile: DriverProfile, val reason: String?) : DriverOnboardingState

    /** Error occurred while interacting with driver domain endpoints */
    data class Error(val error: IshaaraError, val canRetry: Boolean = true) : DriverOnboardingState
}
