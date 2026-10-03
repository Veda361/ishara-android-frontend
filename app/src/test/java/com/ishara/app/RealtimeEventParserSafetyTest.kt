package com.ishara.app

import com.ishara.app.core.realtime.RealtimeEvent
import com.ishara.app.core.realtime.RealtimeEventParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealtimeEventParserSafetyTest {

    @Test
    fun `parses SOS_CREATED event successfully`() {
        val json = """
            {
              "event": "SOS_CREATED",
              "data": {
                "eventId": "se_rt_001",
                "rideId": "ride_rt_99",
                "emergencyType": "SOS",
                "status": "ACTIVE",
                "triggeredByUserId": "usr_passenger_1",
                "triggeredByRole": "USER",
                "locationSnapshot": {
                  "coordinates": [77.5946, 12.9716],
                  "accuracyMeters": 4.0,
                  "headingDegrees": 180.0,
                  "speedMps": 11.2,
                  "isStale": false,
                  "capturedAt": "2026-09-28T12:00:00.000Z",
                  "provider": "driver_profile"
                },
                "triggeredAt": "2026-09-28T12:00:00.000Z"
              },
              "timestamp": "2026-09-28T12:00:00.000Z"
            }
        """.trimIndent()

        val event = RealtimeEventParser.parse(json)
        assertTrue(event is RealtimeEvent.SosCreated)
        val created = event as RealtimeEvent.SosCreated
        assertEquals("se_rt_001", created.eventId)
        assertEquals("ride_rt_99", created.rideId)
        assertEquals("SOS", created.emergencyType)
        assertEquals("ACTIVE", created.status)
        assertEquals("usr_passenger_1", created.triggeredByUserId)
        assertEquals("USER", created.triggeredByRole)
        assertNotNull(created.locationSnapshot)
        assertEquals(77.5946, created.locationSnapshot?.coordinates?.get(0) ?: 0.0, 0.0001)
        assertEquals(12.9716, created.locationSnapshot?.coordinates?.get(1) ?: 0.0, 0.0001)
    }

    @Test
    fun `parses SOS_CANCELLED event successfully`() {
        val json = """
            {
              "event": "SOS_CANCELLED",
              "data": {
                "eventId": "se_rt_001",
                "rideId": "ride_rt_99",
                "emergencyType": "SOS",
                "status": "CANCELLED",
                "cancelledAt": "2026-09-28T12:05:00.000Z",
                "cancellationReason": "Accidental tap"
              },
              "timestamp": "2026-09-28T12:05:00.000Z"
            }
        """.trimIndent()

        val event = RealtimeEventParser.parse(json)
        assertTrue(event is RealtimeEvent.SosCancelled)
        val cancelled = event as RealtimeEvent.SosCancelled
        assertEquals("se_rt_001", cancelled.eventId)
        assertEquals("ride_rt_99", cancelled.rideId)
        assertEquals("CANCELLED", cancelled.status)
        assertEquals("2026-09-28T12:05:00.000Z", cancelled.cancelledAt)
        assertEquals("Accidental tap", cancelled.cancellationReason)
    }
}
