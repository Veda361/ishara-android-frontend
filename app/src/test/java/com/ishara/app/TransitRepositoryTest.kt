package com.ishara.app

import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.local.datasource.InMemoryTransitLocalDataSource
import com.ishara.app.data.remote.datasource.TransitRemoteDataSource
import com.ishara.app.data.remote.dto.TransitTripDriverDto
import com.ishara.app.data.remote.dto.TransitTripDto
import com.ishara.app.data.remote.dto.TransitTripLocationDto
import com.ishara.app.data.remote.dto.TransitTripRouteDto
import com.ishara.app.data.remote.dto.TransitTripVehicleDto
import com.ishara.app.data.repository.TransitRepositoryImpl
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.TransitCacheFreshness
import com.ishara.app.domain.model.TransitCachePolicy
import com.ishara.app.domain.model.UserRole
import com.ishara.app.domain.repository.TransitResource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitRepositoryTest {

    private class TestDispatcherProvider(
        override val main: CoroutineDispatcher = Dispatchers.Unconfined,
        override val io: CoroutineDispatcher = Dispatchers.Unconfined,
        override val default: CoroutineDispatcher = Dispatchers.Unconfined,
        override val unconfined: CoroutineDispatcher = Dispatchers.Unconfined
    ) : DispatcherProvider

    private val fakeSessionStore = object : SessionStore {
        private var session: AuthSession? = AuthSession(
            token = "test_jwt_token",
            userId = "user_123",
            role = UserRole.USER
        )
        override suspend fun saveSession(session: AuthSession) { this.session = session }
        override suspend fun getSession(): AuthSession? = session
        override suspend fun clearSession() { session = null }
        override fun observeSession(): Flow<AuthSession?> = emptyFlow()
    }

    private val fakeTripDto = TransitTripDto(
        id = "trip_001",
        status = "ACTIVE",
        origin = TransitTripLocationDto(
            name = "Campus Gate",
            formattedAddress = "Campus Gate Road",
            coordinates = doubleArrayOf(78.58, 25.44)
        ),
        destination = TransitTripLocationDto(
            name = "Railway Station",
            formattedAddress = "Station Road",
            coordinates = doubleArrayOf(78.60, 25.46)
        ),
        route = TransitTripRouteDto(
            geometryCoordinates = listOf(doubleArrayOf(78.58, 25.44), doubleArrayOf(78.60, 25.46)),
            distanceMeters = 4000.0,
            durationSeconds = 720L,
            provider = "google_routes"
        ),
        startedAt = "2026-09-27T10:00:00.000Z",
        createdAt = "2026-09-27T09:50:00.000Z",
        driver = TransitTripDriverDto(id = "d1", name = "Driver One"),
        vehicle = TransitTripVehicleDto(
            id = "v1",
            registrationNumber = "UP93B1234",
            vehicleType = "BUS",
            make = "Tata",
            model = "Starbus"
        )
    )

    private class FakeTransitRemoteDataSource : TransitRemoteDataSource {
        var resultToReturn: IshaaraResult<List<TransitTripDto>> = IshaaraResult.success(emptyList())

        override suspend fun listActiveTrips(token: String?): IshaaraResult<List<TransitTripDto>> {
            return resultToReturn
        }

        override suspend fun getTripById(tripId: String, token: String?): IshaaraResult<TransitTripDto> {
            val list = (resultToReturn as? IshaaraResult.Success)?.data ?: emptyList()
            val found = list.find { it.id == tripId }
            return if (found != null) {
                IshaaraResult.success(found)
            } else {
                IshaaraResult.failure(IshaaraError.Network("Trip not found"))
            }
        }
    }

    @Test
    fun testGetRoutesInitialNoCacheSuccessfulFetch() = runBlocking {
        val fakeRemote = FakeTransitRemoteDataSource()
        fakeRemote.resultToReturn = IshaaraResult.success(listOf(fakeTripDto))
        val localDataSource = InMemoryTransitLocalDataSource()

        val repository = TransitRepositoryImpl(
            remoteDataSource = fakeRemote,
            localDataSource = localDataSource,
            sessionStore = fakeSessionStore,
            dispatchers = TestDispatcherProvider()
        )

        val emissions = repository.getRoutes(forceRefresh = false).toList()
        assertEquals(2, emissions.size)

        // 1. Loading state with no cache
        assertTrue(emissions[0] is TransitResource.Loading)
        assertEquals(null, (emissions[0] as TransitResource.Loading).cachedData)

        // 2. Success state with fresh routes
        assertTrue(emissions[1] is TransitResource.Success)
        val success = emissions[1] as TransitResource.Success
        assertEquals(1, success.data.size)
        assertEquals("trip_001", success.data[0].id)
        assertEquals(TransitCacheFreshness.FRESH, success.freshness)

        // Verify saved in local cache
        val cached = localDataSource.getRoutes()
        assertEquals(1, cached.size)
        assertEquals("trip_001", cached[0].id)
    }

    @Test
    fun testGetRoutesStaleWhileRevalidate() = runBlocking {
        val fakeRemote = FakeTransitRemoteDataSource()
        fakeRemote.resultToReturn = IshaaraResult.success(listOf(fakeTripDto))
        val localDataSource = InMemoryTransitLocalDataSource()

        // Populate stale cache (timestamp 1 hour ago)
        val policy = TransitCachePolicy(freshnessDurationMillis = 5 * 60 * 1000L) // 5 min freshness
        val oneHourAgo = System.currentTimeMillis() - (60 * 60 * 1000L)
        val oldRoute = com.ishara.app.data.mapper.TransitMapper.toDomain(fakeTripDto, cachedAtMillis = oneHourAgo)
        localDataSource.saveRoutes(listOf(oldRoute), refreshedAtMillis = oneHourAgo)

        val repository = TransitRepositoryImpl(
            remoteDataSource = fakeRemote,
            localDataSource = localDataSource,
            sessionStore = fakeSessionStore,
            dispatchers = TestDispatcherProvider(),
            cachePolicy = policy
        )

        val emissions = repository.getRoutes(forceRefresh = false).toList()

        // Emissions: 1. Success(cached, STALE), 2. Loading(cached), 3. Success(fresh)
        assertEquals(3, emissions.size)

        assertTrue(emissions[0] is TransitResource.Success)
        assertEquals(TransitCacheFreshness.STALE, (emissions[0] as TransitResource.Success).freshness)

        assertTrue(emissions[1] is TransitResource.Loading)
        assertNotNull((emissions[1] as TransitResource.Loading).cachedData)

        assertTrue(emissions[2] is TransitResource.Success)
        assertEquals(TransitCacheFreshness.FRESH, (emissions[2] as TransitResource.Success).freshness)
    }

    @Test
    fun testGetRoutesOfflineWithCachedDataPreserved() = runBlocking {
        val fakeRemote = FakeTransitRemoteDataSource()
        fakeRemote.resultToReturn = IshaaraResult.failure(IshaaraError.Network("No internet connection"))
        val localDataSource = InMemoryTransitLocalDataSource()

        // Populate local cache
        val cachedRoute = com.ishara.app.data.mapper.TransitMapper.toDomain(fakeTripDto, cachedAtMillis = 1000L)
        localDataSource.saveRoutes(listOf(cachedRoute), refreshedAtMillis = 1000L)

        val repository = TransitRepositoryImpl(
            remoteDataSource = fakeRemote,
            localDataSource = localDataSource,
            sessionStore = fakeSessionStore,
            dispatchers = TestDispatcherProvider()
        )

        // Force refresh while offline
        val emissions = repository.getRoutes(forceRefresh = true).toList()
        assertEquals(3, emissions.size)

        // 1. Success with cached data
        assertTrue(emissions[0] is TransitResource.Success)

        // 2. Loading
        assertTrue(emissions[1] is TransitResource.Loading)

        // 3. Error with cached data preserved and isOffline = true
        assertTrue(emissions[2] is TransitResource.Error)
        val errorState = emissions[2] as TransitResource.Error
        assertTrue(errorState.isOffline)
        assertNotNull(errorState.cachedData)
        assertEquals(1, errorState.cachedData?.size)
    }

    @Test
    fun testGetRoutesOfflineWithNoCache() = runBlocking {
        val fakeRemote = FakeTransitRemoteDataSource()
        fakeRemote.resultToReturn = IshaaraResult.failure(IshaaraError.Network("Offline"))
        val localDataSource = InMemoryTransitLocalDataSource()

        val repository = TransitRepositoryImpl(
            remoteDataSource = fakeRemote,
            localDataSource = localDataSource,
            sessionStore = fakeSessionStore,
            dispatchers = TestDispatcherProvider()
        )

        val emissions = repository.getRoutes(forceRefresh = false).toList()
        assertEquals(2, emissions.size)

        assertTrue(emissions[0] is TransitResource.Loading)
        assertTrue(emissions[1] is TransitResource.Error)
        val err = emissions[1] as TransitResource.Error
        assertTrue(err.isOffline)
        assertEquals(null, err.cachedData)
    }

    @Test
    fun testGetRouteByIdFlow() = runBlocking {
        val fakeRemote = FakeTransitRemoteDataSource()
        fakeRemote.resultToReturn = IshaaraResult.success(listOf(fakeTripDto))
        val localDataSource = InMemoryTransitLocalDataSource()

        val repository = TransitRepositoryImpl(
            remoteDataSource = fakeRemote,
            localDataSource = localDataSource,
            sessionStore = fakeSessionStore,
            dispatchers = TestDispatcherProvider()
        )

        val emissions = repository.getRouteById("trip_001", forceRefresh = false).toList()
        assertTrue(emissions.last() is TransitResource.Success)
        val success = emissions.last() as TransitResource.Success
        assertEquals("trip_001", success.data.id)
    }

    @Test
    fun testClearCache() = runBlocking {
        val localDataSource = InMemoryTransitLocalDataSource()
        val cachedRoute = com.ishara.app.data.mapper.TransitMapper.toDomain(fakeTripDto)
        localDataSource.saveRoutes(listOf(cachedRoute), refreshedAtMillis = 1000L)

        val repository = TransitRepositoryImpl(
            remoteDataSource = FakeTransitRemoteDataSource(),
            localDataSource = localDataSource,
            sessionStore = fakeSessionStore,
            dispatchers = TestDispatcherProvider()
        )

        assertEquals(1, localDataSource.getRoutes().size)
        repository.clearCache()
        assertEquals(0, localDataSource.getRoutes().size)
    }
}
