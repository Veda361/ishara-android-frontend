package com.ishara.app

import com.ishara.app.core.realtime.RealtimeEvent
import com.ishara.app.core.realtime.RealtimeEventParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeEventParserTrackingTest {

    @Test
    fun `parse TRACKING_SNAPSHOT frame parses full snapshot`() {
        val json = """
            {
                "type": "TRACKING_SNAPSHOT",
                "sessionId": "sess-1",
                "sequence": 0,
                "timestamp": "2026-09-25T14:15:31.000Z",
                "payload": {
                    "rideId": "ride-101",
                    "status": "IN_PROGRESS",
                    "trackingState": "FRESH",
                    "driver": {
                        "location": { "latitude": 25.4484, "longitude": 78.5685 },
                        "accuracyMeters": 6.5,
                        "headingDegrees": 182.0,
                        "speedMps": 11.2,
                        "freshness": "FRESH"
                    },
                    "route": {
                        "distanceMeters": 8500,
                        "completedDistanceMeters": 3200,
                        "remainingDistanceMeters": 5300,
                        "progressPercent": 37.6,
                        "isOffRoute": false
                    },
                    "distanceToPickupMeters": null,
                    "distanceToDestinationMeters": 5300,
                    "eta": {
                        "available": true,
                        "seconds": 473
                    },
                    "updatedAt": "2026-09-25T14:15:31.100Z"
                }
            }
        """.trimIndent()

        val event = RealtimeEventParser.parse(json)
        assertTrue(event is RealtimeEvent.TrackingSnapshot)
        val snapshot = (event as RealtimeEvent.TrackingSnapshot).snapshot
        assertEquals("ride-101", snapshot.rideId)
        assertEquals("IN_PROGRESS", snapshot.status)
        assertEquals("FRESH", snapshot.trackingState)
        assertNotNull(snapshot.driver)
        assertEquals(25.4484, snapshot.driver!!.location!!.latitude, 0.0001)
        assertEquals(5300.0, snapshot.distanceToDestinationMeters)
        assertEquals(473, snapshot.eta!!.seconds)
    }

    @Test
    fun `parse RIDE_TRACKING_UPDATED frame parses live update`() {
        val json = """
            {
                "type": "RIDE_TRACKING_UPDATED",
                "sessionId": "sess-1",
                "sequence": 1,
                "timestamp": "2026-09-25T14:15:35.000Z",
                "payload": {
                    "rideId": "ride-101",
                    "driverLocation": { "latitude": 25.4489, "longitude": 78.5689 },
                    "freshness": "FRESH",
                    "trackingState": "FRESH",
                    "routeProgress": {
                        "completedDistanceMeters": 3280,
                        "remainingDistanceMeters": 5220,
                        "progressPercent": 38.6
                    },
                    "distanceToPickupMeters": null,
                    "distanceToDestinationMeters": 5220,
                    "eta": { "available": true, "seconds": 465 },
                    "recordedAt": "2026-09-25T14:15:35.000Z"
                }
            }
        """.trimIndent()

        val event = RealtimeEventParser.parse(json)
        assertTrue(event is RealtimeEvent.RideTrackingUpdated)
        val update = (event as RealtimeEvent.RideTrackingUpdated).update
        assertEquals("ride-101", update.rideId)
        assertEquals(25.4489, update.driverLocation!!.latitude, 0.0001)
        assertEquals(78.5689, update.driverLocation!!.longitude, 0.0001)
        assertEquals(5220.0, update.distanceToDestinationMeters)
        assertEquals(465, update.eta!!.seconds)
    }

    @Test
    fun `parse RIDE_TRACKING_ENDED frame parses terminal ride status`() {
        val json = """
            {
                "type": "RIDE_TRACKING_ENDED",
                "payload": {
                    "rideId": "ride-101",
                    "status": "COMPLETED",
                    "reason": "Passenger arrived at destination",
                    "timestamp": "2026-09-25T14:20:00.000Z"
                }
            }
        """.trimIndent()

        val event = RealtimeEventParser.parse(json)
        assertTrue(event is RealtimeEvent.RideTrackingEnded)
        val ended = event as RealtimeEvent.RideTrackingEnded
        assertEquals("ride-101", ended.rideId)
        assertEquals("COMPLETED", ended.status)
        assertEquals("Passenger arrived at destination", ended.reason)
    }

    @Test
    fun `parse RIDE_TRACKING_SUBSCRIBED frame parses subscription confirmation`() {
        val json = """
            {
                "type": "RIDE_TRACKING_SUBSCRIBED",
                "payload": {
                    "rideId": "ride-101",
                    "driverId": "driver-999",
                    "timestamp": "2026-09-25T14:15:30.500Z"
                }
            }
        """.trimIndent()

        val event = RealtimeEventParser.parse(json)
        assertTrue(event is RealtimeEvent.RideTrackingSubscribed)
        val sub = event as RealtimeEvent.RideTrackingSubscribed
        assertEquals("ride-101", sub.rideId)
        assertEquals("driver-999", sub.driverId)
    }

    @Test
    fun `parse RIDE_TRACKING_ERROR frame parses error payload`() {
        val json = """
            {
                "type": "RIDE_TRACKING_ERROR",
                "payload": {
                    "code": "RIDE_NOT_AUTHORIZED",
                    "message": "You are not authorized to track this ride",
                    "rideId": "ride-101"
                }
            }
        """.trimIndent()

        val event = RealtimeEventParser.parse(json)
        assertTrue(event is RealtimeEvent.RideTrackingError)
        val err = event as RealtimeEvent.RideTrackingError
        assertEquals("ride-101", err.rideId)
        assertEquals("RIDE_NOT_AUTHORIZED", err.code)
    }

    @Test
    fun `parse DRIVER_LOCATION_UPDATED frame parses driver GPS fix`() {
        val json = """
            {
                "type": "DRIVER_LOCATION_UPDATED",
                "payload": {
                    "rideId": "ride-101",
                    "location": { "latitude": 18.5204, "longitude": 73.8567 },
                    "recordedAt": "2026-09-25T14:15:30.000Z",
                    "isStale": false
                }
            }
        """.trimIndent()

        val event = RealtimeEventParser.parse(json)
        assertTrue(event is RealtimeEvent.DriverLocationUpdated)
        val loc = event as RealtimeEvent.DriverLocationUpdated
        assertEquals("ride-101", loc.rideId)
        assertEquals(18.5204, loc.location.latitude, 0.0001)
        assertEquals(false, loc.isStale)
    }

    @Test
    fun `malformed json gracefully falls back to RawMessage without throwing`() {
        val event = RealtimeEventParser.parse("NOT_JSON")
        assertTrue(event is RealtimeEvent.RawMessage)
    }
}
