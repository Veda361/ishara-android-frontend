package com.ishara.app

import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.core.location.LocationProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.InMemorySessionStore
import com.ishara.app.data.local.datasource.InMemoryRecentDestinationsLocalDataSource
import com.ishara.app.data.local.datasource.SessionLocalDataSourceImpl
import com.ishara.app.data.remote.datasource.LocationRemoteDataSource
import com.ishara.app.data.remote.dto.ResolvedLocationDto
import com.ishara.app.data.repository.LocationRepositoryImpl
import com.ishara.app.domain.model.StudentDestination
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.usecase.SearchLocationsUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates LocationRepository, SearchLocationsUseCase, and RFC 7946 coordinate conventions.
 */
class LocationRepositoryTest {

    private class MockLocationRemoteDataSource(
        var shouldFail: Boolean = false,
        var returnEmpty: Boolean = false
    ) : LocationRemoteDataSource {

        override suspend fun searchLocations(
            query: String,
            latitude: Double?,
            longitude: Double?,
            radius: Double?,
            limit: Int,
            token: String?
        ): IshaaraResult<List<ResolvedLocationDto>> {
            if (shouldFail) {
                return IshaaraResult.failure(IshaaraError.Network("Offline"))
            }
            if (returnEmpty) {
                return IshaaraResult.success(emptyList())
            }

            return IshaaraResult.success(
                listOf(
                    ResolvedLocationDto(
                        latitude = 25.4484,
                        longitude = 78.5685,
                        formattedAddress = "Jhansi Railway Station, Jhansi, Uttar Pradesh",
                        displayName = "Jhansi Railway Station",
                        city = "Jhansi",
                        state = "Uttar Pradesh",
                        country = "India",
                        googlePlaceId = "ChIJ_Jhansi_Station"
                    ),
                    ResolvedLocationDto(
                        latitude = 25.4358,
                        longitude = 78.5833,
                        formattedAddress = "Bundelkhand University, Jhansi, Uttar Pradesh",
                        displayName = "Bundelkhand University",
                        city = "Jhansi",
                        state = "Uttar Pradesh",
                        country = "India",
                        googlePlaceId = "ChIJ_BU_Campus"
                    )
                )
            )
        }
    }

    private class MockLocationProvider(
        var shouldFail: Boolean = false
    ) : LocationProvider {
        override suspend fun getCurrentLocation(): IshaaraResult<LocationCoordinates> {
            if (shouldFail) {
                return IshaaraResult.failure(IshaaraError.Unknown("Location hardware unavailable"))
            }
            return IshaaraResult.success(
                LocationCoordinates(latitude = 25.4484, longitude = 78.5685)
            )
        }

        override fun observeLocationUpdates(intervalMillis: Long): Flow<LocationCoordinates> = emptyFlow()
        override fun isLocationAvailable(): Boolean = !shouldFail
    }

    @Test
    fun test1_searchLocations_successfulResponse_returnsDomainLocations() = runBlocking {
        val remoteDataSource = MockLocationRemoteDataSource()
        val localDataSource = InMemoryRecentDestinationsLocalDataSource()
        val locationProvider = MockLocationProvider()
        val sessionStore = InMemorySessionStore(
            AuthSession(token = "tok", userId = "u1", role = UserRole.USER)
        )
        val sessionLocalDataSource = SessionLocalDataSourceImpl(sessionStore)
        val repository = LocationRepositoryImpl(
            remoteDataSource,
            localDataSource,
            locationProvider,
            sessionLocalDataSource
        )

        val result = repository.searchLocations(query = "Jhansi", limit = 5)

        assertTrue(result.isSuccess)
        val locations = result.getOrNull()
        assertNotNull(locations)
        assertEquals(2, locations?.size)

        val firstLocation = locations?.get(0)
        assertEquals("Jhansi Railway Station", firstLocation?.name)
        assertEquals("Jhansi", firstLocation?.city)
        assertEquals(25.4484, firstLocation?.coordinates?.latitude ?: 0.0, 0.0001)
        assertEquals(78.5685, firstLocation?.coordinates?.longitude ?: 0.0, 0.0001)
    }

    @Test
    fun test2_searchLocations_coordinateConvention_rfc7946Enforced() = runBlocking {
        val remoteDataSource = MockLocationRemoteDataSource()
        val localDataSource = InMemoryRecentDestinationsLocalDataSource()
        val locationProvider = MockLocationProvider()
        val sessionLocalDataSource = SessionLocalDataSourceImpl(InMemorySessionStore())
        val repository = LocationRepositoryImpl(
            remoteDataSource,
            localDataSource,
            locationProvider,
            sessionLocalDataSource
        )

        val result = repository.searchLocations(query = "Jhansi", limit = 5)
        val firstLocation = result.getOrNull()?.get(0)
        assertNotNull(firstLocation)

        // Backend standard strictly specifies [longitude, latitude]
        val geoJson = firstLocation!!.toGeoJson()
        val array = geoJson.toArray()

        assertEquals("First coordinate in GeoJSON MUST be longitude", 78.5685, array[0], 0.0001)
        assertEquals("Second coordinate in GeoJSON MUST be latitude", 25.4484, array[1], 0.0001)
    }

