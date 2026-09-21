package com.ishara.app.core.realtime

import com.ishara.app.core.location.GeoJsonCoordinate

/**
 * Realtime events matching the 4 Ishaara WebSocket channels.
 */
sealed interface RealtimeEvent {
    /** Channel 19.1: Driver Realtime Voice Streaming */
    data class VoiceTranscription(val partialText: String, val isFinal: Boolean) : RealtimeEvent

    /** Channel 19.2: Passenger Realtime Trip Discovery */
    data class NearbyTripDiscovered(val tripId: String, val routeSummary: String) : RealtimeEvent

    /** Channel 19.3: Driver/Passenger Ride Request Updates */
    data class RideRequestUpdate(val requestId: String, val status: String) : RealtimeEvent

    /** Channel 19.4: Live GPS Ride Tracking & Telemetry */
    data class LiveTelemetryUpdate(
        val rideId: String,
        val coordinate: GeoJsonCoordinate,
        val headingDegrees: Float?,
        val speedMps: Float?,
        val remainingDistanceMeters: Int?,
        val etaSeconds: Int?
    ) : RealtimeEvent

    /** Generic message fallback */
    data class RawMessage(val payload: String) : RealtimeEvent
}
