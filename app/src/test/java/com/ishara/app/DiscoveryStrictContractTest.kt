package com.ishara.app

import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.mapper.DiscoveryMapper
import com.ishara.app.data.remote.datasource.DiscoveryRemoteDataSourceImpl
import com.ishara.app.data.remote.dto.DiscoveryCoordinateDto
import com.ishara.app.data.remote.dto.DiscoveryOptionsDto
import com.ishara.app.data.remote.dto.DiscoverySearchRequestDto
import com.ishara.app.domain.model.DiscoveryQuery
import com.ishara.app.domain.model.TripCompatibility
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests strictly validating compliance with the backend contract:
 * 1. Request schema must strictly contain ONLY origin, destination, options.
 * 2. Request body must NEVER contain fare, seatCapacity, price, vehicleCapacity, etc.
 * 3. Request coordinates must be { latitude, longitude } numbers.
 * 4. Response coordinates must adhere strictly to GeoJSON RFC 7946: [longitude, latitude].
 */
class DiscoveryStrictContractTest {

    private val fakeHttpClient = object : IshaaraHttpClient {
        override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
            return IshaaraResult.success(HttpResponse(statusCode = 200, body = "{}"))
        }
    }
    private val dataSource = DiscoveryRemoteDataSourceImpl(fakeHttpClient, NetworkConfig())

    @Test
    fun `serialized request strictly excludes unsupported fields`() {
        val query = DiscoveryQuery(
            originLatitude = 25.4484,
            originLongitude = 78.5685,
            originName = "Jhansi Station",
            originAddress = "Station Road, Jhansi",
            destinationLatitude = 25.4358,
            destinationLongitude = 78.5522,
            destinationName = "Sipri Bazaar",
            destinationAddress = "Sipri, Jhansi",
            maxPickupDistanceMeters = 1000.0,
            maxResults = 20
        )

        val requestDto = DiscoveryMapper.toRequestDto(query)
        val json = dataSource.serializeRequest(requestDto)

        // Must contain verified fields
        assertTrue(json.contains("\"origin\""))
        assertTrue(json.contains("\"destination\""))
        assertTrue(json.contains("\"options\""))
        assertTrue(json.contains("\"latitude\":25.4484"))
        assertTrue(json.contains("\"longitude\":78.5685"))
        assertTrue(json.contains("\"name\":\"Jhansi Station\""))
        assertTrue(json.contains("\"maxPickupDistanceMeters\":1000.0"))
        assertTrue(json.contains("\"maxResults\":20"))

        // STRICT REGRESSION CHECK: Must NOT contain unsupported fields rejected by Zod .strict()
        assertFalse("Must not contain fare", json.contains("fare"))
        assertFalse("Must not contain price", json.contains("price"))
        assertFalse("Must not contain seatCapacity", json.contains("seatCapacity"))
        assertFalse("Must not contain vehicleCapacity", json.contains("vehicleCapacity"))
        assertFalse("Must not contain passengerCount", json.contains("passengerCount"))
        assertFalse("Must not contain pickup", json.contains("\"pickup\""))
        assertFalse("Must not contain seatsRequested", json.contains("seatsRequested"))
    }

    @Test
    fun `response parser correctly maps GeoJSON longitude latitude coordinates`() {
        val mockResponseJson = """
        {
            "success": true,
            "statusCode": 200,
            "data": {
                "discoverySessionId": "dses_test123",
                "items": [
                    {
                        "tripId": "trip_001",
                        "driver": {
                            "id": "driver_1",
                            "name": "Ramesh Kumar"
                        },
                        "vehicle": {
                            "id": "veh_1",
                            "registrationNumber": "UP93AT1234",
                            "vehicleType": "BUS",
                            "make": "Tata",
                            "model": "Starbus"
                        },
                        "origin": {
                            "name": "Jhansi Station",
                            "formattedAddress": "Station Road",
                            "coordinates": {
                                "type": "Point",
                                "coordinates": [78.5685, 25.4484]
                            }
                        },
                        "destination": {
                            "name": "Sipri Bazaar",
                            "formattedAddress": "Sipri",
                            "coordinates": {
                                "type": "Point",
                                "coordinates": [78.5522, 25.4358]
                            }
                        },
                        "routeSummary": {
                            "distanceMeters": 4500,
                            "durationSeconds": 900
                        },
                        "match": {
                            "pickupDistanceMeters": 50,
                            "destinationDistanceMeters": 80,
                            "directionDifferenceDegrees": 5,
                            "pickupRouteProgress": 0.1,
                            "destinationRouteProgress": 0.9,
                            "estimatedDetourMeters": 130,
                            "compatibility": "HIGH",
                            "score": 0.95
                        }
                    }
                ],
                "pagination": {
                    "limit": 20,
                    "hasMore": false,
                    "nextCursor": null
                }
            }
        }
        """.trimIndent()

        val parsedDto = dataSource.parseDiscoveryResponse(mockResponseJson)
        assertEquals("dses_test123", parsedDto.discoverySessionId)
        assertEquals(1, parsedDto.items.size)

        val domainResult = DiscoveryMapper.toDomain(parsedDto)
        assertEquals(1, domainResult.items.size)

        val trip = domainResult.items[0]
        assertEquals("trip_001", trip.tripId)
        assertEquals("Ramesh Kumar", trip.driver.name)
        assertEquals("UP93AT1234", trip.vehicle.registrationNumber)

        // Verify GeoJSON coordinate mapping: index 0 is longitude, index 1 is latitude
        assertEquals(25.4484, trip.originCoordinate.latitude, 0.0001)
        assertEquals(78.5685, trip.originCoordinate.longitude, 0.0001)
        assertEquals(25.4358, trip.destinationCoordinate.latitude, 0.0001)
        assertEquals(78.5522, trip.destinationCoordinate.longitude, 0.0001)

        // Verify metrics
        assertEquals("4.5 km", trip.formattedDistance)
        assertEquals("15 min", trip.formattedDuration)
        assertEquals("50 m walk", trip.formattedPickupDistance)
        assertEquals(TripCompatibility.HIGH, trip.compatibility)
        assertEquals(0.95, trip.matchScore, 0.0001)
    }

    @Test
    fun `query model rejects out of bounds coordinates`() {
        var errorThrown = false
        try {
            DiscoveryQuery(
                originLatitude = 95.0, // Invalid latitude > 90
                originLongitude = 78.0,
                destinationLatitude = 25.0,
                destinationLongitude = 78.0
            )
        } catch (e: IllegalArgumentException) {
            errorThrown = true
        }
        assertTrue("Must reject invalid latitude", errorThrown)
    }
}
