package com.ishara.app

import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.data.remote.datasource.RideRequestRemoteDataSourceImpl
import com.ishara.app.data.repository.RideRepositoryImpl
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.RideRequestInput
import com.ishara.app.domain.model.RideRequestLocationWaypoint
import com.ishara.app.domain.model.RideRequestStatus
import com.ishara.app.domain.model.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RideRequestRepositoryTest {

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
        override fun observeSession(): kotlinx.coroutines.flow.Flow<AuthSession?> = kotlinx.coroutines.flow.flowOf(sessionToReturn)
        override suspend fun clearSession() {}
    }

    private val sampleInput = RideRequestInput(
        tripId = "651a2b3c4d5e6f7a8b9c0d1e",
        pickup = RideRequestLocationWaypoint("Gate A", 25.2799, 82.9995),
        destination = RideRequestLocationWaypoint("Lanka", 25.2850, 83.0030)
    )

    private val sampleResponseJson = """
    {
        "success": true,
        "statusCode": 201,
        "message": "Ride request submitted successfully.",
        "data": {
            "id": "req_12345",
            "tripId": "651a2b3c4d5e6f7a8b9c0d1e",
            "driverId": "drv_67890",
            "userId": "usr_abcde",
            "pickup": {
                "name": "Gate A",
                "formattedAddress": "Gate A",
                "coordinates": {
                    "type": "Point",
                    "coordinates": [82.9995, 25.2799]
                }
            },
            "destination": {
                "name": "Lanka",
                "formattedAddress": "Lanka",
                "coordinates": {
                    "type": "Point",
                    "coordinates": [83.0030, 25.2850]
                }
            },
            "status": "PENDING",
            "requestedAt": "2026-09-23T13:00:00Z",
            "expiresAt": "2026-09-23T13:02:00Z"
        }
    }
    """.trimIndent()

    @Test
    fun `submitRideRequest succeeds and passes Bearer token and Idempotency-Key`() = runBlocking {
        val mockHttpClient = MockHttpClient(
            IshaaraResult.success(HttpResponse(statusCode = 201, body = sampleResponseJson))
        )
        val mockSession = MockSessionLocalDataSource(
            AuthSession(token = "valid_token_xyz", role = UserRole.USER, userId = "usr_abcde")
        )
        val networkConfig = NetworkConfig()
        val dataSource = RideRequestRemoteDataSourceImpl(mockHttpClient, networkConfig)
        val repository = RideRepositoryImpl(
            httpClient = mockHttpClient,
            networkConfig = networkConfig,
            sessionLocalDataSource = mockSession,
            rideRequestRemoteDataSource = dataSource
        )

        val result = repository.submitRideRequest(sampleInput, idempotencyKey = "uuid-key-123")

        assertTrue(result is IshaaraResult.Success)
        val data = (result as IshaaraResult.Success).data
        assertEquals("req_12345", data.id)
        assertEquals(RideRequestStatus.PENDING, data.status)
        assertEquals("Gate A", data.pickupAddress)

        // Verify HTTP request headers
        val req = mockHttpClient.lastRequest
        assertEquals("Bearer valid_token_xyz", req?.headers?.get("Authorization"))
        assertEquals("uuid-key-123", req?.headers?.get("Idempotency-Key"))
        assertEquals("${networkConfig.fullApiBaseUrl}/ride-requests", req?.url)
    }

    @Test
    fun `submitRideRequest maps HTTP 409 Conflict to duplicate request error`() = runBlocking {
        val conflictJson = """
        {
            "success": false,
            "error": {
                "code": "DUPLICATE_RIDE_REQUEST",
                "message": "You already have an active pending ride request for this trip."
            }
        }
        """.trimIndent()

        val mockHttpClient = MockHttpClient(
            IshaaraResult.failure(IshaaraError.Conflict(message = conflictJson))
        )
        val mockSession = MockSessionLocalDataSource(
            AuthSession(token = "tok", role = UserRole.USER, userId = "usr")
        )
        val networkConfig = NetworkConfig()
        val dataSource = RideRequestRemoteDataSourceImpl(mockHttpClient, networkConfig)
        val repository = RideRepositoryImpl(
            httpClient = mockHttpClient,
            networkConfig = networkConfig,
            sessionLocalDataSource = mockSession,
            rideRequestRemoteDataSource = dataSource
        )

        val result = repository.submitRideRequest(sampleInput)

        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Conflict)
        assertTrue(error.message.contains("already have an active pending ride request"))
    }

    @Test
    fun `submitRideRequest maps HTTP 400 TRIP_NOT_ELIGIBLE to trip unavailable error`() = runBlocking {
        val ineligibleJson = """
        {
            "success": false,
            "error": {
                "code": "TRIP_NOT_ELIGIBLE",
                "message": "Trip is not eligible for ride requests (current trip status: COMPLETED)."
            }
        }
        """.trimIndent()

        val mockHttpClient = MockHttpClient(
            IshaaraResult.failure(IshaaraError.Validation("tripId", ineligibleJson))
        )
        val mockSession = MockSessionLocalDataSource(
            AuthSession(token = "tok", role = UserRole.USER, userId = "usr")
        )
        val networkConfig = NetworkConfig()
        val dataSource = RideRequestRemoteDataSourceImpl(mockHttpClient, networkConfig)
        val repository = RideRepositoryImpl(
            httpClient = mockHttpClient,
            networkConfig = networkConfig,
            sessionLocalDataSource = mockSession,
            rideRequestRemoteDataSource = dataSource
        )

        val result = repository.submitRideRequest(sampleInput)

        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Validation)
        assertTrue(error.message.contains("no longer active"))
    }

    @Test
    fun `submitRideRequest requires authenticated session`() = runBlocking {
        val mockHttpClient = MockHttpClient(
            IshaaraResult.success(HttpResponse(statusCode = 201, body = sampleResponseJson))
        )
        val mockSession = MockSessionLocalDataSource(sessionToReturn = null)
        val networkConfig = NetworkConfig()
        val dataSource = RideRequestRemoteDataSourceImpl(mockHttpClient, networkConfig)
        val repository = RideRepositoryImpl(
            httpClient = mockHttpClient,
            networkConfig = networkConfig,
            sessionLocalDataSource = mockSession,
            rideRequestRemoteDataSource = dataSource
        )

        val result = repository.submitRideRequest(sampleInput)

        assertTrue(result is IshaaraResult.Failure)
        val error = (result as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Authentication)
    }
}
