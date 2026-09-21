package com.ishara.app.core.location

import com.ishara.app.core.result.IshaaraResult
import kotlinx.coroutines.flow.Flow

/**
 * Clean architectural abstraction for location services.
 * Keeps FusedLocationProviderClient and hardware GPS implementations decoupled from UI/features.
 */
interface LocationProvider {
    /**
     * Retrieves the single most accurate current location fix.
     */
    suspend fun getCurrentLocation(): IshaaraResult<LocationCoordinates>

    /**
     * Observes continuous location stream at specified interval (for driver route tracking).
     */
    fun observeLocationUpdates(intervalMillis: Long): Flow<LocationCoordinates>

    /**
     * Returns true if device location hardware and permissions are available.
     */
    fun isLocationAvailable(): Boolean
}
