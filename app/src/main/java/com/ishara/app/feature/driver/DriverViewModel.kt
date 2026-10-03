package com.ishara.app.feature.driver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverIdentity
import com.ishara.app.domain.model.DriverOperationalContext
import com.ishara.app.domain.model.DriverProfileStatus
import com.ishara.app.domain.model.DriverTodayStats
import com.ishara.app.domain.model.DriverTripStatus
import com.ishara.app.domain.model.DriverVerificationStatus
import com.ishara.app.domain.usecase.GetDriverOperationalContextUseCase
import com.ishara.app.domain.usecase.ManageDriverTripLifecycleUseCase
import com.ishara.app.domain.usecase.SetDriverAvailabilityUseCase
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Production ViewModel managing the Driver / Conductor operational experience.
 *
 * Implements:
 * 1. Strict Unidirectional Data Flow (UDF) through [DriverHomeUiState].
 * 2. Explicit lifecycle stages: Loading, Content, Empty, Offline, Error.
 * 3. Driver availability transitions: Online / Offline.
 * 4. Authoritative trip lifecycle transitions: Start / Complete / Cancel.
 * 5. Concurrent action and duplicate tap prevention.
 * 6. Role protection handling (403 Forbidden -> non-retryable error).
 */
class DriverViewModel(
    val driverName: String,
    private val getDriverOperationalContextUseCase: GetDriverOperationalContextUseCase,
    private val setDriverAvailabilityUseCase: SetDriverAvailabilityUseCase,
    private val manageDriverTripLifecycleUseCase: ManageDriverTripLifecycleUseCase,
    private val navigationManager: NavigationManager,
    private val startDriverTrackingUseCase: com.ishara.app.domain.usecase.StartDriverTrackingUseCase? = null,
    private val stopDriverTrackingUseCase: com.ishara.app.domain.usecase.StopDriverTrackingUseCase? = null,
    private val observeDriverTrackingStatusUseCase: com.ishara.app.domain.usecase.ObserveDriverTrackingStatusUseCase? = null,
    private val getDriverRatingSummaryUseCase: com.ishara.app.domain.usecase.GetDriverRatingSummaryUseCase? = null,
    private val observeAgencyMembershipStateUseCase: com.ishara.app.domain.usecase.ObserveAgencyMembershipStateUseCase? = null,
    private val observeDriverProfileUseCase: com.ishara.app.domain.usecase.ObserveDriverProfileUseCase? = null,
    private val refreshDriverProfileUseCase: com.ishara.app.domain.usecase.RefreshDriverProfileUseCase? = null,
    private val refreshAgencyMembershipUseCase: com.ishara.app.domain.usecase.RefreshAgencyMembershipUseCase? = null,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(DriverHomeUiState())
    val uiState: StateFlow<DriverHomeUiState> = _uiState.asStateFlow()

    init {
        loadOperationalContext()
        observeTrackingStatus()
        observeMembershipState()
        observeDriverProfile()
    }

    private fun observeTrackingStatus() {
        observeDriverTrackingStatusUseCase?.let { useCase ->
            scope.launch(dispatchers.main) {
                useCase().collect { status ->
                    _uiState.update { it.copy(telemetryStatus = status) }
                }
            }
        }
    }

    private fun observeMembershipState() {
        observeAgencyMembershipStateUseCase?.let { useCase ->
            scope.launch(dispatchers.main) {
                useCase().collect { memState ->
                    _uiState.update { current ->
                        val updatedAccess = com.ishara.app.domain.model.DriverAccessState.resolve(
                            verificationStatus = current.accessState.verificationStatus,
                            isSuspended = current.driverProfile?.isSuspended ?: false,
                            operatingType = current.driverProfile?.operatingType.orEmpty(),
                            membershipState = memState,
                            vehicle = current.getCurrentVehicle()
                        )
                        current.copy(
                            membershipState = memState,
                            accessState = updatedAccess
                        )
                    }
                }
            }
        }
    }

    private fun observeDriverProfile() {
        observeDriverProfileUseCase?.let { useCase ->
            scope.launch(dispatchers.main) {
                useCase().collect { profile ->
                    if (profile != null) {
                        _uiState.update { current ->
                            val updatedAccess = com.ishara.app.domain.model.DriverAccessState.resolve(
                                verificationStatus = profile.verificationStatus,
                                isSuspended = profile.isSuspended,
                                operatingType = profile.operatingType,
                                membershipState = current.membershipState,
                                vehicle = current.getCurrentVehicle()
                            )
                            current.copy(
                                driverProfile = profile,
                                accessState = updatedAccess
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Refreshes driver profile, agency membership, and operational context from authoritative backend.
     */
    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true, userFacingError = null) }
        scope.launch(dispatchers.io) {
            refreshDriverProfileUseCase?.invoke()
            refreshAgencyMembershipUseCase?.invoke()
            loadOperationalContext(silent = true)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    /**
     * Fetches the latest authoritative operational context from the backend.
     * When [silent] is true, avoids replacing active content with a full-screen loading spinner.
     */
    fun loadOperationalContext(silent: Boolean = false) {
        if (!silent) {
            _uiState.update {
                it.copy(
                    stage = DriverHomeStage.Loading,
                    userFacingError = null
                )
            }
        }

        scope.launch(dispatchers.io) {
            val result = getDriverOperationalContextUseCase.execute()
            when (result) {
                is IshaaraResult.Success -> {
                    val context = result.data
                    val stage = resolveStage(context)

                    if (context.activeTrip?.status == DriverTripStatus.ACTIVE) {
                        startDriverTrackingUseCase?.invoke(context.activeTrip.id)
                    } else {
                        stopDriverTrackingUseCase?.invoke()
                    }

                    _uiState.update { current ->
                        val updatedAccess = com.ishara.app.domain.model.DriverAccessState.resolve(
                            verificationStatus = context.driver.verificationStatus,
                            isSuspended = current.driverProfile?.isSuspended ?: false,
                            operatingType = current.driverProfile?.operatingType.orEmpty(),
                            membershipState = current.membershipState,
                            vehicle = context.vehicle
                        )
                        current.copy(
                            stage = stage,
                            accessState = updatedAccess,
                            isActionInProgress = false,
                            tripActionState = if (!silent) TripActionState.Idle else current.tripActionState,
                            statusActionState = if (!silent) DriverStatusActionState.Idle else current.statusActionState
                        )
                    }

                    getDriverRatingSummaryUseCase?.let { useCase ->
                        when (val ratingResult = useCase()) {
                            is IshaaraResult.Success -> {
                                _uiState.update { it.copy(ratingSummary = ratingResult.data) }
                            }
                            is IshaaraResult.Failure -> {
                                // Silent fallback - non-blocking
                            }
                        }
                    }
                }
                is IshaaraResult.Failure -> {
                    val isDriverNotVerified = (result.error is IshaaraError.Forbidden &&
                            (result.error.errorCode == "DRIVER_NOT_VERIFIED" || result.error.message.contains("DRIVER_NOT_VERIFIED", ignoreCase = true))) ||
                            (result.error is IshaaraError.Validation &&
                            (result.error.errorCode == "DRIVER_NOT_VERIFIED" || result.error.message.contains("DRIVER_NOT_VERIFIED", ignoreCase = true)))

                    if (isDriverNotVerified) {
                        // Driver is not verified yet, but application access is granted.
                        // Provide an offline, unverified operational context so Driver Home is fully usable.
                        val unverifiedIdentity = DriverIdentity(
                            id = _uiState.value.driverProfile?.id.orEmpty(),
                            userId = _uiState.value.driverProfile?.userId.orEmpty(),
                            verificationStatus = _uiState.value.driverProfile?.verificationStatus ?: DriverVerificationStatus.PENDING,
                            status = DriverProfileStatus.OFFLINE,
                            licenseNumberMasked = _uiState.value.driverProfile?.licenseNumberMasked,
                            licenseVerifiedAt = null
                        )
                        val offlineContext = DriverOperationalContext(
                            driver = unverifiedIdentity,
                            vehicle = null,
                            activeTrip = null,
                            activeRidesCount = 0,
                            todayStats = DriverTodayStats(
                                completedRidesCount = 0,
                                isOnline = false,
                                currentDate = "",
                                timezone = "Asia/Kolkata"
                            )
                        )
                        _uiState.update { current ->
                            val updatedAccess = com.ishara.app.domain.model.DriverAccessState.resolve(
                                verificationStatus = unverifiedIdentity.verificationStatus,
                                isSuspended = current.driverProfile?.isSuspended ?: false,
                                operatingType = current.driverProfile?.operatingType.orEmpty(),
                                membershipState = current.membershipState,
                                vehicle = null
                            )
                            current.copy(
                                stage = DriverHomeStage.Offline(offlineContext),
                                accessState = updatedAccess,
                                isActionInProgress = false,
                                tripActionState = if (!silent) TripActionState.Idle else current.tripActionState,
                                statusActionState = if (!silent) DriverStatusActionState.Idle else current.statusActionState
                            )
                        }
                    } else {
                        val isForbidden = result.error is IshaaraError.Forbidden
                        _uiState.update {
                            it.copy(
                                stage = DriverHomeStage.Error(
                                    error = result.error,
                                    canRetry = !isForbidden
                                ),
                                isActionInProgress = false,
                                tripActionState = if (!silent) TripActionState.Idle else it.tripActionState,
                                statusActionState = if (!silent) DriverStatusActionState.Idle else it.statusActionState
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Resolves the operational context into an explicit stage.
     */
    private fun resolveStage(context: DriverOperationalContext): DriverHomeStage {
        return when {
            context.driver.status == DriverProfileStatus.OFFLINE -> {
                DriverHomeStage.Offline(context)
            }
            context.activeTrip == null -> {
                DriverHomeStage.Empty(context)
            }
            else -> {
                DriverHomeStage.Content(context)
            }
        }
    }

    /**
     * Transitions driver status to ONLINE.
     * Precondition: Driver must be VERIFIED and have operational access.
     */
    fun goOnline() {
        if (_uiState.value.isActionInProgress) return

        val accessState = _uiState.value.accessState
        val currentContext = getCurrentContext()
        val isVerified = accessState.platformVerified || currentContext?.driver?.verificationStatus == DriverVerificationStatus.VERIFIED
        if (!isVerified || !accessState.operationalAccess) {
            _uiState.update {
                it.copy(
                    userFacingError = accessState.operationalLockReason ?: "Cannot go online: Driver profile verification is pending or rejected."
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isActionInProgress = true,
                statusActionState = DriverStatusActionState.Submitting,
                userFacingError = null
            )
        }

        scope.launch(dispatchers.io) {
            val result = setDriverAvailabilityUseCase.goOnline()
            when (result) {
                is IshaaraResult.Success -> {
                    _uiState.update {
                        it.copy(
                            statusActionState = DriverStatusActionState.Idle,
                            userFacingNotification = "You are now Online and available."
                        )
                    }
                    loadOperationalContext(silent = true)
                }
                is IshaaraResult.Failure -> {
                    val userMsg = if ((result.error is IshaaraError.Forbidden && (result.error.errorCode == "DRIVER_NOT_VERIFIED" || result.error.message.contains("DRIVER_NOT_VERIFIED", ignoreCase = true))) ||
                        (result.error is IshaaraError.Validation && (result.error.errorCode == "DRIVER_NOT_VERIFIED" || result.error.message.contains("DRIVER_NOT_VERIFIED", ignoreCase = true)))) {
                        "Cannot go online: Driver verification is pending administrative review."
                    } else {
                        result.error.message
                    }
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            statusActionState = DriverStatusActionState.Error(result.error),
                            userFacingError = userMsg
                        )
                    }
                }
            }
        }
    }

    /**
     * Transitions driver status to OFFLINE.
     * Precondition: Driver must not currently be ON_RIDE.
     */
    fun goOffline() {
        if (_uiState.value.isActionInProgress) return

        val currentContext = getCurrentContext()
        if (currentContext?.driver?.status == DriverProfileStatus.ON_RIDE) {
            _uiState.update {
                it.copy(
                    userFacingError = "Cannot go offline while operating an active ride."
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isActionInProgress = true,
                statusActionState = DriverStatusActionState.Submitting,
                userFacingError = null
            )
        }

        scope.launch(dispatchers.io) {
            val result = setDriverAvailabilityUseCase.goOffline()
            when (result) {
                is IshaaraResult.Success -> {
                    stopDriverTrackingUseCase?.invoke()
                    _uiState.update {
                        it.copy(
                            statusActionState = DriverStatusActionState.Idle,
                            userFacingNotification = "You are now Offline."
                        )
                    }
                    loadOperationalContext(silent = true)
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            statusActionState = DriverStatusActionState.Error(result.error),
                            userFacingError = result.error.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Starts a CREATED trip atomically (transitions to ACTIVE).
     */
    fun startTrip(tripId: String) {
        if (_uiState.value.isActionInProgress) return

        val accessState = _uiState.value.accessState
        val currentContext = getCurrentContext()
        val isVerified = accessState.platformVerified || currentContext?.driver?.verificationStatus == DriverVerificationStatus.VERIFIED
        if (!isVerified || !accessState.operationalAccess) {
            _uiState.update {
                it.copy(
                    userFacingError = accessState.operationalLockReason ?: "Cannot start trip: Driver verification required."
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isActionInProgress = true,
                tripActionState = TripActionState.Submitting(tripId, TripActionType.START),
                userFacingError = null
            )
        }

        scope.launch(dispatchers.io) {
            val result = manageDriverTripLifecycleUseCase.startTrip(tripId)
            when (result) {
                is IshaaraResult.Success -> {
                    startDriverTrackingUseCase?.invoke(tripId)
                    _uiState.update {
                        it.copy(
                            tripActionState = TripActionState.Success(result.data, "Trip started successfully."),
                            userFacingNotification = "Trip is now active."
                        )
                    }
                    loadOperationalContext(silent = true)
                }
                is IshaaraResult.Failure -> {
                    val userMsg = if ((result.error is IshaaraError.Forbidden && (result.error.errorCode == "DRIVER_NOT_VERIFIED" || result.error.message.contains("DRIVER_NOT_VERIFIED", ignoreCase = true))) ||
                        (result.error is IshaaraError.Validation && (result.error.errorCode == "DRIVER_NOT_VERIFIED" || result.error.message.contains("DRIVER_NOT_VERIFIED", ignoreCase = true)))) {
                        "Cannot start trip: Driver verification is required."
                    } else {
                        result.error.message
                    }
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            tripActionState = TripActionState.Error(result.error),
                            userFacingError = userMsg
                        )
                    }
                }
            }
        }
    }

    /**
     * Completes an ACTIVE trip atomically (transitions to COMPLETED).
     */
    fun completeTrip(tripId: String) {
        if (_uiState.value.isActionInProgress) return

        _uiState.update {
            it.copy(
                isActionInProgress = true,
                tripActionState = TripActionState.Submitting(tripId, TripActionType.COMPLETE),
                userFacingError = null
            )
        }

        scope.launch(dispatchers.io) {
            val result = manageDriverTripLifecycleUseCase.completeTrip(tripId)
            when (result) {
                is IshaaraResult.Success -> {
                    stopDriverTrackingUseCase?.invoke()
                    _uiState.update {
                        it.copy(
                            tripActionState = TripActionState.Success(result.data, "Trip marked as completed."),
                            userFacingNotification = "Trip completed successfully."
                        )
                    }
                    loadOperationalContext(silent = true)
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            tripActionState = TripActionState.Error(result.error),
                            userFacingError = result.error.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Cancels a CREATED or ACTIVE trip atomically.
     */
    fun cancelTrip(tripId: String) {
        if (_uiState.value.isActionInProgress) return

        _uiState.update {
            it.copy(
                isActionInProgress = true,
                tripActionState = TripActionState.Submitting(tripId, TripActionType.CANCEL),
                userFacingError = null
            )
        }

        scope.launch(dispatchers.io) {
            val result = manageDriverTripLifecycleUseCase.cancelTrip(tripId)
            when (result) {
                is IshaaraResult.Success -> {
                    stopDriverTrackingUseCase?.invoke()
                    _uiState.update {
                        it.copy(
                            tripActionState = TripActionState.Success(result.data, "Trip cancelled."),
                            userFacingNotification = "Trip has been cancelled."
                        )
                    }
                    loadOperationalContext(silent = true)
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isActionInProgress = false,
                            tripActionState = TripActionState.Error(result.error),
                            userFacingError = result.error.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Clears user-facing error and notification messages.
     */
    fun dismissNotification() {
        _uiState.update {
            it.copy(
                userFacingNotification = null,
                userFacingError = null,
                tripActionState = TripActionState.Idle,
                statusActionState = DriverStatusActionState.Idle
            )
        }
    }

    /**
     * Retries loading operational context after an error.
     */
    fun retry() {
        loadOperationalContext()
    }

    /**
     * Extracts active [DriverOperationalContext] from the current stage if available.
     */
    fun getCurrentContext(): DriverOperationalContext? {
        return when (val stage = _uiState.value.stage) {
            is DriverHomeStage.Content -> stage.context
            is DriverHomeStage.Empty -> stage.context
            is DriverHomeStage.Offline -> stage.context
            else -> null
        }
    }
}
