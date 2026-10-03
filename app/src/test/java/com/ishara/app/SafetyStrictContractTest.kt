package com.ishara.app

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.mapper.SafetyMapper
import com.ishara.app.data.remote.datasource.SafetyRemoteDataSourceImpl
import com.ishara.app.data.remote.dto.CancelSosRequestDto
import com.ishara.app.data.remote.dto.CreateEmergencyContactRequestDto
import com.ishara.app.data.remote.dto.CreateSosRequestDto
import com.ishara.app.data.remote.dto.EmergencyContactResponseDto
import com.ishara.app.data.remote.dto.EmergencyEventResponseDto
import com.ishara.app.data.remote.dto.SafetyLocationSnapshotDto
import com.ishara.app.domain.model.EmergencyContactRelationship
import com.ishara.app.domain.model.EmergencyStatus
import com.ishara.app.domain.model.EmergencyType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Strict contract and domain mapping tests for Phase 15: Safety, SOS & Emergency Response.
 */
class SafetyStrictContractTest {

    private val sampleEventJson = """
        {
          "success": true,
          "data": {
            "id": "66f7f8a1b2c3d4e5f6789012",
            "eventId": "se_abc123def456",
            "rideId": "ride_998877",
            "tripId": "trip_445566",
            "triggeredByUserId": "user_112233",
            "triggeredByRole": "USER",
            "driverId": "drv_profile_7788",
            "passengerUserId": "user_112233",
            "emergencyType": "SOS",
            "status": "ACTIVE",
            "locationSnapshot": {
              "coordinates": [77.5946, 12.9716],
              "accuracyMeters": 4.5,
              "headingDegrees": 180.0,
              "speedMps": 12.3,
              "isStale": false,
              "capturedAt": "2026-09-28T12:00:00.000Z",
              "provider": "driver_profile"
            },
            "triggeredAt": "2026-09-28T12:00:01.000Z",
            "acknowledgedAt": null,
            "resolvedAt": null,
            "cancelledAt": null,
            "cancellationReason": null,
            "createdAt": "2026-09-28T12:00:01.000Z",
            "updatedAt": "2026-09-28T12:00:01.000Z"
          },
          "message": "SOS emergency event triggered successfully."
        }
    """.trimIndent()

    private val sampleContactJson = """
        {
          "success": true,
          "data": {
            "id": "contact_123",
            "name": "Jane Doe",
            "phoneNumber": "+919876543210",
            "relationship": "PARENT",
            "isVerified": false,
            "isActive": true,
            "createdAt": "2026-09-28T10:00:00.000Z",
            "updatedAt": "2026-09-28T10:00:00.000Z"
          },
          "message": "Emergency contact added successfully."
        }
    """.trimIndent()

    // =========================================================================
    // 1. Serialization Tests (Strict Backend Schema Compliance)
    // =========================================================================

    @Test
    fun `serializeCreateSosRequest generates strict schema matching backend`() {
        val dataSource = SafetyRemoteDataSourceImpl(
            httpClient = object : IshaaraHttpClient {
                override suspend fun execute(request: HttpRequest) = IshaaraResult.success(HttpResponse(200, "{}"))
            },
            networkConfig = NetworkConfig()
        )

        val json = dataSource.serializeCreateSosRequest(CreateSosRequestDto(emergencyType = "SOS"))
        assertEquals("""{"emergencyType":"SOS"}""", json)

        // Strict schema must not contain extra fields
        assertFalse(json.contains("location"))
        assertFalse(json.contains("coordinates"))
        assertFalse(json.contains("rideId"))
        assertFalse(json.contains("triggeredByUserId"))
    }

    @Test
    fun `serializeCancelSosRequest generates optional reason correctly`() {
        val dataSource = SafetyRemoteDataSourceImpl(
            httpClient = object : IshaaraHttpClient {
                override suspend fun execute(request: HttpRequest) = IshaaraResult.success(HttpResponse(200, "{}"))
            },
            networkConfig = NetworkConfig()
        )

        val jsonWithReason = dataSource.serializeCancelSosRequest(CancelSosRequestDto(reason = "False alarm"))
        assertEquals("""{"reason":"False alarm"}""", jsonWithReason)

        val jsonWithoutReason = dataSource.serializeCancelSosRequest(CancelSosRequestDto(reason = null))
        assertEquals("{}", jsonWithoutReason)
    }

