package com.ishara.app.domain.repository

import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.CurrentLocationDisplay
import com.ishara.app.domain.model.SearchResultLocation
import com.ishara.app.domain.model.StudentDestination
import kotlinx.coroutines.flow.Flow

/**
 * Domain repository contract for location discovery, geocoding, and recent search caching.
 */
interface LocationRepository {
    /**
     * Searches for places and landmarks via backend geo-orchestrator.
     * Enforces min 2 characters query.
     */
    suspend fun searchLocations(
        query: String,
        latitude: Double? = null,
        longitude: Double? = null,
        radius: Double? = null,
        limit: Int = 5
    ): IshaaraResult<List<SearchResultLocation>>

    /**
     * Resolves the current physical device coordinates.
     */
    suspend fun getCurrentLocation(): IshaaraResult<LocationCoordinates>

    /**
     * Resolves coordinates to human-readable place description.
     */
    suspend fun resolveHumanReadableLocation(
        coordinates: LocationCoordinates
    ): IshaaraResult<CurrentLocationDisplay>

    /**
     * Observes locally cached recent destinations in descending order of usage.
     */
    fun getRecentDestinations(): Flow<List<StudentDestination>>

    /**
     * Saves a destination to recent local history (max 5 items, privacy-preserving).
     */
    suspend fun saveRecentDestination(destination: StudentDestination): IshaaraResult<Unit>

    /**
     * Clears all locally cached recent destinations.
     */
    suspend fun clearRecentDestinations(): IshaaraResult<Unit>
}
