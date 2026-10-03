package com.ishara.app

import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpResponse
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.mapper.TransitMapper
import com.ishara.app.data.remote.datasource.TransitRemoteDataSourceImpl
import com.ishara.app.domain.model.TransitOperatingStatus
import com.ishara.app.domain.model.TransitStopType
import com.ishara.app.domain.model.TransitVehicleType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitStrictContractTest {

    private val sampleActiveTripsJson = """
        {
            "success": true,
            "data": [
                {
                    "id": "6741ab0123456789abcdef01",
                    "status": "ACTIVE",
                    "origin": {
                        "name": "Jhansi Railway Station",
                        "formattedAddress": "Station Road, Jhansi, UP",
                        "coordinates": {
                            "type": "Point",
                            "coordinates": [78.5833, 25.4484]
                        },
                        "googlePlaceId": "ChIJ_z123456"
                    },
                    "destination": {
                        "name": "Bundelkhand University",
                        "formattedAddress": "Kanpur Road, Jhansi, UP",
                        "coordinates": {
                            "type": "Point",
                            "coordinates": [78.6012, 25.4611]
                        },
                        "googlePlaceId": "ChIJ_z654321"
                    },
                    "route": {
                        "geometry": {
                            "type": "LineString",
                            "coordinates": [
                                [78.5833, 25.4484],
                                [78.5900, 25.4520],
                                [78.6012, 25.4611]
                            ]
                        },
                        "distanceMeters": 4500.0,
                        "durationSeconds": 900,
                        "provider": "google_routes"
                    },
                    "startedAt": "2026-09-27T10:00:00.000Z",
                    "createdAt": "2026-09-27T09:45:00.000Z",
                    "driver": {
                        "id": "6741drv001",
                        "name": "Ramesh Kumar",
                        "image": "https://example.com/driver.png"
                    },
                    "vehicle": {
                        "id": "6741veh001",
                        "registrationNumber": "UP93AT1234",
                        "vehicleType": "BUS",
                        "make": "Tata",
                        "model": "Starbus"
                    }
                }
            ]
        }
    """.trimIndent()

    private val fakeHttpClient = object : IshaaraHttpClient {
        override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> {
            return IshaaraResult.success(HttpResponse(statusCode = 200, headers = emptyMap(), body = sampleActiveTripsJson))
        }
    }

    private val remoteDataSource = TransitRemoteDataSourceImpl(fakeHttpClient, NetworkConfig())

    @Test
    fun testParseActiveTripsStrictContract() {
        val dtos = remoteDataSource.parseActiveTripsResponse(sampleActiveTripsJson)
        assertEquals(1, dtos.size)

        val dto = dtos[0]
        assertEquals("6741ab0123456789abcdef01", dto.id)
        assertEquals("ACTIVE", dto.status)
        assertEquals("Jhansi Railway Station", dto.origin.name)
        assertEquals("Station Road, Jhansi, UP", dto.origin.formattedAddress)
        assertEquals(78.5833, dto.origin.coordinates[0], 0.0001)
        assertEquals(25.4484, dto.origin.coordinates[1], 0.0001)

        assertEquals("Bundelkhand University", dto.destination.name)
        assertEquals("Kanpur Road, Jhansi, UP", dto.destination.formattedAddress)
        assertEquals(78.6012, dto.destination.coordinates[0], 0.0001)
        assertEquals(25.4611, dto.destination.coordinates[1], 0.0001)

        assertNotNull(dto.route)
        assertEquals(4500.0, dto.route?.distanceMeters ?: 0.0, 0.01)
        assertEquals(900L, dto.route?.durationSeconds)
        assertEquals(3, dto.route?.geometryCoordinates?.size)

        assertEquals("Ramesh Kumar", dto.driver.name)
        assertEquals("UP93AT1234", dto.vehicle.registrationNumber)
        assertEquals("BUS", dto.vehicle.vehicleType)
    }

    @Test
    fun testTransitMapperToDomain() {
        val dtos = remoteDataSource.parseActiveTripsResponse(sampleActiveTripsJson)
        val route = TransitMapper.toDomain(dtos[0], cachedAtMillis = 1000L)

        assertEquals("6741ab0123456789abcdef01", route.id)
        assertEquals("Jhansi Railway Station ↔ Bundelkhand University", route.name)
        assertEquals(TransitOperatingStatus.ACTIVE, route.operatingStatus)
        assertEquals(TransitVehicleType.BUS, route.vehicleType)
        assertEquals("UP93AT1234", route.vehiclePlate)
        assertEquals("Ramesh Kumar", route.driverName)
        assertEquals(4500.0, route.distanceMeters ?: 0.0, 0.01)
        assertEquals(900L, route.durationSeconds)

        // Verify Coordinate Inversion Safety: GeoJSON [lng, lat] -> Domain (lat, lng)
        assertEquals(25.4484, route.originStop.coordinates.latitude, 0.0001)
        assertEquals(78.5833, route.originStop.coordinates.longitude, 0.0001)
        assertEquals(0, route.originStop.sequence)
        assertEquals(TransitStopType.ORIGIN, route.originStop.stopType)

        assertEquals(25.4611, route.destinationStop.coordinates.latitude, 0.0001)
        assertEquals(78.6012, route.destinationStop.coordinates.longitude, 0.0001)
        assertEquals(1, route.destinationStop.sequence)
        assertEquals(TransitStopType.DESTINATION, route.destinationStop.stopType)

        // Stops sequence
        assertEquals(2, route.stops.size)
        assertEquals(0, route.stops[0].sequence)
        assertEquals(1, route.stops[1].sequence)

        // Geometry points converted properly
        assertEquals(3, route.geometry.size)
        assertEquals(25.4484, route.geometry[0].latitude, 0.0001)
        assertEquals(78.5833, route.geometry[0].longitude, 0.0001)
    }

    @Test
    fun testTransitScheduleDerivedHonesty() {
        val dtos = remoteDataSource.parseActiveTripsResponse(sampleActiveTripsJson)
        val route = TransitMapper.toDomain(dtos[0])
        val schedule = TransitMapper.toSchedule(route)

        assertEquals(route.id, schedule.routeId)
        assertEquals(TransitOperatingStatus.ACTIVE, schedule.status)
        assertNotNull(schedule.startedAtEpochMillis)
        assertTrue(schedule.operatingNote.contains("Active transit service operating now"))
    }

    @Test
    fun testNullableAndMissingRouteHandling() {
        val jsonWithoutRoute = """
            {
                "success": true,
                "data": [
                    {
                        "id": "trip_no_route",
                        "status": "CREATED",
                        "origin": {
                            "formattedAddress": "Campus Gate, Jhansi",
                            "coordinates": { "type": "Point", "coordinates": [78.5, 25.4] }
                        },
                        "destination": {
                            "formattedAddress": "Library, Jhansi",
                            "coordinates": { "type": "Point", "coordinates": [78.51, 25.41] }
                        },
                        "driver": { "id": "d1", "name": "Driver 1" },
                        "vehicle": { "id": "v1", "registrationNumber": "UP93B9999", "vehicleType": "AUTO", "make": "Bajaj", "model": "Compact" }
                    }
                ]
            }
        """.trimIndent()

        val dtos = remoteDataSource.parseActiveTripsResponse(jsonWithoutRoute)
        assertEquals(1, dtos.size)
        val route = TransitMapper.toDomain(dtos[0])

        assertNull(route.distanceMeters)
        assertNull(route.durationSeconds)
        assertTrue(route.geometry.isEmpty())
        assertEquals(TransitOperatingStatus.SCHEDULED, route.operatingStatus)
        assertEquals(TransitVehicleType.AUTO, route.vehicleType)
    }

    @Test
    fun testRemoteDataSourceListActiveTrips() = runBlocking {
        val result = remoteDataSource.listActiveTrips(token = "test_token")
        assertTrue(result is IshaaraResult.Success)
        val list = (result as IshaaraResult.Success).data
        assertEquals(1, list.size)
        assertEquals("6741ab0123456789abcdef01", list[0].id)
    }
}
