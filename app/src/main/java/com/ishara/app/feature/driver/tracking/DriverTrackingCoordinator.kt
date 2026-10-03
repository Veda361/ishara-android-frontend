package com.ishara.app.feature.driver.tracking

import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.location.FusedLocationProvider
import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.location.LocationProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.usecase.UpdateDriverLocationUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * Production Coordinator orchestrating driver GPS acquisition, monotonic validation,
 * offline freshest-only buffering, and backend REST synchronization.
 *
 * Guarantees:
 * 1. Monotonic Timestamp Enforcement: Drops out-of-order or stale location fixes.
 * 2. Freshest-Only Offline Policy: Never queues unbounded historical GPS fixes. Retains at most ONE freshest fix.
 * 3. Lifecycle-Aware: Started only during ACTIVE trips; stopped on complete/cancel/offline.
 * 4. Coordinate Safety: Preserves lat/lng until backend DTO boundary.
 */
class DriverTrackingCoordinator(
    private val locationProvider: LocationProvider,
    private val updateDriverLocationUseCase: UpdateDriverLocationUseCase,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    externalScope: CoroutineScope? = null
) {
    private val scope: CoroutineScope = externalScope ?: CoroutineScope(dispatchers.io)

    private val _status = MutableStateFlow<DriverTrackingStatus>(DriverTrackingStatus.Idle)
    val status: StateFlow<DriverTrackingStatus> = _status.asStateFlow()

    private var activeTripId: String? = null
    private var trackingJob: Job? = null
    private val lastSentMonotonicTime = AtomicLong(0L)

    // Freshest-only offline buffer (at most ONE location fix)
    @Volatile
    private var freshestPendingLocation: LocationCoordinates? = null

    fun isTrackingActive(): Boolean = trackingJob?.isActive == true && activeTripId != null

    /**
     * Starts continuous high-frequency GPS tracking for the designated active trip.
     */
    fun startTracking(
        tripId: String,
        intervalMillis: Long = FusedLocationProvider.DEFAULT_ACTIVE_INTERVAL_MILLIS
    ) {
        if (isTrackingActive() && activeTripId == tripId) {
            IshaaraLogger.i(TAG, "Tracking already active for trip: $tripId")
            return
        }

        stopTracking()
        activeTripId = tripId
        _status.value = DriverTrackingStatus.Active(tripId = tripId)
        IshaaraLogger.i(TAG, "Starting driver GPS tracking for trip: $tripId (interval: ${intervalMillis}ms)")

        trackingJob = scope.launch(dispatchers.io) {
            locationProvider.observeLocationUpdates(intervalMillis)
                .catch { throwable ->
                    IshaaraLogger.e(TAG, "Error in location updates stream", throwable)
                    if (throwable is SecurityException) {
                        _status.value = DriverTrackingStatus.PermissionRequired
                    } else {
                        _status.value = DriverTrackingStatus.Error(throwable.message ?: "Location stream failure")
                    }
                }
                .collect { location ->
                    processLocationFix(location, tripId)
                }
        }
    }

    /**
     * Halts GPS tracking and cleans up active resources.
     */
    fun stopTracking() {
        trackingJob?.cancel()
        trackingJob = null
        activeTripId = null
        freshestPendingLocation = null
        _status.value = DriverTrackingStatus.Idle
        IshaaraLogger.i(TAG, "Driver GPS tracking stopped.")
    }

    /**
     * Ingests a new hardware GPS fix and synchronizes with backend if valid and monotonic.
     */
    private suspend fun processLocationFix(location: LocationCoordinates, tripId: String) {
        // Enforce physical coordinate validity
        if (location.latitude !in -90.0..90.0 || location.longitude !in -180.0..180.0) {
            IshaaraLogger.w(TAG, "Dropping physically invalid coordinates.")
            return
        }

        // Monotonic timestamp validation: drop out-of-order fixes
        val lastTime = lastSentMonotonicTime.get()
        if (location.timestampMillis <= lastTime) {
            IshaaraLogger.w(TAG, "Dropping stale/out-of-order GPS fix (${location.timestampMillis} <= $lastTime).")
            return
        }

        val formattedLog = IshaaraLogger.formatCoordinatesForLog(location.latitude, location.longitude)
        IshaaraLogger.d(TAG, "Attempting location sync: $formattedLog (accuracy=${location.accuracyMeters}m)")

        when (val result = updateDriverLocationUseCase(location)) {
            is IshaaraResult.Success -> {
                lastSentMonotonicTime.set(location.timestampMillis)
                freshestPendingLocation = null
                _status.value = DriverTrackingStatus.Active(
                    tripId = tripId,
                    lastFixTimestampMillis = location.timestampMillis
                )
            }
            is IshaaraResult.Failure -> {
                handleSyncFailure(result.error, location, tripId)
            }
        }
    }

    private fun handleSyncFailure(
        error: IshaaraError,
        location: LocationCoordinates,
        tripId: String
    ) {
        when (error) {
            is IshaaraError.Network -> {
                // Offline freshest-only policy: store at most ONE newest location fix
                freshestPendingLocation = location
                _status.value = DriverTrackingStatus.NetworkUnavailable(freshestFix = location)
                IshaaraLogger.w(TAG, "Network unavailable during location sync. Buffered single freshest fix.")
            }
            is IshaaraError.Authentication -> {
                IshaaraLogger.e(TAG, "Authentication expired during location sync. Stopping tracking.")
                stopTracking()
                _status.value = DriverTrackingStatus.Error("Session expired")
            }
            is IshaaraError.Forbidden -> {
                IshaaraLogger.e(TAG, "Driver role forbidden during location sync. Stopping tracking.")
                stopTracking()
                _status.value = DriverTrackingStatus.Error("Driver not verified or access denied")
            }
            is IshaaraError.Validation -> {
                // Out of order or validation rejection from backend: drop fix
                IshaaraLogger.w(TAG, "Backend rejected location fix with validation error: ${error.message}")
            }
            else -> {
                IshaaraLogger.w(TAG, "Transient error during location sync: ${error.message}")
            }
        }
    }

    companion object {
        private const val TAG = "DriverTrackingCoordinator"
    }
}
