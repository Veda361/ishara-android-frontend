package com.ishara.app.domain.repository

import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverActiveTrip
import com.ishara.app.domain.model.DriverEmergencyContact
import com.ishara.app.domain.model.DriverIdentity
import com.ishara.app.domain.model.DriverOnboardingState
import com.ishara.app.domain.model.DriverOperationalContext
import com.ishara.app.domain.model.DriverProfile
import com.ishara.app.domain.model.DriverVerificationDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Domain repository contract for Driver / Conductor operations.
 * Decoupled from HTTP, serialization, and frameworks.
 *
 * Implements authoritative driver onboarding, verification state machine,
 * driver profile cache management, and operational readiness prerequisites.
 */
interface DriverRepository {

    /**
     * Retrieves the comprehensive live operational snapshot for the authenticated driver.
     */
    suspend fun getOperationalContext(timezone: String? = null): IshaaraResult<DriverOperationalContext>

    /**
     * Initializes the driver verification profile on backend with a license number.
     * Kept for backwards-compatibility.
     */
    suspend fun createProfile(licenseNumber: String): IshaaraResult<DriverIdentity> =
        IshaaraResult.failure(IshaaraError.Validation(message = "Not implemented"))

    /**
     * Fetches the authoritative DriverProfile from GET /api/v1/drivers/me.
     */
    suspend fun getDriverProfile(): IshaaraResult<DriverProfile> =
        IshaaraResult.failure(IshaaraError.NotFound(message = "Not implemented"))

    /**
     * Submits driver profile onboarding via POST /api/v1/drivers/me.
     * Sets verificationStatus to PENDING on backend.
     */
    suspend fun createDriverProfile(
        licenseNumber: String,
        yearsOfExperience: Int? = null,
        emergencyContact: DriverEmergencyContact? = null,
        operatingType: String
    ): IshaaraResult<DriverProfile> =
        IshaaraResult.failure(IshaaraError.Validation(message = "Not implemented"))

    /**
     * Updates safe driver profile fields via PATCH /api/v1/drivers/me.
     */
    suspend fun updateDriverProfile(
        licenseNumber: String? = null,
        yearsOfExperience: Int? = null,
        emergencyContact: DriverEmergencyContact? = null
    ): IshaaraResult<DriverProfile> =
        IshaaraResult.failure(IshaaraError.Validation(message = "Not implemented"))

    /**
     * Retrieves driver-facing verification status from GET /api/v1/drivers/me/verification.
     */
    suspend fun getVerificationStatus(): IshaaraResult<DriverVerificationDetails> =
        IshaaraResult.failure(IshaaraError.NotFound(message = "Not implemented"))

    /**
     * Submits or resubmits driver verification via POST /api/v1/drivers/me/verification.
     */
    suspend fun submitVerification(notes: String? = null): IshaaraResult<DriverVerificationDetails> =
        IshaaraResult.failure(IshaaraError.Validation(message = "Not implemented"))

    /**
     * Observes the cached authoritative DriverProfile.
     */
    fun observeDriverProfile(): StateFlow<DriverProfile?> = MutableStateFlow(null)

    /**
     * Observes the reactive state machine for Driver Onboarding and Verification.
     */
    fun observeDriverOnboardingState(): StateFlow<DriverOnboardingState> = MutableStateFlow(DriverOnboardingState.Loading)

    /**
     * Observes the authoritative driver profile state machine.
     * Strictly separates Loading, NeedsOperatingTypeSelection, Ready, and Error.
     */
    fun observeDriverProfileState(): StateFlow<com.ishara.app.domain.model.DriverProfileState> =
        MutableStateFlow(com.ishara.app.domain.model.DriverProfileState.Loading)

    /**
     * Refreshes the cached driver profile from backend and updates the onboarding state.
     */
    suspend fun refreshDriverProfile(): IshaaraResult<DriverProfile> = getDriverProfile()

    /**
     * Clears all in-memory cached driver state upon logout or account switching.
     */
    fun clearDriverState() {}

    /**
     * Transitions the verified driver to ONLINE status.
     */
    suspend fun setOnline(): IshaaraResult<DriverIdentity>

    /**
     * Transitions the driver to OFFLINE status.
     */
    suspend fun setOffline(): IshaaraResult<DriverIdentity>

    /**
     * Starts a CREATED trip atomically (transitions to ACTIVE).
     */
    suspend fun startTrip(tripId: String): IshaaraResult<DriverActiveTrip>

    /**
     * Completes an ACTIVE trip atomically (transitions to COMPLETED).
     */
    suspend fun completeTrip(tripId: String): IshaaraResult<DriverActiveTrip>

    /**
     * Cancels a CREATED or ACTIVE trip atomically.
     */
    suspend fun cancelTrip(tripId: String): IshaaraResult<DriverActiveTrip>

    /**
     * Ingests driver GPS update (deferred to Phase 09, stubbed for compatibility).
     */
    suspend fun updateLocation(location: LocationCoordinates): IshaaraResult<Unit>
}
