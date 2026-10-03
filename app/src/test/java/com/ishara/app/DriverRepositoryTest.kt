package com.ishara.app

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.mapper.DriverMapper
import com.ishara.app.data.remote.datasource.DriverRemoteDataSourceImpl
import com.ishara.app.data.remote.dto.DriverGeoJsonPointDto
import com.ishara.app.data.remote.dto.DriverTripLocationDto
import com.ishara.app.data.repository.DriverRepositoryImpl
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.DriverProfileStatus
import com.ishara.app.domain.model.DriverTripStatus
import com.ishara.app.domain.model.DriverVerificationStatus
import com.ishara.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriverRepositoryTest {

    private class MockHttpClient(
        var responseToReturn: IshaaraResult<HttpResponse>
    ) : IshaaraHttpClient {
        var lastRequest: HttpRequest? = null

        override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
            lastRequest = request
            return responseToReturn
        }
    }

    private class MockSessionLocalDataSource(
        var sessionToReturn: AuthSession?
    ) : SessionLocalDataSource {
        override suspend fun saveSession(session: AuthSession) {}
        override suspend fun getSession(): AuthSession? = sessionToReturn
        override fun observeSession(): Flow<AuthSession?> = flowOf(sessionToReturn)
        override suspend fun clearSession() {}
    }

    private val validSession = AuthSession(
        token = "test_driver_bearer_token",
        userId = "driver_usr_123",
        role = UserRole.DRIVER_CONDUCTOR
    )

    private val sampleContextJson = """
    {
        "success": true,
        "statusCode": 200,
        "data": {
            "driver": {
                "id": "651a2b3c4d5e6f7a8b9c0d1e",
                "userId": "driver_usr_123",
                "verificationStatus": "VERIFIED",
                "status": "ONLINE",
                "licenseNumberMasked": "DL••••••••1234",
                "licenseVerifiedAt": "2026-09-01T10:00:00.000Z"
            },
            "vehicle": {
                "id": "veh_123",
                "registrationNumber": "UP93AT1234",
                "vehicleType": "BUS",
                "make": "Tata",
                "model": "Starbus",
                "isActive": true,
                "isVerified": true
            },
            "activeTrip": null,
            "activeRidesCount": 0,
            "todayStats": {
                "completedRidesCount": 5,
                "isOnline": true,
                "currentDate": "2026-09-24",
                "timezone": "Asia/Kolkata"
            }
        }
    }
    """.trimIndent()

    private val sampleTripJson = """
    {
        "success": true,
        "statusCode": 200,
        "data": {
            "id": "651a2b3c4d5e6f7a8b9c0d30",
            "driverId": "651a2b3c4d5e6f7a8b9c0d1e",
            "vehicleId": "veh_123",
            "origin": {
                "formattedAddress": "Gate A, University",
                "coordinates": {
                    "type": "Point",
                    "coordinates": [78.5685, 25.4484]
                }
            },
            "destination": {
                "formattedAddress": "Railway Station",
                "coordinates": {
                    "type": "Point",
                    "coordinates": [78.5780, 25.4520]
                }
            },
            "status": "ACTIVE"
        }
    }
    """.trimIndent()

    @Test
    fun getOperationalContext_success_callsCorrectEndpointWithBearerHeader() = runBlocking {
        val mockHttpClient = MockHttpClient(
            IshaaraResult.success(HttpResponse(statusCode = 200, headers = emptyMap(), body = sampleContextJson))
        )
        val mockSessionStore = MockSessionLocalDataSource(validSession)
        val remoteDataSource = DriverRemoteDataSourceImpl(mockHttpClient, NetworkConfig())
        val repository = DriverRepositoryImpl(remoteDataSource, mockSessionStore)

        val result = repository.getOperationalContext(timezone = "Asia/Kolkata")

        assertTrue(result is IshaaraResult.Success)
        val context = (result as IshaaraResult.Success).data
        assertNotNull(context)
        assertEquals("651a2b3c4d5e6f7a8b9c0d1e", context.driver.id)
        assertEquals(DriverVerificationStatus.VERIFIED, context.driver.verificationStatus)
        assertEquals(DriverProfileStatus.ONLINE, context.driver.status)

        // Verify request details
        val req = mockHttpClient.lastRequest
        assertNotNull(req)
        assertEquals(HttpMethod.GET, req?.method)
        assertTrue(req?.url?.contains("/drivers/me/operations/context") == true)
        assertEquals("Bearer test_driver_bearer_token", req?.headers?.get("Authorization"))
        assertEquals("Asia/Kolkata", req?.queryParams?.get("timezone"))
    }

    @Test
    fun getOperationalContext_withoutActiveSession_failsWithAuthenticationError() = runBlocking {
        val mockHttpClient = MockHttpClient(
            IshaaraResult.success(HttpResponse(statusCode = 200, headers = emptyMap(), body = sampleContextJson))
        )
        val mockSessionStore = MockSessionLocalDataSource(null)
        val remoteDataSource = DriverRemoteDataSourceImpl(mockHttpClient, NetworkConfig())
        val repository = DriverRepositoryImpl(remoteDataSource, mockSessionStore)

        val result = repository.getOperationalContext()

        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Authentication)
        assertEquals("No active session.", error.message)
    }

    @Test
    fun setOnline_success_executesPostToStatusOnline() = runBlocking {
        val profileJson = """{"id": "651a2b3c4d5e6f7a8b9c0d1e", "status": "ONLINE", "verificationStatus": "VERIFIED"}"""
        val mockHttpClient = MockHttpClient(
            IshaaraResult.success(HttpResponse(statusCode = 200, headers = emptyMap(), body = profileJson))
        )
        val repository = DriverRepositoryImpl(
            DriverRemoteDataSourceImpl(mockHttpClient, NetworkConfig()),
            MockSessionLocalDataSource(validSession)
        )

        val result = repository.setOnline()

        assertTrue(result is IshaaraResult.Success)
        val driver = (result as IshaaraResult.Success).data
        assertEquals(DriverProfileStatus.ONLINE, driver.status)

        val req = mockHttpClient.lastRequest
        assertNotNull(req)
        assertEquals(HttpMethod.POST, req?.method)
        assertTrue(req?.url?.endsWith("/drivers/me/status/online") == true)
        assertEquals("Bearer test_driver_bearer_token", req?.headers?.get("Authorization"))
        assertEquals("{}", req?.body)
    }

    @Test
    fun setOffline_success_executesPostToStatusOffline() = runBlocking {
        val profileJson = """{"id": "651a2b3c4d5e6f7a8b9c0d1e", "status": "OFFLINE", "verificationStatus": "VERIFIED"}"""
        val mockHttpClient = MockHttpClient(
            IshaaraResult.success(HttpResponse(statusCode = 200, headers = emptyMap(), body = profileJson))
        )
        val repository = DriverRepositoryImpl(
            DriverRemoteDataSourceImpl(mockHttpClient, NetworkConfig()),
            MockSessionLocalDataSource(validSession)
        )

        val result = repository.setOffline()

        assertTrue(result is IshaaraResult.Success)
        val driver = (result as IshaaraResult.Success).data
        assertEquals(DriverProfileStatus.OFFLINE, driver.status)

        val req = mockHttpClient.lastRequest
        assertNotNull(req)
        assertEquals(HttpMethod.POST, req?.method)
        assertTrue(req?.url?.endsWith("/drivers/me/status/offline") == true)
        assertEquals("Bearer test_driver_bearer_token", req?.headers?.get("Authorization"))
        assertEquals("{}", req?.body)
    }

    @Test
    fun startTrip_success_executesPostToTripStart() = runBlocking {
        val mockHttpClient = MockHttpClient(
            IshaaraResult.success(HttpResponse(statusCode = 200, headers = emptyMap(), body = sampleTripJson))
        )
        val repository = DriverRepositoryImpl(
            DriverRemoteDataSourceImpl(mockHttpClient, NetworkConfig()),
            MockSessionLocalDataSource(validSession)
        )

        val tripId = "651a2b3c4d5e6f7a8b9c0d30"
        val result = repository.startTrip(tripId)

        assertTrue(result is IshaaraResult.Success)
        val trip = (result as IshaaraResult.Success).data
        assertEquals(DriverTripStatus.ACTIVE, trip.status)

        val req = mockHttpClient.lastRequest
        assertNotNull(req)
        assertEquals(HttpMethod.POST, req?.method)
        assertTrue(req?.url?.endsWith("/trips/$tripId/start") == true)
        assertEquals("Bearer test_driver_bearer_token", req?.headers?.get("Authorization"))
    }

    @Test
    fun completeTrip_success_executesPostToTripComplete() = runBlocking {
        val completedJson = """{"id": "651a2b3c4d5e6f7a8b9c0d30", "status": "COMPLETED"}"""
        val mockHttpClient = MockHttpClient(
            IshaaraResult.success(HttpResponse(statusCode = 200, headers = emptyMap(), body = completedJson))
        )
        val repository = DriverRepositoryImpl(
            DriverRemoteDataSourceImpl(mockHttpClient, NetworkConfig()),
            MockSessionLocalDataSource(validSession)
        )

        val tripId = "651a2b3c4d5e6f7a8b9c0d30"
        val result = repository.completeTrip(tripId)

        assertTrue(result is IshaaraResult.Success)
        val trip = (result as IshaaraResult.Success).data
        assertEquals(DriverTripStatus.COMPLETED, trip.status)

        val req = mockHttpClient.lastRequest
        assertNotNull(req)
        assertEquals(HttpMethod.POST, req?.method)
        assertTrue(req?.url?.endsWith("/trips/$tripId/complete") == true)
    }

    @Test
    fun cancelTrip_success_executesPostToTripCancel() = runBlocking {
        val cancelledJson = """{"id": "651a2b3c4d5e6f7a8b9c0d30", "status": "CANCELLED"}"""
        val mockHttpClient = MockHttpClient(
            IshaaraResult.success(HttpResponse(statusCode = 200, headers = emptyMap(), body = cancelledJson))
        )
        val repository = DriverRepositoryImpl(
            DriverRemoteDataSourceImpl(mockHttpClient, NetworkConfig()),
            MockSessionLocalDataSource(validSession)
        )

        val tripId = "651a2b3c4d5e6f7a8b9c0d30"
        val result = repository.cancelTrip(tripId)

        assertTrue(result is IshaaraResult.Success)
        val trip = (result as IshaaraResult.Success).data
        assertEquals(DriverTripStatus.CANCELLED, trip.status)

        val req = mockHttpClient.lastRequest
        assertNotNull(req)
        assertEquals(HttpMethod.POST, req?.method)
        assertTrue(req?.url?.endsWith("/trips/$tripId/cancel") == true)
    }

    @Test
    fun forbiddenError_isPropagatedAccurately() = runBlocking {
        val mockHttpClient = MockHttpClient(
            IshaaraResult.failure(IshaaraError.Forbidden("Access denied: Required role 'DRIVER_CONDUCTOR'."))
        )
        val repository = DriverRepositoryImpl(
            DriverRemoteDataSourceImpl(mockHttpClient, NetworkConfig()),
            MockSessionLocalDataSource(validSession)
        )

        val result = repository.getOperationalContext()

        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Forbidden)
        assertEquals("Access denied: Required role 'DRIVER_CONDUCTOR'.", error.message)
    }

    @Test
    fun conflictError_isPropagatedAccurately() = runBlocking {
        val mockHttpClient = MockHttpClient(
            IshaaraResult.failure(IshaaraError.Conflict("Driver already has an active trip."))
        )
        val repository = DriverRepositoryImpl(
            DriverRemoteDataSourceImpl(mockHttpClient, NetworkConfig()),
            MockSessionLocalDataSource(validSession)
        )

        val result = repository.startTrip("651a2b3c4d5e6f7a8b9c0d30")

        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Conflict)
        assertEquals("Driver already has an active trip.", error.message)
    }

    @Test
    fun getOperationalContext_whenNotFound_provisionsProfileAndRetriesSuccessfully() = runBlocking {
        var callCount = 0
        val sequentialHttpClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                return when {
                    request.url.contains("/drivers/me/operations/context") && callCount == 0 -> {
                        callCount++
                        IshaaraResult.failure(IshaaraError.NotFound("Driver profile not found. Please complete driver onboarding first."))
                    }
                    request.url.contains("/drivers/me/profile") && request.method == HttpMethod.POST -> {
                        IshaaraResult.success(HttpResponse(201, emptyMap(), """{"success":true,"data":{"id":"dp-1","userId":"driver-1","status":"OFFLINE","verificationStatus":"PENDING"}}"""))
                    }
                    request.url.contains("/drivers/me/operations/context") && callCount > 0 -> {
                        IshaaraResult.success(HttpResponse(200, emptyMap(), sampleContextJson))
                    }
                    else -> IshaaraResult.failure(IshaaraError.Unknown("Unexpected request: ${request.url}"))
                }
            }
        }

        val repository = DriverRepositoryImpl(
            DriverRemoteDataSourceImpl(sequentialHttpClient, NetworkConfig()),
            MockSessionLocalDataSource(validSession),
            autoProvisionOnNotFound = true
        )

        val result = repository.getOperationalContext()
        assertTrue(result is IshaaraResult.Success)
        val context = (result as IshaaraResult.Success).data
        assertEquals("651a2b3c4d5e6f7a8b9c0d1e", context.driver.id)
    }

    @Test
    fun getOperationalContext_whenNotFound_defaultAutoProvisionFalse_returnsNotFoundError() = runBlocking {
        val notFoundHttpClient = object : IshaaraHttpClient {
            override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
                return IshaaraResult.failure(IshaaraError.NotFound("Driver operations context not found"))
            }
        }

        val repository = DriverRepositoryImpl(
            DriverRemoteDataSourceImpl(notFoundHttpClient, NetworkConfig()),
            MockSessionLocalDataSource(validSession)
            // autoProvisionOnNotFound defaults to false
        )

        val result = repository.getOperationalContext()
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.NotFound)
    }

    @Test
    fun driverMapper_strictlyMapsGeoJsonCoordinates_lonThenLat() {
        val locationDto = DriverTripLocationDto(
            name = "Main Bus Stand",
            formattedAddress = "Main Bus Stand, Jhansi",
            coordinates = DriverGeoJsonPointDto(
                type = "Point",
                coordinates = doubleArrayOf(78.5685, 25.4484) // [lon, lat]
            )
        )

        val domain = DriverMapper.toDomain(locationDto)

        assertEquals(25.4484, domain.latitude, 0.00001)
        assertEquals(78.5685, domain.longitude, 0.00001)
        assertEquals("Main Bus Stand", domain.name)
        assertEquals("Main Bus Stand, Jhansi", domain.formattedAddress)
    }
}