    @Test
    fun test3_searchLocations_emptyResponse_returnsEmptyList() = runBlocking {
        val remoteDataSource = MockLocationRemoteDataSource(returnEmpty = true)
        val localDataSource = InMemoryRecentDestinationsLocalDataSource()
        val locationProvider = MockLocationProvider()
        val sessionLocalDataSource = SessionLocalDataSourceImpl(InMemorySessionStore())
        val repository = LocationRepositoryImpl(
            remoteDataSource,
            localDataSource,
            locationProvider,
            sessionLocalDataSource
        )

        val result = repository.searchLocations(query = "Unknown Place", limit = 5)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isEmpty() == true)
    }

    @Test
    fun test4_searchLocations_remoteFailure_returnsNetworkError() = runBlocking {
        val remoteDataSource = MockLocationRemoteDataSource(shouldFail = true)
        val localDataSource = InMemoryRecentDestinationsLocalDataSource()
        val locationProvider = MockLocationProvider()
        val sessionLocalDataSource = SessionLocalDataSourceImpl(InMemorySessionStore())
        val repository = LocationRepositoryImpl(
            remoteDataSource,
            localDataSource,
            locationProvider,
            sessionLocalDataSource
        )

        val result = repository.searchLocations(query = "Jhansi", limit = 5)

        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.Network)
    }

    @Test
    fun test5_searchUseCase_validatesShortQuery() = runBlocking {
        val remoteDataSource = MockLocationRemoteDataSource()
        val localDataSource = InMemoryRecentDestinationsLocalDataSource()
        val locationProvider = MockLocationProvider()
        val sessionLocalDataSource = SessionLocalDataSourceImpl(InMemorySessionStore())
        val repository = LocationRepositoryImpl(
            remoteDataSource,
            localDataSource,
            locationProvider,
            sessionLocalDataSource
        )
        val useCase = SearchLocationsUseCase(repository)

        val result = useCase(query = "J")

        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.Validation)
        assertEquals("Search query must be at least 2 characters.", result.errorOrNull()?.message)
    }

    @Test
    fun test6_searchUseCase_validatesLongQuery() = runBlocking {
        val remoteDataSource = MockLocationRemoteDataSource()
        val localDataSource = InMemoryRecentDestinationsLocalDataSource()
        val locationProvider = MockLocationProvider()
        val sessionLocalDataSource = SessionLocalDataSourceImpl(InMemorySessionStore())
        val repository = LocationRepositoryImpl(
            remoteDataSource,
            localDataSource,
            locationProvider,
            sessionLocalDataSource
        )
        val useCase = SearchLocationsUseCase(repository)

        val longQuery = "A".repeat(101)
        val result = useCase(query = longQuery)

        assertTrue(result.isFailure)
        assertTrue(result.errorOrNull() is IshaaraError.Validation)
        assertEquals("Search query cannot exceed 100 characters.", result.errorOrNull()?.message)
    }

    @Test
    fun test7_recentDestinations_cachesAndCapsAtFive() = runBlocking {
        val localDataSource = InMemoryRecentDestinationsLocalDataSource()

        for (i in 1..7) {
            localDataSource.saveDestination(
                StudentDestination(
                    name = "Stop $i",
                    formattedAddress = "Address $i",
                    coordinates = LocationCoordinates(latitude = 25.0 + i * 0.01, longitude = 78.0)
                )
            )
        }

        val recents = localDataSource.getRecentDestinations().first()

        assertEquals("Recent destinations must be capped at 5", 5, recents.size)
        assertEquals("Most recently saved item must be first", "Stop 7", recents[0].name)
        assertEquals("Second item must be Stop 6", "Stop 6", recents[1].name)
    }

    @Test
    fun test8_recentDestinations_clear_emptiesList() = runBlocking {
        val localDataSource = InMemoryRecentDestinationsLocalDataSource()

        localDataSource.saveDestination(
            StudentDestination(
                name = "Campus Gate",
                formattedAddress = "Main Road",
                coordinates = LocationCoordinates(latitude = 25.44, longitude = 78.56)
            )
        )

        assertEquals(1, localDataSource.getRecentDestinations().first().size)

        localDataSource.clear()

        assertTrue(localDataSource.getRecentDestinations().first().isEmpty())
    }
}
