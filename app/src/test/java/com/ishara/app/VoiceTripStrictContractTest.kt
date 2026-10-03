package com.ishara.app

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.mapper.VoiceTripDraftMapper
import com.ishara.app.data.remote.datasource.VoiceTripDraftRemoteDataSourceImpl
import com.ishara.app.data.remote.dto.CreateVoiceTripDraftRequestDto
import com.ishara.app.data.remote.dto.ResolvedLocationDto
import com.ishara.app.data.remote.dto.VoiceTripDraftEndpointDto
import com.ishara.app.data.remote.dto.VoiceTripDraftResponseDto
import com.ishara.app.domain.model.VoiceTripDraftStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Strict contract test validating Phase 12 Voice Trip Draft DTO serialization,
 * pure Kotlin JSON parsing, domain model mapping, and HTTP error normalization.
 */
class VoiceTripStrictContractTest {

    private class MockHttpClient(
        var responseCode: Int = 201,
        var responseBody: String = ""
    ) : IshaaraHttpClient {
        var lastRequest: HttpRequest? = null

        override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
            lastRequest = request
            return if (responseCode in 200..299) {
                IshaaraResult.Success(HttpResponse(responseCode, responseBody))
            } else {
                IshaaraResult.Failure(
                    com.ishara.app.core.result.IshaaraError.Server(
                        code = responseCode,
                        message = "HTTP $responseCode: $responseBody"
                    )
                )
            }
        }
    }

    @Test
    fun serializeCreateRequest_generatesStrictJsonMatchingZodSchema() {
        val remoteDataSource = VoiceTripDraftRemoteDataSourceImpl(
            httpClient = MockHttpClient(),
            networkConfig = NetworkConfig()
        )

        val dto = CreateVoiceTripDraftRequestDto(
            inputMode = "DEVICE_TRANSCRIPT",
            transcript = "Take me from Jhansi Railway Station to SRGI College",
            languageHint = "en"
        )

        val json = remoteDataSource.serializeCreateRequest(dto)

        assertTrue("Must contain exact inputMode", json.contains("\"inputMode\":\"DEVICE_TRANSCRIPT\""))
        assertTrue("Must contain transcript", json.contains("\"transcript\":\"Take me from Jhansi Railway Station to SRGI College\""))
        assertTrue("Must contain languageHint", json.contains("\"languageHint\":\"en\""))
        assertFalse("Must not contain extraneous fields", json.contains("coordinates") || json.contains("driverId"))
    }

    @Test
    fun parseVoiceTripDraftResponse_parsesBackendResponseCorrectly() {
        val remoteDataSource = VoiceTripDraftRemoteDataSourceImpl(
            httpClient = MockHttpClient(),
            networkConfig = NetworkConfig()
        )

        val rawBackendJson = """
        {
          "statusCode": 201,
          "success": true,
          "data": {
            "id": "673f456789abcdef01234567",
            "driverId": "673e456789abcdef01234560",
            "inputMode": "DEVICE_TRANSCRIPT",
            "originalTranscript": "Take me from Jhansi Station to SRGI College",
            "normalizedTranscript": "Take me from Jhansi Station to SRGI College",
            "intent": "CREATE_TRIP",
            "origin": {
              "query": "Jhansi Station",
              "resolved": {
                "latitude": 25.4484,
                "longitude": 78.5685,
                "formattedAddress": "Jhansi Junction Railway Station, Jhansi, UP",
                "displayName": "Jhansi Railway Station",
                "provider": "google_maps",
                "city": "Jhansi",
                "state": "Uttar Pradesh",
                "country": "India"
              }
            },
            "destination": {
              "query": "SRGI College",
              "resolved": {
                "latitude": 25.4984,
                "longitude": 78.6085,
                "formattedAddress": "SRGI College Campus, Gwalior Road, Jhansi, UP",
                "displayName": "SRGI College",
                "provider": "google_maps",
                "city": "Jhansi",
                "state": "Uttar Pradesh",
                "country": "India"
              }
            },
            "status": "CREATED",
            "expiresAt": "2026-09-27T16:15:00.000Z",
            "createdAt": "2026-09-27T16:00:00.000Z",
            "updatedAt": "2026-09-27T16:00:00.000Z"
          },
          "message": "Voice trip draft created successfully."
        }
        """.trimIndent()

        val parsedDto = remoteDataSource.parseVoiceTripDraftResponse(rawBackendJson)

        assertEquals("673f456789abcdef01234567", parsedDto.id)
        assertEquals("DEVICE_TRANSCRIPT", parsedDto.inputMode)
        assertEquals("Take me from Jhansi Station to SRGI College", parsedDto.originalTranscript)
        assertEquals("CREATE_TRIP", parsedDto.intent)
        assertEquals("CREATED", parsedDto.status)

        // Origin check
        assertEquals("Jhansi Station", parsedDto.origin.query)
        assertEquals(25.4484, parsedDto.origin.resolved.latitude, 0.0001)
        assertEquals(78.5685, parsedDto.origin.resolved.longitude, 0.0001)
        assertEquals("Jhansi Railway Station", parsedDto.origin.resolved.displayName)

        // Destination check
        assertEquals("SRGI College", parsedDto.destination.query)
        assertEquals(25.4984, parsedDto.destination.resolved.latitude, 0.0001)
        assertEquals(78.6085, parsedDto.destination.resolved.longitude, 0.0001)
        assertEquals("SRGI College", parsedDto.destination.resolved.displayName)
    }

    @Test
    fun voiceTripDraftMapper_mapsToPureDomainModel() {
        val dto = VoiceTripDraftResponseDto(
            id = "draft_123",
            driverId = "driver_456",
            inputMode = "DEVICE_TRANSCRIPT",
            originalTranscript = "Jhansi to Gwalior",
            normalizedTranscript = "Jhansi to Gwalior",
            intent = "CREATE_TRIP",
            origin = VoiceTripDraftEndpointDto(
                query = "Jhansi",
                resolved = ResolvedLocationDto(
                    latitude = 25.4484,
                    longitude = 78.5685,
                    formattedAddress = "Jhansi, Uttar Pradesh",
                    displayName = "Jhansi City"
                )
            ),
            destination = VoiceTripDraftEndpointDto(
                query = "Gwalior",
                resolved = ResolvedLocationDto(
                    latitude = 26.2183,
                    longitude = 78.1828,
                    formattedAddress = "Gwalior, Madhya Pradesh",
                    displayName = "Gwalior City"
                )
            ),
            status = "CREATED",
            expiresAt = "2026-09-27T16:15:00.000Z",
            createdAt = "2026-09-27T16:00:00.000Z",
            updatedAt = "2026-09-27T16:00:00.000Z"
        )

        val domain = VoiceTripDraftMapper.toDomain(dto)

        assertEquals("draft_123", domain.id)
        assertEquals(VoiceTripDraftStatus.CREATED, domain.status)
        assertEquals("Jhansi City", domain.origin.displayName)
        assertEquals(25.4484, domain.origin.coordinates.latitude, 0.0001)
        assertEquals("Gwalior City", domain.destination.displayName)
        assertEquals(26.2183, domain.destination.coordinates.latitude, 0.0001)

        // Test conversion to DiscoveryQuery
        val query = domain.toDiscoveryQuery()
        assertEquals(25.4484, query.originLatitude, 0.0001)
        assertEquals(78.5685, query.originLongitude, 0.0001)
        assertEquals(26.2183, query.destinationLatitude, 0.0001)
        assertEquals(78.1828, query.destinationLongitude, 0.0001)
        assertEquals("Jhansi City", query.originName)
        assertEquals("Gwalior City", query.destinationName)
    }

    @Test
    fun remoteDataSource_sendsBearerTokenAndHandlesSuccess() = runBlocking {
        val mockHttpClient = MockHttpClient(
            responseCode = 201,
            responseBody = """{"data":{"id":"d1","inputMode":"DEVICE_TRANSCRIPT","originalTranscript":"test","normalizedTranscript":"test","intent":"CREATE_TRIP","status":"CREATED","expiresAt":"","createdAt":"","updatedAt":"","origin":{"query":"A","resolved":{"latitude":25.0,"longitude":78.0,"formattedAddress":"A"}},"destination":{"query":"B","resolved":{"latitude":25.1,"longitude":78.1,"formattedAddress":"B"}}}}"""
        )
        val remoteDataSource = VoiceTripDraftRemoteDataSourceImpl(
            httpClient = mockHttpClient,
            networkConfig = NetworkConfig()
        )

        val result = remoteDataSource.createVoiceTripDraft(
            request = CreateVoiceTripDraftRequestDto(transcript = "test"),
            token = "sample_session_jwt"
        )

        assertTrue(result is IshaaraResult.Success)
        assertNotNull(mockHttpClient.lastRequest)
        assertEquals("Bearer sample_session_jwt", mockHttpClient.lastRequest?.headers?.get("Authorization"))
        assertEquals(HttpMethod.POST, mockHttpClient.lastRequest?.method)
    }

    @Test
    fun remoteDataSource_handles403ForbiddenProperly() = runBlocking {
        val mockHttpClient = MockHttpClient(
            responseCode = 403,
            responseBody = """{"statusCode":403,"success":false,"error":{"code":"FORBIDDEN","message":"Access forbidden: requires one of [DRIVER_CONDUCTOR]"}}"""
        )
        val remoteDataSource = VoiceTripDraftRemoteDataSourceImpl(
            httpClient = mockHttpClient,
            networkConfig = NetworkConfig()
        )

        val result = remoteDataSource.createVoiceTripDraft(
            request = CreateVoiceTripDraftRequestDto(transcript = "test"),
            token = "student_token"
        )

        assertTrue(result is IshaaraResult.Failure)
        val failure = result as IshaaraResult.Failure
        assertTrue(failure.error.message.contains("403"))
    }
}
