package com.ishara.app.data.repository

import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.location.LocationProvider
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.RecentDestinationsLocalDataSource
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.mapper.LocationMapper
import com.ishara.app.data.remote.datasource.LocationRemoteDataSource
import com.ishara.app.domain.model.CurrentLocationDisplay
import com.ishara.app.domain.model.SearchResultLocation
import com.ishara.app.domain.model.StudentDestination
import com.ishara.app.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of LocationRepository coordinating remote searches,
 * device GPS provider, and local recent searches cache.
 */
class LocationRepositoryImpl(
    private val remoteDataSource: LocationRemoteDataSource,
    private val localDataSource: RecentDestinationsLocalDataSource,
    private val locationProvider: LocationProvider,
    private val sessionLocalDataSource: SessionLocalDataSource
) : LocationRepository {

    override suspend fun searchLocations(
        query: String,
        latitude: Double?,
        longitude: Double?,
        radius: Double?,
        limit: Int
    ): IshaaraResult<List<SearchResultLocation>> {
        val token = sessionLocalDataSource.getSession()?.token
        return remoteDataSource.searchLocations(
            query = query,
            latitude = latitude,
            longitude = longitude,
            radius = radius,
            limit = limit,
            token = token
        ).map { dtos ->
            LocationMapper.toDomainList(dtos)
        }
    }

    override suspend fun getCurrentLocation(): IshaaraResult<LocationCoordinates> {
        return locationProvider.getCurrentLocation()
    }

    override suspend fun resolveHumanReadableLocation(
        coordinates: LocationCoordinates
    ): IshaaraResult<CurrentLocationDisplay> {
        // Formats coordinates into a human-friendly city/area display
        // Guards against raw latitude/longitude exposure
        val title = "Current location"
        val subtitle = resolveAreaFromCoordinates(coordinates.latitude, coordinates.longitude)

        return IshaaraResult.success(
            CurrentLocationDisplay(
                title = title,
                subtitle = subtitle,
                coordinates = coordinates
            )
        )
    }

    override fun getRecentDestinations(): Flow<List<StudentDestination>> {
        return localDataSource.getRecentDestinations()
    }

    override suspend fun saveRecentDestination(destination: StudentDestination): IshaaraResult<Unit> {
        localDataSource.saveDestination(destination)
        return IshaaraResult.success(Unit)
    }

    override suspend fun clearRecentDestinations(): IshaaraResult<Unit> {
        localDataSource.clear()
        return IshaaraResult.success(Unit)
    }

    /**
     * Resolves a known area or city for regional bounds, or a clean contextual string.
     */
    private fun resolveAreaFromCoordinates(lat: Double, lng: Double): String {
        // Bundelkhand University / Jhansi region bounds
        return if (lat in 25.35..25.55 && lng in 78.45..78.68) {
            "Jhansi, Uttar Pradesh"
        } else if (lat in 18.40..18.70 && lng in 73.70..74.05) {
            "Pune, Maharashtra"
        } else {
            "Campus Region"
        }
    }
}
