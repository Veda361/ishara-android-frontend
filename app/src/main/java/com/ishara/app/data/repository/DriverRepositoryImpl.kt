package com.ishara.app.data.repository

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.mapper.DriverMapper
import com.ishara.app.data.remote.datasource.DriverRemoteDataSource
import com.ishara.app.data.remote.dto.CreateDriverProfileRequestDto
import com.ishara.app.data.remote.dto.EmergencyContactRequestDto
import com.ishara.app.data.remote.dto.UpdateDriverProfileRequestDto
import com.ishara.app.domain.model.DriverActiveTrip
import com.ishara.app.domain.model.DriverEmergencyContact
import com.ishara.app.domain.model.DriverIdentity
import com.ishara.app.domain.model.DriverOnboardingState
import com.ishara.app.domain.model.DriverOperationalContext
import com.ishara.app.domain.model.DriverProfile
import com.ishara.app.domain.model.DriverVerificationDetails
import com.ishara.app.domain.model.DriverVerificationStatus
import com.ishara.app.domain.repository.DriverRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Production implementation of [DriverRepository].
 * Manages driver profile cache, verification state machine, and operational execution.
 */
class DriverRepositoryImpl(
    private val remoteDataSource: DriverRemoteDataSource,
    private val localDataSource: SessionLocalDataSource,
    private val autoProvisionOnNotFound: Boolean = false
) : DriverRepository {

    private val mutex = Mutex()
    private val _driverProfileFlow = MutableStateFlow<DriverProfile?>(null)
    private val _driverOnboardingStateFlow = MutableStateFlow<DriverOnboardingState>(DriverOnboardingState.Loading)
    private val _driverProfileStateFlow = MutableStateFlow<com.ishara.app.domain.model.DriverProfileState>(com.ishara.app.domain.model.DriverProfileState.Loading)

    override fun observeDriverProfile(): StateFlow<DriverProfile?> = _driverProfileFlow.asStateFlow()

    override fun observeDriverOnboardingState(): StateFlow<DriverOnboardingState> = _driverOnboardingStateFlow.asStateFlow()

    override fun observeDriverProfileState(): StateFlow<com.ishara.app.domain.model.DriverProfileState> = _driverProfileStateFlow.asStateFlow()

    override suspend fun getDriverProfile(): IshaaraResult<DriverProfile> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        val result = remoteDataSource.getDriverProfile(session.token)
        return when (result) {
            is IshaaraResult.Success -> {
                val profile = DriverMapper.toDomain(result.data)
                _driverProfileFlow.value = profile
                _driverOnboardingStateFlow.value = mapToOnboardingState(profile)
                if (profile.operatingType.isBlank()) {
                    _driverProfileStateFlow.value = com.ishara.app.domain.model.DriverProfileState.NeedsOperatingTypeSelection
                } else {
                    _driverProfileStateFlow.value = com.ishara.app.domain.model.DriverProfileState.Ready(profile, profile.operatingType)
                }
                IshaaraLogger.i(
                    "ISHAARA_DRIVER_PROFILE",
                    "role=DRIVER_CONDUCTOR operatingType=${profile.operatingType} verificationStatus=${profile.verificationStatus}"
                )
                IshaaraResult.success(profile)
            }
            is IshaaraResult.Failure -> {
                if (result.error is IshaaraError.NotFound) {
                    IshaaraLogger.i(TAG, "Driver profile not found (404). Transitioning to NeedsOnboarding / NeedsOperatingTypeSelection.")
                    _driverProfileFlow.value = null
                    _driverProfileStateFlow.value = com.ishara.app.domain.model.DriverProfileState.NeedsOperatingTypeSelection
                    _driverOnboardingStateFlow.value = DriverOnboardingState.NeedsOnboarding
                } else {
                    _driverProfileStateFlow.value = com.ishara.app.domain.model.DriverProfileState.Error(result.error)
                    _driverOnboardingStateFlow.value = DriverOnboardingState.Error(result.error)
                }
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun createDriverProfile(
        licenseNumber: String,
        yearsOfExperience: Int?,
        emergencyContact: DriverEmergencyContact?,
        operatingType: String
    ): IshaaraResult<DriverProfile> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        val trimmedType = operatingType.trim().uppercase()
        if (trimmedType != "INDIVIDUAL" && trimmedType != "AGENCY") {
            _driverOnboardingStateFlow.value = DriverOnboardingState.NeedsOperatingTypeSelection
            _driverProfileStateFlow.value = com.ishara.app.domain.model.DriverProfileState.NeedsOperatingTypeSelection
            return IshaaraResult.failure(
                IshaaraError.Validation(field = "operatingType", message = "Please choose an operating type (Individual or Agency/Fleet).")
            )
        }

        _driverOnboardingStateFlow.value = DriverOnboardingState.Submitting

        val chosenOperatingType = trimmedType
        IshaaraLogger.i("ISHAARA_ONBOARDING", "selection=$chosenOperatingType")

        val request = CreateDriverProfileRequestDto(
            licenseNumber = licenseNumber.trim().uppercase(),
            yearsOfExperience = yearsOfExperience,
            emergencyContact = emergencyContact?.let {
                EmergencyContactRequestDto(
                    name = it.name.trim(),
                    phoneNumber = it.phoneNumber.trim(),
                    relationship = it.relationship?.trim()
                )
            },
            operatingType = chosenOperatingType
        )

        val result = remoteDataSource.createDriverProfileFull(request, session.token)
        return when (result) {
            is IshaaraResult.Success -> {
                val profile = DriverMapper.toDomain(result.data)

                // Authoritative profile reconciliation: GET /drivers/me to confirm backend persistence
                val authoritativeResult = remoteDataSource.getDriverProfile(session.token)
                val finalProfile = if (authoritativeResult is IshaaraResult.Success) {
                    DriverMapper.toDomain(authoritativeResult.data)
                } else {
                    profile
                }

                IshaaraLogger.i("ISHAARA_ONBOARDING", "backendPersistence=SUCCESS")
                IshaaraLogger.i("ISHAARA_ONBOARDING", "backendOperatingType=${finalProfile.operatingType}")
                IshaaraLogger.i(
                    "ISHAARA_DRIVER_PROFILE",
                    "role=DRIVER_CONDUCTOR operatingType=${finalProfile.operatingType} verificationStatus=${finalProfile.verificationStatus}"
                )

                _driverProfileFlow.value = finalProfile
                _driverProfileStateFlow.value = if (finalProfile.operatingType.isBlank()) {
                    com.ishara.app.domain.model.DriverProfileState.NeedsOperatingTypeSelection
                } else {
                    com.ishara.app.domain.model.DriverProfileState.Ready(finalProfile, finalProfile.operatingType)
                }
                _driverOnboardingStateFlow.value = mapToOnboardingState(finalProfile)
                IshaaraLogger.i(TAG, "Driver profile created successfully: id=${finalProfile.id}")
                IshaaraResult.success(finalProfile)
            }
            is IshaaraResult.Failure -> {
                if (result.error is IshaaraError.Conflict) {
                    IshaaraLogger.i(TAG, "Driver profile already exists (409 Conflict). Reconciling state with GET /me...")
                    val fetchResult = remoteDataSource.getDriverProfile(session.token)
                    if (fetchResult is IshaaraResult.Success) {
                        val reconciled = DriverMapper.toDomain(fetchResult.data)
                        _driverProfileFlow.value = reconciled
                        _driverProfileStateFlow.value = if (reconciled.operatingType.isBlank()) {
                            com.ishara.app.domain.model.DriverProfileState.NeedsOperatingTypeSelection
                        } else {
                            com.ishara.app.domain.model.DriverProfileState.Ready(reconciled, reconciled.operatingType)
                        }
                        _driverOnboardingStateFlow.value = mapToOnboardingState(reconciled)
                        IshaaraLogger.i(
                            "ISHAARA_DRIVER_PROFILE",
                            "role=DRIVER_CONDUCTOR operatingType=${reconciled.operatingType} verificationStatus=${reconciled.verificationStatus}"
                        )
                        return IshaaraResult.success(reconciled)
                    }
                }
                _driverProfileStateFlow.value = com.ishara.app.domain.model.DriverProfileState.Error(result.error)
                _driverOnboardingStateFlow.value = DriverOnboardingState.Error(result.error)
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun updateDriverProfile(
        licenseNumber: String?,
        yearsOfExperience: Int?,
        emergencyContact: DriverEmergencyContact?
    ): IshaaraResult<DriverProfile> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        val request = UpdateDriverProfileRequestDto(
            licenseNumber = licenseNumber?.trim()?.uppercase(),
            yearsOfExperience = yearsOfExperience,
            emergencyContact = emergencyContact?.let {
                EmergencyContactRequestDto(
                    name = it.name.trim(),
                    phoneNumber = it.phoneNumber.trim(),
                    relationship = it.relationship?.trim()
                )
            }
        )

        val result = remoteDataSource.updateDriverProfile(request, session.token)
        return when (result) {
            is IshaaraResult.Success -> {
                val profile = DriverMapper.toDomain(result.data)
                _driverProfileFlow.value = profile
                _driverOnboardingStateFlow.value = mapToOnboardingState(profile)
                IshaaraResult.success(profile)
            }
            is IshaaraResult.Failure -> {
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun getVerificationStatus(): IshaaraResult<DriverVerificationDetails> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        val result = remoteDataSource.getVerificationStatus(session.token)
        return result.map { dto ->
            val details = DriverMapper.toDomain(dto)
            // Update cached profile verification status if cached profile exists
            _driverProfileFlow.value?.let { current ->
                val updated = current.copy(
                    verificationStatus = details.verificationStatus,
                    rejectionReason = details.rejectionReason,
                    submittedAt = details.submittedAt,
                    reviewedAt = details.reviewedAt,
                    reviewedBy = details.reviewedBy
                )
                _driverProfileFlow.value = updated
                _driverOnboardingStateFlow.value = mapToOnboardingState(updated)
            }
            details
        }
    }

    override suspend fun submitVerification(notes: String?): IshaaraResult<DriverVerificationDetails> = mutex.withLock {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        val result = remoteDataSource.submitVerification(notes, session.token)
        return when (result) {
            is IshaaraResult.Success -> {
                val details = DriverMapper.toDomain(result.data)
                _driverProfileFlow.value?.let { current ->
                    val updated = current.copy(
                        verificationStatus = details.verificationStatus,
                        rejectionReason = null,
                        submittedAt = details.submittedAt
                    )
                    _driverProfileFlow.value = updated
                    _driverOnboardingStateFlow.value = mapToOnboardingState(updated)
                }
                IshaaraLogger.i(TAG, "Driver verification submitted/resubmitted: status=${details.verificationStatus}")
                IshaaraResult.success(details)
            }
            is IshaaraResult.Failure -> {
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun refreshDriverProfile(): IshaaraResult<DriverProfile> {
        return getDriverProfile()
    }

    override fun clearDriverState() {
        _driverProfileFlow.value = null
        _driverProfileStateFlow.value = com.ishara.app.domain.model.DriverProfileState.Loading
        _driverOnboardingStateFlow.value = DriverOnboardingState.Loading
        IshaaraLogger.i(TAG, "Driver state cleared.")
    }

    private fun mapToOnboardingState(profile: DriverProfile): DriverOnboardingState {
        return when {
            profile.operatingType.isBlank() -> DriverOnboardingState.NeedsOperatingTypeSelection
            profile.isSuspended -> DriverOnboardingState.Suspended(profile, profile.suspensionReason)
            profile.verificationStatus == DriverVerificationStatus.VERIFIED -> DriverOnboardingState.Verified(profile)
            profile.verificationStatus == DriverVerificationStatus.REJECTED -> DriverOnboardingState.Rejected(profile, profile.rejectionReason)
            else -> DriverOnboardingState.PendingVerification(profile)
        }
    }

    override suspend fun getOperationalContext(timezone: String?): IshaaraResult<DriverOperationalContext> {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))

        val result = remoteDataSource.getOperationalContext(timezone, session.token)
        if (autoProvisionOnNotFound && result is IshaaraResult.Failure && result.error is IshaaraError.NotFound) {
            // Auto-heal ONLY when explicitly enabled (for legacy unit tests)
            val licenseSuffix = session.userId.filter { it.isLetterOrDigit() }.takeLast(8).ifEmpty { "00000001" }.uppercase()
            val initialLicense = "DL-$licenseSuffix"
            val createResult = remoteDataSource.createDriverProfile(initialLicense, session.token)
            if (createResult is IshaaraResult.Success) {
                return remoteDataSource.getOperationalContext(timezone, session.token).map { dto ->
                    DriverMapper.toDomain(dto)
                }
            }
        }

        return result.map { dto ->
            DriverMapper.toDomain(dto)
        }
    }

    override suspend fun createProfile(licenseNumber: String): IshaaraResult<DriverIdentity> {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))
        return remoteDataSource.createDriverProfile(licenseNumber, session.token).map { dto ->
            DriverMapper.toDomain(dto)
        }
    }

    override suspend fun setOnline(): IshaaraResult<DriverIdentity> {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))
        return remoteDataSource.setOnline(session.token).map { dto ->
            DriverMapper.toDomain(dto)
        }
    }

    override suspend fun setOffline(): IshaaraResult<DriverIdentity> {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))
        return remoteDataSource.setOffline(session.token).map { dto ->
            DriverMapper.toDomain(dto)
        }
    }

    override suspend fun startTrip(tripId: String): IshaaraResult<DriverActiveTrip> {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))
        return remoteDataSource.startTrip(tripId, session.token).map { dto ->
            DriverMapper.toDomain(dto)
        }
    }

    override suspend fun completeTrip(tripId: String): IshaaraResult<DriverActiveTrip> {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))
        return remoteDataSource.completeTrip(tripId, session.token).map { dto ->
            DriverMapper.toDomain(dto)
        }
    }

    override suspend fun cancelTrip(tripId: String): IshaaraResult<DriverActiveTrip> {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))
        return remoteDataSource.cancelTrip(tripId, session.token).map { dto ->
            DriverMapper.toDomain(dto)
        }
    }

    override suspend fun updateLocation(location: LocationCoordinates): IshaaraResult<Unit> {
        val session = localDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "No active session."))
        return remoteDataSource.updateLocation(location, session.token)
    }

    companion object {
        private const val TAG = "DriverRepository"
    }
}
