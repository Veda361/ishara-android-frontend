package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverEmergencyContact
import com.ishara.app.domain.model.DriverOnboardingState
import com.ishara.app.domain.model.DriverProfile
import com.ishara.app.domain.model.DriverVerificationDetails
import com.ishara.app.domain.repository.DriverRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Use case to fetch the authenticated driver's profile from GET /api/v1/drivers/me.
 */
class GetDriverProfileUseCase(
    private val driverRepository: DriverRepository
) {
    suspend operator fun invoke(): IshaaraResult<DriverProfile> {
        return driverRepository.getDriverProfile()
    }
}

/**
 * Use case to submit driver onboarding and initialize driver credentials via POST /api/v1/drivers/me.
 */
class CreateDriverProfileUseCase(
    private val driverRepository: DriverRepository
) {
    suspend operator fun invoke(
        licenseNumber: String,
        yearsOfExperience: Int? = null,
        emergencyContact: DriverEmergencyContact? = null,
        operatingType: String
    ): IshaaraResult<DriverProfile> {
        return driverRepository.createDriverProfile(
            licenseNumber = licenseNumber,
            yearsOfExperience = yearsOfExperience,
            emergencyContact = emergencyContact,
            operatingType = operatingType
        )
    }
}

/**
 * Use case to update safe driver profile fields via PATCH /api/v1/drivers/me.
 */
class UpdateDriverProfileUseCase(
    private val driverRepository: DriverRepository
) {
    suspend operator fun invoke(
        licenseNumber: String? = null,
        yearsOfExperience: Int? = null,
        emergencyContact: DriverEmergencyContact? = null
    ): IshaaraResult<DriverProfile> {
        return driverRepository.updateDriverProfile(
            licenseNumber = licenseNumber,
            yearsOfExperience = yearsOfExperience,
            emergencyContact = emergencyContact
        )
    }
}

/**
 * Use case to fetch driver verification status from GET /api/v1/drivers/me/verification.
 */
class GetDriverVerificationStatusUseCase(
    private val driverRepository: DriverRepository
) {
    suspend operator fun invoke(): IshaaraResult<DriverVerificationDetails> {
        return driverRepository.getVerificationStatus()
    }
}

/**
 * Use case to submit or resubmit driver verification via POST /api/v1/drivers/me/verification.
 */
class SubmitDriverVerificationUseCase(
    private val driverRepository: DriverRepository
) {
    suspend operator fun invoke(notes: String? = null): IshaaraResult<DriverVerificationDetails> {
        return driverRepository.submitVerification(notes)
    }
}

/**
 * Use case to observe the reactive cached DriverProfile.
 */
class ObserveDriverProfileUseCase(
    private val driverRepository: DriverRepository
) {
    operator fun invoke(): StateFlow<DriverProfile?> {
        return driverRepository.observeDriverProfile()
    }
}

/**
 * Use case to observe the Driver Onboarding & Verification state machine.
 */
class ObserveDriverOnboardingStateUseCase(
    private val driverRepository: DriverRepository
) {
    operator fun invoke(): StateFlow<DriverOnboardingState> {
        return driverRepository.observeDriverOnboardingState()
    }
}

/**
 * Use case to refresh the cached driver profile.
 */
class RefreshDriverProfileUseCase(
    private val driverRepository: DriverRepository
) {
    suspend operator fun invoke(): IshaaraResult<DriverProfile> {
        return driverRepository.refreshDriverProfile()
    }
}

/**
 * Use case to purge driver transient state on logout or account switching.
 */
class ClearDriverStateUseCase(
    private val driverRepository: DriverRepository
) {
    operator fun invoke() {
        driverRepository.clearDriverState()
    }
}

/**
 * Use case to observe the authoritative DriverProfileState state machine.
 * Strictly separates Loading, NeedsOperatingTypeSelection, Ready, and Error.
 */
class ObserveDriverProfileStateUseCase(
    private val driverRepository: DriverRepository
) {
    operator fun invoke(): StateFlow<com.ishara.app.domain.model.DriverProfileState> {
        return driverRepository.observeDriverProfileState()
    }
}

