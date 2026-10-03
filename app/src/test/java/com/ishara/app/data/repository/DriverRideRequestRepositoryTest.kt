package com.ishara.app.data.repository

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.remote.datasource.DriverRideRequestRemoteDataSourceImpl
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.DriverRideRequestStatus
import com.ishara.app.domain.model.DriverRideStatus
import com.ishara.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DriverRideRequestRepositoryTest {

    private lateinit var fakeHttpClient: FakeIshaaraHttpClient
    private lateinit var remoteDataSource: DriverRideRequestRemoteDataSourceImpl
    private lateinit var fakeSessionDataSource: FakeSessionLocalDataSource
    private lateinit var repository: DriverRideRequestRepositoryImpl
    private val networkConfig = NetworkConfig()

    @Before
    fun setup() {
        fakeHttpClient = FakeIshaaraHttpClient()
        remoteDataSource = DriverRideRequestRemoteDataSourceImpl(fakeHttpClient, networkConfig)
        fakeSessionDataSource = FakeSessionLocalDataSource(
            AuthSession(
                token = "valid_driver_jwt_token",
                userId = "driver_123",
                role = UserRole.DRIVER_CONDUCTOR
            )
        )
        repository = DriverRideRequestRepositoryImpl(
            remoteDataSource = remoteDataSource,
            sessionLocalDataSource = fakeSessionDataSource,
            networkConfig = networkConfig
        )
    }

    @Test
    fun getDriverRideRequests_sendsAuthorizationHeaderAndCorrectEndpoint() = runBlocking {
        val sampleResponse = """
            {
                "success": true,
                "statusCode": 200,
                "data": {
                    "items": [
                        {
                            "id": "req_001",
                            "tripId": "trip_001",
                            "driverId": "driver_123",
                            "userId": "user_456",
                            "pickup": {
                                "formattedAddress": "Gate 1",
                                "coordinates": [77.5946, 12.9716]
                            },
                            "destination": {
                                "formattedAddress": "Library",
                                "coordinates": [77.5982, 12.9754]
                            },
                            "status": "PENDING",
                            "requestedAt": "2026-09-25T13:00:00.000Z",
                            "expiresAt": "2026-09-25T13:05:00.000Z"
                        }
                    ],
                    "total": 1,
                    "page": 1,
                    "limit": 20,
                    "hasMore": false
                }
            }
        """.trimIndent()
        fakeHttpClient.responseToReturn = IshaaraResult.success(HttpResponse(200, sampleResponse))

        val result = repository.getDriverRideRequests(status = "PENDING")

        assertTrue(result is IshaaraResult.Success)
        val page = (result as IshaaraResult.Success).data
        assertEquals(1, page.items.size)
        val request = page.items[0]
        assertEquals("req_001", request.id)
        assertEquals("trip_001", request.tripId)
        assertEquals("Gate 1", request.pickupAddress)
        // GeoJSON RFC 7946 coordinates [lng, lat] -> (lat, lng)
        assertEquals(12.9716, request.pickupCoordinates.first, 0.0001)
        assertEquals(77.5946, request.pickupCoordinates.second, 0.0001)
        assertEquals(DriverRideRequestStatus.PENDING, request.status)

        // Verify request details sent to HTTP client
        val recorded = fakeHttpClient.lastRequest
        assertNotNull(recorded)
        assertEquals(HttpMethod.GET, recorded?.method)
        assertTrue(recorded?.url?.endsWith("/drivers/me/ride-requests") == true)
        assertEquals("Bearer valid_driver_jwt_token", recorded?.headers?.get("Authorization"))
        assertEquals("PENDING", recorded?.queryParams?.get("status"))
    }

    @Test
    fun acceptRideRequest_hitsCorrectBackendEndpoint() = runBlocking {
        val acceptResponse = """
            {
                "success": true,
                "statusCode": 200,
                "data": {
                    "id": "req_001",
                    "tripId": "trip_001",
                    "driverId": "driver_123",
                    "userId": "user_456",
                    "pickup": { "formattedAddress": "Gate 1", "coordinates": [77.5, 12.9] },
                    "destination": { "formattedAddress": "Library", "coordinates": [77.6, 13.0] },
                    "status": "ACCEPTED",
                    "requestedAt": "2026-09-25T13:00:00.000Z"
                }
            }
        """.trimIndent()
        fakeHttpClient.responseToReturn = IshaaraResult.success(HttpResponse(200, acceptResponse))

        val result = repository.acceptRideRequest("req_001")

        assertTrue(result is IshaaraResult.Success)
        val accepted = (result as IshaaraResult.Success).data
        assertEquals(DriverRideRequestStatus.ACCEPTED, accepted.status)

        val recorded = fakeHttpClient.lastRequest
        assertNotNull(recorded)
        assertEquals(HttpMethod.POST, recorded?.method)
        assertTrue(recorded?.url?.endsWith("/ride-requests/req_001/accept") == true)
        assertEquals("Bearer valid_driver_jwt_token", recorded?.headers?.get("Authorization"))
    }

    @Test
    fun rejectRideRequest_sendsReasonBody() = runBlocking {
        val rejectResponse = """
            {
                "success": true,
                "statusCode": 200,
                "data": {
                    "id": "req_001",
                    "tripId": "trip_001",
                    "driverId": "driver_123",
                    "userId": "user_456",
                    "pickup": { "formattedAddress": "Gate 1", "coordinates": [77.5, 12.9] },
                    "destination": { "formattedAddress": "Library", "coordinates": [77.6, 13.0] },
                    "status": "REJECTED",
                    "requestedAt": "2026-09-25T13:00:00.000Z",
                    "rejectionReason": "Vehicle full"
                }
            }
        """.trimIndent()
        fakeHttpClient.responseToReturn = IshaaraResult.success(HttpResponse(200, rejectResponse))

        val result = repository.rejectRideRequest("req_001", "Vehicle full")

        assertTrue(result is IshaaraResult.Success)
        val rejected = (result as IshaaraResult.Success).data
        assertEquals(DriverRideRequestStatus.REJECTED, rejected.status)
        assertEquals("Vehicle full", rejected.rejectionReason)

        val recorded = fakeHttpClient.lastRequest
        assertNotNull(recorded)
        assertEquals(HttpMethod.POST, recorded?.method)
        assertTrue(recorded?.url?.endsWith("/ride-requests/req_001/reject") == true)
        assertTrue(recorded?.body?.contains("Vehicle full") == true)
    }

    @Test
    fun markPassengerBoarded_hitsPickupEndpoint() = runBlocking {
        val pickupResponse = """
            {
                "success": true,
                "statusCode": 200,
                "data": {
                    "id": "ride_999",
                    "tripId": "trip_001",
                    "driverId": "driver_123",
                    "userId": "user_456",
                    "pickup": { "formattedAddress": "Gate 1", "coordinates": [77.5, 12.9] },
                    "destination": { "formattedAddress": "Library", "coordinates": [77.6, 13.0] },
                    "status": "PICKED_UP",
                    "pickupTime": "2026-09-25T13:02:00.000Z"
                }
            }
        """.trimIndent()
        fakeHttpClient.responseToReturn = IshaaraResult.success(HttpResponse(200, pickupResponse))

        val result = repository.markPassengerBoarded("ride_999")

        assertTrue(result is IshaaraResult.Success)
        val ride = (result as IshaaraResult.Success).data
        assertEquals(DriverRideStatus.PICKED_UP, ride.status)

        val recorded = fakeHttpClient.lastRequest
        assertNotNull(recorded)
        assertEquals(HttpMethod.POST, recorded?.method)
        assertTrue(recorded?.url?.endsWith("/rides/ride_999/pickup") == true)
    }

    @Test
    fun getDriverRideRequests_returnsAuthenticationErrorWhenSessionNull() = runBlocking {
        fakeSessionDataSource.session = null
        val result = repository.getDriverRideRequests()
        assertTrue(result is IshaaraResult.Failure)
        assertTrue((result as IshaaraResult.Failure).error is IshaaraError.Authentication)
    }

    private class FakeIshaaraHttpClient : IshaaraHttpClient {
        var responseToReturn: IshaaraResult<HttpResponse> = IshaaraResult.success(HttpResponse(200, "{}"))
        var lastRequest: HttpRequest? = null

        override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
            lastRequest = request
            return responseToReturn
        }
    }

    private class FakeSessionLocalDataSource(var session: AuthSession?) : SessionLocalDataSource {
        override suspend fun getSession(): AuthSession? = session
        override suspend fun saveSession(session: AuthSession) { this.session = session }
        override suspend fun clearSession() { this.session = null }
        override fun observeSession(): Flow<AuthSession?> = emptyFlow()
    }
}
