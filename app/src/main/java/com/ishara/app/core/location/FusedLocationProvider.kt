package com.ishara.app.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Production FusedLocationProvider implementing clean [LocationProvider] abstraction.
 *
 * Guarantees:
 * 1. Strictly maps Android [Location] to domain [LocationCoordinates] without leaking platform types.
 * 2. Emits location updates using Kotlin Coroutines [callbackFlow].
 * 3. Safely unregisters callbacks on cancellation to prevent memory leaks and battery drain.
 * 4. Contextual runtime permission and hardware GPS checks.
 *
 * Notice: Interval and displacement configurations are client-side production defaults.
 * CLIENT CONFIGURATION — NOT BACKEND CONTRACT.
 */
class FusedLocationProvider(
    private val context: Context,
    private val fusedClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context),
    private val minDisplacementMeters: Float = DEFAULT_MIN_DISPLACEMENT_METERS
) : LocationProvider {

    override fun isLocationAvailable(): Boolean {
        val hasPermission = hasLocationPermission()
        val isHardwareEnabled = isGpsOrNetworkEnabled()
        IshaaraLogger.d(TAG, "isLocationAvailable: permission=${if (hasPermission) "GRANTED" else "DENIED"} servicesEnabled=$isHardwareEnabled")
        return hasPermission && isHardwareEnabled
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): IshaaraResult<LocationCoordinates> {
        val hasPermission = hasLocationPermission()
        val servicesEnabled = isGpsOrNetworkEnabled()
        IshaaraLogger.d(TAG, "permission=${if (hasPermission) "GRANTED" else "DENIED"} servicesEnabled=$servicesEnabled")

        if (!hasPermission) {
            return IshaaraResult.failure(IshaaraError.PermissionDenied("Location permission not granted."))
        }
        if (!servicesEnabled) {
            return IshaaraResult.failure(IshaaraError.LocationDisabled("Device location services are disabled."))
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location: Location? ->
                        if (location != null) {
                            val coords = location.toDomainCoordinates()
                            if (coords.isValid()) {
                                logLocationDiagnostic(location)
                                continuation.resume(IshaaraResult.success(coords))
                                return@addOnSuccessListener
                            }
                        }

                        // Fallback to last known location if immediate fix is null or invalid
                        fusedClient.lastLocation
                            .addOnSuccessListener { lastLoc: Location? ->
                                if (lastLoc != null) {
                                    val coords = lastLoc.toDomainCoordinates()
                                    if (coords.isValid()) {
                                        logLocationDiagnostic(lastLoc)
                                        continuation.resume(IshaaraResult.success(coords))
                                        return@addOnSuccessListener
                                    }
                                }

                                // Fallback: try one-shot location update
                                requestOneShotLocation(continuation)
                            }
                            .addOnFailureListener { e ->
                                IshaaraLogger.w(TAG, "Failed to retrieve last location: ${e.message}")
                                requestOneShotLocation(continuation)
                            }
                    }
                    .addOnFailureListener { e ->
                        IshaaraLogger.w(TAG, "Current location request failed: ${e.message}")
                        requestOneShotLocation(continuation)
                    }
            } catch (e: SecurityException) {
                IshaaraLogger.e(TAG, "SecurityException getting current location", e)
                continuation.resume(
                    IshaaraResult.failure(IshaaraError.PermissionDenied("SecurityException: ${e.message}"))
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestOneShotLocation(
        continuation: kotlin.coroutines.Continuation<IshaaraResult<LocationCoordinates>>
    ) {
        val oneShotRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
            .setMaxUpdates(1)
            .setDurationMillis(8_000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                fusedClient.removeLocationUpdates(this)
                val loc = result.lastLocation
                if (loc != null) {
                    val coords = loc.toDomainCoordinates()
                    if (coords.isValid()) {
                        logLocationDiagnostic(loc)
                        if (continuation.context[kotlinx.coroutines.Job]?.isActive == true) {
                            continuation.resume(IshaaraResult.success(coords))
                        }
                        return
                    }
                }
                if (continuation.context[kotlinx.coroutines.Job]?.isActive == true) {
                    continuation.resume(
                        IshaaraResult.failure(IshaaraError.Network("Unable to resolve GPS location fix."))
                    )
                }
            }
        }

        try {
            fusedClient.requestLocationUpdates(oneShotRequest, callback, Looper.getMainLooper())
        } catch (e: Exception) {
            IshaaraLogger.e(TAG, "One-shot location request failed", e)
            if (continuation.context[kotlinx.coroutines.Job]?.isActive == true) {
                continuation.resume(
                    IshaaraResult.failure(IshaaraError.Network("Location request failed: ${e.message}"))
                )
            }
        }
    }

    private fun logLocationDiagnostic(loc: Location) {
        val ageMs = (System.currentTimeMillis() - loc.time).coerceAtLeast(0L)
        IshaaraLogger.d(
            TAG,
            "permission=GRANTED servicesEnabled=true accuracy=${loc.accuracy} latitude=${loc.latitude} longitude=${loc.longitude} ageMs=$ageMs provider=${loc.provider ?: "fused"}"
        )
    }

    @SuppressLint("MissingPermission")
    override fun observeLocationUpdates(intervalMillis: Long): Flow<LocationCoordinates> = callbackFlow {
        if (!hasLocationPermission()) {
            IshaaraLogger.w(TAG, "Location permission missing when observing updates.")
            close(SecurityException("Location permission missing."))
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis / 2)
            .setMinUpdateDistanceMeters(minDisplacementMeters)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    val coords = location.toDomainCoordinates()
                    if (coords.isValid()) {
                        logLocationDiagnostic(location)
                        trySend(coords)
                    } else {
                        IshaaraLogger.w(TAG, "Dropping invalid coordinates fix: lat=${coords.latitude} lng=${coords.longitude}")
                    }
                }
            }
        }

        try {
            fusedClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
            IshaaraLogger.i(TAG, "Registered FusedLocationProvider updates (interval: ${intervalMillis}ms).")
        } catch (e: SecurityException) {
            IshaaraLogger.e(TAG, "SecurityException requesting location updates", e)
            close(e)
        }

        awaitClose {
            IshaaraLogger.i(TAG, "Removing FusedLocationProvider updates.")
            fusedClient.removeLocationUpdates(callback)
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    private fun isGpsOrNetworkEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        val gpsEnabled = try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            false
        }
        val networkEnabled = try {
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
        return gpsEnabled || networkEnabled
    }

    companion object {
        private const val TAG = "ISHAARA_LOCATION"

        /**
         * Conservative default minimum displacement filter to prevent battery drain while stopped.
         * CLIENT CONFIGURATION — NOT BACKEND CONTRACT.
         */
        const val DEFAULT_MIN_DISPLACEMENT_METERS = 5.0f

        /**
         * Conservative default active trip sampling interval (4 seconds).
         * CLIENT CONFIGURATION — NOT BACKEND CONTRACT.
         */
        const val DEFAULT_ACTIVE_INTERVAL_MILLIS = 4_000L

        /**
         * Extension mapping Android [Location] to pure domain [LocationCoordinates].
         */
        fun Location.toDomainCoordinates(): LocationCoordinates = LocationCoordinates(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = if (hasAccuracy()) accuracy else null,
            headingDegrees = if (hasBearing()) bearing else null,
            speedMps = if (hasSpeed()) speed else null,
            timestampMillis = time
        )
    }
}
