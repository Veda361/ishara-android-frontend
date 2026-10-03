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
    data class RideRequestCreated(val requestId: String, val tripId: String, val expiresAt: String? = null) : RealtimeEvent
    data class RideRequestCancelled(val requestId: String, val reason: String? = null) : RealtimeEvent
    data class RideRequestExpired(val requestId: String) : RealtimeEvent
    data class RideCreated(val rideId: String, val tripId: String) : RealtimeEvent
    data class RideDriverArriving(val rideId: String, val tripId: String? = null) : RealtimeEvent
    data class RidePickedUp(val rideId: String, val tripId: String? = null) : RealtimeEvent
    data class RideStarted(val rideId: String, val tripId: String? = null) : RealtimeEvent
    data class RideCompleted(val rideId: String, val tripId: String? = null) : RealtimeEvent
    data class RideCancelled(val rideId: String, val reason: String? = null) : RealtimeEvent

    /** Channel 19.4: Live GPS Ride Tracking & Telemetry */
    data class LiveTelemetryUpdate(
        val rideId: String,
        val coordinate: GeoJsonCoordinate,
        val headingDegrees: Float? = null,
        val speedMps: Float? = null,
        val remainingDistanceMeters: Int? = null,
        val etaSeconds: Int? = null,
        val routeProgressPercentage: Float? = null
    ) : RealtimeEvent

    /** Phase 11: Ride Tracking Channel Events */
    data class RideTrackingSubscribed(val rideId: String, val driverId: String?, val timestamp: String?) : RealtimeEvent
    data class TrackingSnapshot(val snapshot: com.ishara.app.data.remote.dto.TrackingResponseDto) : RealtimeEvent
    data class RideTrackingUpdated(val update: com.ishara.app.data.remote.dto.RideTrackingUpdatedPayloadDto) : RealtimeEvent
    data class RideTrackingEnded(val rideId: String, val status: String, val reason: String?, val timestamp: String?) : RealtimeEvent
    data class RideTrackingError(val rideId: String?, val code: String?, val message: String?) : RealtimeEvent
    data class DriverLocationUpdated(
        val rideId: String,
        val location: com.ishara.app.data.remote.dto.TrackingCoordinatesDto,
        val recordedAt: String?,
        val isStale: Boolean
    ) : RealtimeEvent

    /** Phase 15: Safety & SOS Realtime Events */
    data class SosCreated(
        val eventId: String,
        val rideId: String,
        val emergencyType: String,
        val status: String,
        val triggeredByUserId: String,
        val triggeredByRole: String,
        val locationSnapshot: com.ishara.app.data.remote.dto.SafetyLocationSnapshotDto?,
        val triggeredAt: String
    ) : RealtimeEvent

    data class SosCancelled(
        val eventId: String,
        val rideId: String,
        val emergencyType: String,
        val status: String,
        val cancelledAt: String?,
        val cancellationReason: String?
    ) : RealtimeEvent

    /** Safely captured unknown event types from the server */
    data class UnknownEvent(val eventType: String, val payload: String) : RealtimeEvent

    /** Generic message fallback */
    data class RawMessage(val payload: String) : RealtimeEvent
}