    @Test
    fun `serializeCreateEmergencyContactRequest produces exact fields`() {
        val dataSource = SafetyRemoteDataSourceImpl(
            httpClient = object : IshaaraHttpClient {
                override suspend fun execute(request: HttpRequest) = IshaaraResult.success(HttpResponse(200, "{}"))
            },
            networkConfig = NetworkConfig()
        )

        val json = dataSource.serializeCreateEmergencyContactRequest(
            CreateEmergencyContactRequestDto(
                name = "Mom",
                phoneNumber = "+919876543210",
                relationship = "PARENT"
            )
        )
        assertEquals("""{"name":"Mom","phoneNumber":"+919876543210","relationship":"PARENT"}""", json)
    }

    // =========================================================================
    // 2. Response Parsing Tests
    // =========================================================================

    @Test
    fun `parseEmergencyEventResponse parses valid envelope correctly`() {
        val dataSource = SafetyRemoteDataSourceImpl(
            httpClient = object : IshaaraHttpClient {
                override suspend fun execute(request: HttpRequest) = IshaaraResult.success(HttpResponse(200, "{}"))
            },
            networkConfig = NetworkConfig()
        )

        val dto = dataSource.parseEmergencyEventResponse(sampleEventJson)
        assertEquals("se_abc123def456", dto.eventId)
        assertEquals("ride_998877", dto.rideId)
        assertEquals("trip_445566", dto.tripId)
        assertEquals("user_112233", dto.triggeredByUserId)
        assertEquals("USER", dto.triggeredByRole)
        assertEquals("drv_profile_7788", dto.driverId)
        assertEquals("user_112233", dto.passengerUserId)
        assertEquals("SOS", dto.emergencyType)
        assertEquals("ACTIVE", dto.status)
        assertEquals(77.5946, dto.locationSnapshot.coordinates?.get(0) ?: 0.0, 0.0001)
        assertEquals(12.9716, dto.locationSnapshot.coordinates?.get(1) ?: 0.0, 0.0001)
        assertEquals(4.5, dto.locationSnapshot.accuracyMeters ?: 0.0, 0.01)
        assertFalse(dto.locationSnapshot.isStale)
        assertEquals("driver_profile", dto.locationSnapshot.provider)
        assertNull(dto.acknowledgedAt)
        assertNull(dto.cancelledAt)
    }

    @Test
    fun `parseNullableEmergencyEventResponse returns null when data is null`() {
        val dataSource = SafetyRemoteDataSourceImpl(
            httpClient = object : IshaaraHttpClient {
                override suspend fun execute(request: HttpRequest) = IshaaraResult.success(HttpResponse(200, "{}"))
            },
            networkConfig = NetworkConfig()
        )

        val nullJson = """{"success":true,"data":null,"message":"No active SOS event for this ride."}"""
        val result = dataSource.parseNullableEmergencyEventResponse(nullJson)
        assertNull(result)
    }

    @Test
    fun `parseEmergencyContactResponse parses contact correctly`() {
        val dataSource = SafetyRemoteDataSourceImpl(
            httpClient = object : IshaaraHttpClient {
                override suspend fun execute(request: HttpRequest) = IshaaraResult.success(HttpResponse(200, "{}"))
            },
            networkConfig = NetworkConfig()
        )

        val contactDto = dataSource.parseEmergencyContactResponse(sampleContactJson)
        assertEquals("contact_123", contactDto.id)
        assertEquals("Jane Doe", contactDto.name)
        assertEquals("+919876543210", contactDto.phoneNumber)
        assertEquals("PARENT", contactDto.relationship)
        assertFalse(contactDto.isVerified)
        assertTrue(contactDto.isActive)
    }

    // =========================================================================
    // 3. Domain Enum Mapping & Safety Fallback Tests
    // =========================================================================

