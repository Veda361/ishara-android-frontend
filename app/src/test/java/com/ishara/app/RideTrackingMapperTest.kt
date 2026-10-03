package com.ishara.app

import com.ishara.app.core.location.LocationCoordinates
import com.ishara.app.data.mapper.RideTrackingMapper
import com.ishara.app.data.remote.dto.RideDriverLocationResponseDto
import com.ishara.app.data.remote.dto.RideTrackingUpdatedPayloadDto
import com.ishara.app.data.remote.dto.TrackingCoordinatesDto
import com.ishara.app.data.remote.dto.TrackingDriverInfoDto
import com.ishara.app.data.remote.dto.TrackingEtaInfoDto
import com.ishara.app.data.remote.dto.TrackingResponseDto
import com.ishara.app.data.remote.dto.TrackingRouteInfoDto
import com.ishara.app.data.remote.dto.TrackingRouteProgressSummaryDto
import com.ishara.app.domain.model.RideTrackingSnapshot
import com.ishara.app.domain.model.TrackingFreshness
import com.ishara.app.domain.model.TrackingRideStatus
import com.ishara.app.domain.model.TrackingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RideTrackingMapperTest {

    @Test
    fun `mapCoordinates valid coordinate maps correctly`() {
        val dto = TrackingCoordinatesDto(latitude = 25.4484, longitude = 78.5685)
        val result = RideTrackingMapper.mapCoordinates(dto)
        assertNotNull(result)
        assertEquals(25.4484, result!!.latitude, 0.0001)
        assertEquals(78.5685, result.longitude, 0.0001)
    }

    @Test
    fun `mapCoordinates invalid zero zero returns null`() {
        val dto = TrackingCoordinatesDto(latitude = 0.0, longitude = 0.0)
        val result = RideTrackingMapper.mapCoordinates(dto)
        assertNull(result)
    }

    @Test
    fun `mapCoordinates out of bounds returns null`() {
        val invalidLat = TrackingCoordinatesDto(latitude = 95.0, longitude = 78.0)
        assertNull(RideTrackingMapper.mapCoordinates(invalidLat))

        val invalidLng = TrackingCoordinatesDto(latitude = 25.0, longitude = 195.0)
        assertNull(RideTrackingMapper.mapCoordinates(invalidLng))
    }

    @Test
    fun `mapDriverInfo maps all telemetry fields accurately`() {
        val dto = TrackingDriverInfoDto(
            location = TrackingCoordinatesDto(25.4484, 78.5685),
            accuracyMeters = 5.5,
            headingDegrees = 180.0,
            speedMps = 12.0,
            recordedAt = "2026-09-25T14:00:00Z",
            receivedAt = "2026-09-25T14:00:01Z",
            freshness = "FRESH"
        )
        val result = RideTrackingMapper.mapDriverInfo(dto)
        assertNotNull(result)
        assertEquals(25.4484, result!!.coordinates.latitude, 0.0001)
        assertEquals(5.5f, result.accuracyMeters)
        assertEquals(180f, result.headingDegrees)
        assertEquals(12f, result.speedMps)
        assertEquals(TrackingFreshness.FRESH, result.freshness)
    }

    @Test
    fun `mapDriverLocationResponse maps driver fix correctly`() {
        val dto = RideDriverLocationResponseDto(
            rideId = "ride-123",
            driverId = "driver-456",
            location = TrackingCoordinatesDto(18.5204, 73.8567),
            accuracyMeters = 8.0,
            headingDegrees = 90.0,
            speedMps = 15.0,
            recordedAt = "2026-09-25T14:10:00Z",
            status = "FRESH"
        )
        val result = RideTrackingMapper.mapDriverLocationResponse(dto)
        assertNotNull(result)
        assertEquals(18.5204, result!!.coordinates.latitude, 0.0001)
        assertEquals(TrackingFreshness.FRESH, result.freshness)
    }

    @Test
    fun `mapRouteInfo maps completed and remaining distance`() {
        val dto = TrackingRouteInfoDto(
            distanceMeters = 10000.0,
            completedDistanceMeters = 4000.0,
            remainingDistanceMeters = 6000.0,
            progressPercent = 40.0,
            distanceFromRouteMeters = 5.0,
            isOffRoute = false
        )
        val result = RideTrackingMapper.mapRouteInfo(dto)
        assertNotNull(result)
        assertEquals(10000, result!!.totalDistanceMeters)
        assertEquals(4000, result.completedDistanceMeters)
        assertEquals(6000, result.remainingDistanceMeters)
        assertEquals(40.0f, result.progressPercent)
        assertFalse(result.isOffRoute)
    }

    @Test
    fun `mapEta formats minutes correctly and rejects unavailable eta`() {
        val availableDto = TrackingEtaInfoDto(available = true, seconds = 130, confidence = "MEDIUM")
        val availableEta = RideTrackingMapper.mapEta(availableDto)
        assertTrue(availableEta.available)
        assertEquals(130, availableEta.seconds)
        assertEquals("3 mins", availableEta.formattedEtaMinutes)

        val oneMinDto = TrackingEtaInfoDto(available = true, seconds = 45)
        assertEquals("1 min", RideTrackingMapper.mapEta(oneMinDto).formattedEtaMinutes)

        val unavailableDto = TrackingEtaInfoDto(available = false, seconds = null)
        val unavailableEta = RideTrackingMapper.mapEta(unavailableDto)
        assertFalse(unavailableEta.available)
        assertNull(unavailableEta.formattedEtaMinutes)
    }

    @Test
    fun `mapTrackingResponse converts complete REST snapshot`() {
        val dto = TrackingResponseDto(
            rideId = "ride-abc",
            status = "IN_PROGRESS",
            trackingState = "FRESH",
            driver = TrackingDriverInfoDto(
                location = TrackingCoordinatesDto(25.4484, 78.5685),
                freshness = "FRESH"
            ),
            route = TrackingRouteInfoDto(
                distanceMeters = 5000.0,
                completedDistanceMeters = 2000.0,
                remainingDistanceMeters = 3000.0,
                progressPercent = 40.0
            ),
            distanceToPickupMeters = null,
            distanceToDestinationMeters = 3000.0,
            eta = TrackingEtaInfoDto(available = true, seconds = 300),
            updatedAt = "2026-09-25T14:20:00Z"
        )

        val snapshot = RideTrackingMapper.mapTrackingResponse(
            dto = dto,
            pickupAddress = "Hostel Gate 2",
            destinationAddress = "Engineering Complex",
            pickupCoords = LocationCoordinates(25.44, 78.56),
            destinationCoords = LocationCoordinates(25.48, 78.60)
        )

        assertEquals("ride-abc", snapshot.rideId)
        assertEquals(TrackingRideStatus.IN_PROGRESS, snapshot.status)
        assertEquals(TrackingState.FRESH, snapshot.trackingState)
        assertEquals("Hostel Gate 2", snapshot.pickupAddress)
        assertEquals("Engineering Complex", snapshot.destinationAddress)
        assertEquals(3000, snapshot.distanceToDestinationMeters)
        assertEquals("5 mins", snapshot.eta.formattedEtaMinutes)
    }

    @Test
    fun `reconcileWithRealtimeUpdate updates driver position and route progress without losing static metadata`() {
        val initialSnapshot = RideTrackingSnapshot(
            rideId = "ride-xyz",
            status = TrackingRideStatus.DRIVER_ARRIVING,
            trackingState = TrackingState.FRESH,
            driverLocation = null,
            routeProgress = null,
            distanceToPickupMeters = 1500,
            distanceToDestinationMeters = null,
            eta = RideTrackingMapper.mapEta(TrackingEtaInfoDto(available = true, seconds = 240)),
            updatedAt = "2026-09-25T14:00:00Z",
            pickupAddress = "Campus North",
            destinationAddress = "Library"
        )

        val update = RideTrackingUpdatedPayloadDto(
            rideId = "ride-xyz",
            driverLocation = TrackingCoordinatesDto(25.4500, 78.5700),
            freshness = "FRESH",
            trackingState = "FRESH",
            routeProgress = TrackingRouteProgressSummaryDto(
                completedDistanceMeters = 500.0,
                remainingDistanceMeters = 1000.0,
                progressPercent = 33.3
            ),
            distanceToPickupMeters = 1000.0,
            distanceToDestinationMeters = null,
            eta = TrackingEtaInfoDto(available = true, seconds = 180),
            recordedAt = "2026-09-25T14:02:00Z"
        )

        val reconciled = RideTrackingMapper.reconcileWithRealtimeUpdate(initialSnapshot, update)

        assertEquals(25.4500, reconciled.driverLocation!!.coordinates.latitude, 0.0001)
        assertEquals(78.5700, reconciled.driverLocation!!.coordinates.longitude, 0.0001)
        assertEquals(1000, reconciled.distanceToPickupMeters)
        assertEquals("3 mins", reconciled.eta.formattedEtaMinutes)
        assertEquals("Campus North", reconciled.pickupAddress)
        assertEquals("Library", reconciled.destinationAddress)
        assertEquals("2026-09-25T14:02:00Z", reconciled.updatedAt)
    }
}
