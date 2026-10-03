package com.ishara.app

import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.mapper.RideRequestMapper
import com.ishara.app.data.remote.datasource.RideRequestRemoteDataSourceImpl
import com.ishara.app.domain.model.RideRequestInput
import com.ishara.app.domain.model.RideRequestLocationWaypoint
import com.ishara.app.domain.model.RideRequestResult
import com.ishara.app.domain.model.RideRequestStatus
import com.ishara.app.domain.repository.RideRepository
import com.ishara.app.domain.usecase.CreateRideRequestUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Strict contract and regression test for Ride Request:
 * 1. Serialized JSON must contain ONLY tripId, pickup, destination.
 * 2. Serialized JSON must NEVER contain fare, price, seatCapacity, seatsRequested, etc.
 * 3. 24-character hex ObjectId regex validation.
 * 4. Response parsing maps status to PENDING and GeoJSON coordinates correctly.
 * 5. Minimum 50m separation rule.
 */
class RideRequestStrictContractTest {

    private val fakeHttpClient = object : IshaaraHttpClient {
        override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
            return IshaaraResult.success(HttpResponse(statusCode = 201, body = "{}"))
        }
    }
    private val dataSource = RideRequestRemoteDataSourceImpl(fakeHttpClient, NetworkConfig())

    @Test
    fun `serialized request strictly excludes unsupported fields`() {
        val input = RideRequestInput(
            tripId = "651a2b3c4d5e6f7a8b9c0d1e",
            pickup = RideRequestLocationWaypoint(
                formattedAddress = "Kenyatta University Gate A, Nairobi",
                latitude = -1.1818,
                longitude = 36.9275,
                name = "Gate A"
            ),
            destination = RideRequestLocationWaypoint(
                formattedAddress = "Safari Park Hotel, Kasarani, Nairobi",
                latitude = -1.2185,
                longitude = 36.8856,
                name = "Safari Park"
            )
        )

        val requestDto = RideRequestMapper.toDto(input)
        val json = dataSource.serializeRequest(requestDto)

        // Verified fields present
        assertTrue(json.contains("\"tripId\":\"651a2b3c4d5e6f7a8b9c0d1e\""))
        assertTrue(json.contains("\"pickup\""))
        assertTrue(json.contains("\"destination\""))
        assertTrue(json.contains("\"formattedAddress\":\"Kenyatta University Gate A, Nairobi\""))
        assertTrue(json.contains("\"latitude\":-1.1818"))
        assertTrue(json.contains("\"longitude\":36.9275"))
        assertTrue(json.contains("\"name\":\"Gate A\""))

        // STRICT REGRESSION CHECKS: Must NOT contain client-controlled or unsupported fields
        assertFalse("Must not contain fare", json.contains("fare"))
        assertFalse("Must not contain price", json.contains("price"))
        assertFalse("Must not contain seatCapacity", json.contains("seatCapacity"))
        assertFalse("Must not contain seatsRequested", json.contains("seatsRequested"))
        assertFalse("Must not contain passengerCount", json.contains("passengerCount"))
        assertFalse("Must not contain status", json.contains("\"status\""))
        assertFalse("Must not contain driverId", json.contains("\"driverId\""))
        assertFalse("Must not contain userId", json.contains("\"userId\""))
    }

    @Test
    fun `response parser correctly maps RideRequestResponse with PENDING status`() {
        val mockJson = """
        {
            "success": true,
            "statusCode": 201,
            "message": "Ride request submitted successfully.",
            "data": {
                "id": "670e1c2b3f4a5b6c7d8e9f01",
                "tripId": "651a2b3c4d5e6f7a8b9c0d1e",
                "driverId": "651a00112233445566778899",
                "userId": "6519ffeeddccbbaa99887766",
                "pickup": {
                    "name": "Gate A",
                    "formattedAddress": "Kenyatta University Gate A",
                    "coordinates": {
                        "type": "Point",
                        "coordinates": [36.9275, -1.1818]
                    }
                },
                "destination": {
                    "name": "Safari Park",
                    "formattedAddress": "Safari Park Hotel",
                    "coordinates": {
                        "type": "Point",
                        "coordinates": [36.8856, -1.2185]
                    }
                },
                "status": "PENDING",
                "requestedAt": "2026-09-23T13:00:00.000Z",
                "respondedAt": null,
                "expiresAt": "2026-09-23T13:02:00.000Z",
                "rejectionReason": null,
                "cancellationReason": null,
                "createdAt": "2026-09-23T13:00:00.000Z",
                "updatedAt": "2026-09-23T13:00:00.000Z"
            }
        }
        """.trimIndent()

        val parsedDto = dataSource.parseRideRequestResponse(mockJson)
        assertEquals("670e1c2b3f4a5b6c7d8e9f01", parsedDto.id)
        assertEquals("651a2b3c4d5e6f7a8b9c0d1e", parsedDto.tripId)
        assertEquals("651a00112233445566778899", parsedDto.driverId)
        assertEquals("6519ffeeddccbbaa99887766", parsedDto.userId)
        assertEquals("PENDING", parsedDto.status)
        assertEquals("Kenyatta University Gate A", parsedDto.pickup.formattedAddress)
        assertEquals("Safari Park Hotel", parsedDto.destination.formattedAddress)

        val domainResult = RideRequestMapper.toDomain(parsedDto)
        assertEquals("670e1c2b3f4a5b6c7d8e9f01", domainResult.id)
        assertEquals(RideRequestStatus.PENDING, domainResult.status)
        assertEquals("2026-09-23T13:02:00.000Z", domainResult.expiresAt)
    }

    @Test
    fun `use case enforces 50m minimum separation rule`() = runBlocking {
        val fakeRepo = object : RideRepository {
            override suspend fun submitRideRequest(
                input: RideRequestInput,
                idempotencyKey: String?
            ): IshaaraResult<RideRequestResult> {
                return IshaaraResult.success(
                    RideRequestResult(
                        id = "req_1",
                        tripId = input.tripId,
                        driverId = "drv_1",
                        userId = "usr_1",
                        pickupAddress = input.pickup.formattedAddress,
                        destinationAddress = input.destination.formattedAddress,
                        status = RideRequestStatus.PENDING,
                        requestedAt = "2026-09-23T13:00:00Z",
                        expiresAt = "2026-09-23T13:02:00Z"
                    )
                )
            }

            override suspend fun createRideRequest(
                tripId: String,
                pickupAddress: String,
                dropoffAddress: String,
                seatsRequested: Int
            ): IshaaraResult<com.ishara.app.domain.model.RideRequest> = throw UnsupportedOperationException()

            override suspend fun getActiveRide(rideId: String): IshaaraResult<com.ishara.app.domain.model.Ride> = throw UnsupportedOperationException()
            override suspend fun getActivePassengerRide(): IshaaraResult<com.ishara.app.domain.model.Ride?> = IshaaraResult.success(null)
            override suspend fun cancelRide(rideId: String, reason: String?): IshaaraResult<Unit> = IshaaraResult.success(Unit)
        }

        val useCase = CreateRideRequestUseCase(fakeRepo)

        // 1. Same coordinates (0m separation) -> Should fail validation
        val sameCoordInput = RideRequestInput(
            tripId = "651a2b3c4d5e6f7a8b9c0d1e",
            pickup = RideRequestLocationWaypoint(
                formattedAddress = "Gate A",
                latitude = 25.2799,
                longitude = 82.9995
            ),
            destination = RideRequestLocationWaypoint(
                formattedAddress = "Gate A across street",
                latitude = 25.2799,
                longitude = 82.9995
            )
        )

        val failResult = useCase.execute(sameCoordInput)
        assertTrue(failResult is IshaaraResult.Failure)
        val error = (failResult as IshaaraResult.Failure).error
        assertTrue(error is IshaaraError.Validation)
        assertTrue(error.message.contains("minimum 50m separation"))

        // 2. ~500m separation -> Should succeed
        val validCoordInput = RideRequestInput(
            tripId = "651a2b3c4d5e6f7a8b9c0d1e",
            pickup = RideRequestLocationWaypoint(
                formattedAddress = "Gate A",
                latitude = 25.2799,
                longitude = 82.9995
            ),
            destination = RideRequestLocationWaypoint(
                formattedAddress = "Lanka Market",
                latitude = 25.2850,
                longitude = 83.0030
            )
        )

        val successResult = useCase.execute(validCoordInput)
        assertTrue(successResult is IshaaraResult.Success)
    }

    @Test
    fun `use case rejects invalid tripId ObjectId regex`() = runBlocking {
        val fakeRepo = object : RideRepository {
            override suspend fun submitRideRequest(
                input: RideRequestInput,
                idempotencyKey: String?
            ): IshaaraResult<RideRequestResult> = throw UnsupportedOperationException()
            override suspend fun createRideRequest(tripId: String, pickupAddress: String, dropoffAddress: String, seatsRequested: Int) = throw UnsupportedOperationException()
            override suspend fun getActiveRide(rideId: String) = throw UnsupportedOperationException()
            override suspend fun getActivePassengerRide() = IshaaraResult.success(null)
            override suspend fun cancelRide(rideId: String, reason: String?) = IshaaraResult.success(Unit)
        }

        var exceptionThrown = false
        try {
            RideRequestInput(
                tripId = "invalid_id_not_24_chars",
                pickup = RideRequestLocationWaypoint("A", 25.0, 82.0),
                destination = RideRequestLocationWaypoint("B", 25.1, 82.1)
            )
        } catch (e: IllegalArgumentException) {
            exceptionThrown = true
        }
        assertTrue("Must reject invalid ObjectId format", exceptionThrown)
    }
}