    @Test
    fun `EmergencyStatus maps known statuses and safely handles unknown values`() {
        assertEquals(EmergencyStatus.ACTIVE, EmergencyStatus.fromString("ACTIVE"))
        assertEquals(EmergencyStatus.ACKNOWLEDGED, EmergencyStatus.fromString("ACKNOWLEDGED"))
        assertEquals(EmergencyStatus.RESOLVED, EmergencyStatus.fromString("RESOLVED"))
        assertEquals(EmergencyStatus.CANCELLED, EmergencyStatus.fromString("CANCELLED"))

        // Terminal check
        assertFalse(EmergencyStatus.ACTIVE.isTerminal)
        assertFalse(EmergencyStatus.ACKNOWLEDGED.isTerminal)
        assertTrue(EmergencyStatus.RESOLVED.isTerminal)
        assertTrue(EmergencyStatus.CANCELLED.isTerminal)

        // Unknown future status
        assertEquals(EmergencyStatus.UNKNOWN, EmergencyStatus.fromString("ESCALATED_TO_SUPERVISOR"))
        assertEquals(EmergencyStatus.UNKNOWN, EmergencyStatus.fromString(null))
        assertFalse(EmergencyStatus.UNKNOWN.isTerminal)
    }

    @Test
    fun `EmergencyType maps known types and safely handles unknown values`() {
        assertEquals(EmergencyType.SOS, EmergencyType.fromString("SOS"))
        assertEquals(EmergencyType.SAFETY_CONCERN, EmergencyType.fromString("SAFETY_CONCERN"))
        assertEquals(EmergencyType.UNKNOWN, EmergencyType.fromString("FUTURE_FIRE_ALARM"))
        assertEquals(EmergencyType.UNKNOWN, EmergencyType.fromString(null))
    }

    @Test
    fun `EmergencyContactRelationship maps known values and safely handles unknown values`() {
        assertEquals(EmergencyContactRelationship.PARENT, EmergencyContactRelationship.fromString("PARENT"))
        assertEquals(EmergencyContactRelationship.SPOUSE, EmergencyContactRelationship.fromString("SPOUSE"))
        assertEquals(EmergencyContactRelationship.SIBLING, EmergencyContactRelationship.fromString("SIBLING"))
        assertEquals(EmergencyContactRelationship.FRIEND, EmergencyContactRelationship.fromString("FRIEND"))
        assertEquals(EmergencyContactRelationship.GUARDIAN, EmergencyContactRelationship.fromString("GUARDIAN"))
        assertEquals(EmergencyContactRelationship.OTHER, EmergencyContactRelationship.fromString("OTHER"))
        assertEquals(EmergencyContactRelationship.UNKNOWN, EmergencyContactRelationship.fromString("COLLEAGUE"))
        assertEquals(EmergencyContactRelationship.UNKNOWN, EmergencyContactRelationship.fromString(null))
    }

    // =========================================================================
    // 4. SafetyMapper Domain Conversion Tests
    // =========================================================================

    @Test
    fun `SafetyMapper toDomain maps DTO correctly`() {
        val dto = EmergencyEventResponseDto(
            id = "mongo_1",
            eventId = "se_test",
            rideId = "ride_1",
            tripId = null,
            triggeredByUserId = "user_1",
            triggeredByRole = "USER",
            driverId = "drv_1",
            passengerUserId = "user_1",
            emergencyType = "SOS",
            status = "ACTIVE",
            locationSnapshot = SafetyLocationSnapshotDto(
                coordinates = listOf(77.5, 12.9),
                accuracyMeters = 5.0,
                headingDegrees = 90.0,
                speedMps = 0.0,
                isStale = false,
                capturedAt = "2026-09-28T12:00:00Z",
                provider = "driver_profile"
            ),
            triggeredAt = "2026-09-28T12:00:00Z",
            acknowledgedAt = null,
            resolvedAt = null,
            cancelledAt = null,
            cancellationReason = null,
            createdAt = "2026-09-28T12:00:00Z",
            updatedAt = "2026-09-28T12:00:00Z"
        )

        val domain = SafetyMapper.toDomain(dto)
        assertEquals("se_test", domain.eventId)
        assertEquals(EmergencyType.SOS, domain.emergencyType)
        assertEquals(EmergencyStatus.ACTIVE, domain.status)
        assertTrue(domain.locationSnapshot.hasCoordinates)
        assertEquals(77.5, domain.locationSnapshot.longitude ?: 0.0, 0.001)
        assertEquals(12.9, domain.locationSnapshot.latitude ?: 0.0, 0.001)
        assertFalse(domain.locationSnapshot.isStale)
    }
}
