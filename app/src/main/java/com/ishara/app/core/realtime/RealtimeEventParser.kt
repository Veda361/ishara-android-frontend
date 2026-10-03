package com.ishara.app.core.realtime

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.location.GeoJsonCoordinate
import com.ishara.app.data.remote.dto.RideTrackingUpdatedPayloadDto
import com.ishara.app.data.remote.dto.TrackingCoordinatesDto
import com.ishara.app.data.remote.dto.TrackingDriverInfoDto
import com.ishara.app.data.remote.dto.TrackingEtaInfoDto
import com.ishara.app.data.remote.dto.TrackingResponseDto
import com.ishara.app.data.remote.dto.TrackingRouteInfoDto
import com.ishara.app.data.remote.dto.TrackingRouteProgressSummaryDto

/**
 * Pure Kotlin parser for incoming WebSocket frames across all 4 Ishaara realtime channels.
 *
 * Guarantees:
 * 1. Zero crash on malformed JSON or unexpected payloads.
 * 2. Handles both snake_case and camelCase backend serialization.
 * 3. Graceful fallback to [RealtimeEvent.UnknownEvent] or [RealtimeEvent.RawMessage].
 */
object RealtimeEventParser {
    private const val TAG = "RealtimeEventParser"

    fun parse(text: String): RealtimeEvent {
        val trimmed = text.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            return RealtimeEvent.RawMessage(text)
        }

        return try {
            val eventType = extractField(trimmed, "event")
                ?: extractField(trimmed, "type")
                ?: extractField(trimmed, "action")

            when (eventType?.lowercase()) {
                "live_telemetry_update", "telemetry_update", "telemetry" -> parseTelemetry(trimmed)
                "ride_tracking_subscribed" -> parseRideTrackingSubscribed(trimmed)
                "tracking_snapshot" -> parseTrackingSnapshot(trimmed)
                "ride_tracking_updated" -> parseRideTrackingUpdated(trimmed)
                "ride_tracking_ended" -> parseRideTrackingEnded(trimmed)
                "ride_tracking_error" -> parseRideTrackingError(trimmed)
                "driver_location_updated" -> parseDriverLocationUpdated(trimmed)
                "ride_request_update", "ride_request" -> parseRideRequest(trimmed)
                "ride_request_created" -> parseRideRequestCreated(trimmed)
                "ride_request_cancelled" -> parseRideRequestCancelled(trimmed)
                "ride_request_expired" -> parseRideRequestExpired(trimmed)
                "ride_created" -> parseRideCreated(trimmed)
                "ride_driver_arriving" -> parseRideDriverArriving(trimmed)
                "ride_picked_up" -> parseRidePickedUp(trimmed)
                "ride_started" -> parseRideStarted(trimmed)
                "ride_completed" -> parseRideCompleted(trimmed)
                "ride_cancelled" -> parseRideCancelled(trimmed)
                "nearby_trip_discovered", "trip_discovery" -> parseDiscovery(trimmed)
                "voice_transcription", "transcription" -> parseVoice(trimmed)
                "sos_created" -> parseSosCreated(trimmed)
                "sos_cancelled" -> parseSosCancelled(trimmed)
                null -> {
                    // Inspect payload fields if no explicit event discriminator is present
                    if (trimmed.contains("\"coordinates\"") && (trimmed.contains("\"rideId\"") || trimmed.contains("\"ride_id\""))) {
                        parseTelemetry(trimmed)
                    } else {
                        RealtimeEvent.RawMessage(text)
                    }
                }
                else -> RealtimeEvent.UnknownEvent(eventType = eventType, payload = text)
            }
        } catch (e: Exception) {
            IshaaraLogger.w(TAG, "Failed to parse realtime event payload", e)
            RealtimeEvent.RawMessage(text)
        }
    }

    private fun parseTelemetry(json: String): RealtimeEvent {
        val rideId = extractField(json, "rideId") ?: extractField(json, "ride_id") ?: ""
        val coords = extractCoordinateArray(json, "coordinates") ?: extractCoordinateArray(json, "coordinate")

        if (coords == null) {
            return RealtimeEvent.RawMessage(json)
        }

        val heading = extractNumericField(json, "headingDegrees")
            ?: extractNumericField(json, "heading_degrees")
            ?: extractNumericField(json, "heading")

        val speed = extractNumericField(json, "speedMps")
            ?: extractNumericField(json, "speed_mps")
            ?: extractNumericField(json, "speed")

        val distance = extractNumericField(json, "remainingDistanceMeters")
            ?: extractNumericField(json, "remaining_distance_meters")
            ?: extractNumericField(json, "remainingDistance")

        val eta = extractNumericField(json, "etaSeconds")
            ?: extractNumericField(json, "eta_seconds")
            ?: extractNumericField(json, "eta")

        val progress = extractNumericField(json, "routeProgressPercentage")
            ?: extractNumericField(json, "route_progress_percentage")
            ?: extractNumericField(json, "progress")

        return RealtimeEvent.LiveTelemetryUpdate(
            rideId = rideId,
            coordinate = coords,
            headingDegrees = heading?.toFloat(),
            speedMps = speed?.toFloat(),
            remainingDistanceMeters = distance?.toInt(),
            etaSeconds = eta?.toInt(),
            routeProgressPercentage = progress?.toFloat()
        )
    }

    private fun parseRideRequest(json: String): RealtimeEvent {
        val requestId = extractField(json, "requestId") ?: extractField(json, "request_id") ?: ""
        val status = extractField(json, "status") ?: "UPDATED"
        return RealtimeEvent.RideRequestUpdate(requestId, status)
    }

    private fun parseRideRequestCreated(json: String): RealtimeEvent {
        val requestId = extractField(json, "requestId") ?: extractField(json, "request_id") ?: ""
        val tripId = extractField(json, "tripId") ?: extractField(json, "trip_id") ?: ""
        val expiresAt = extractField(json, "expiresAt") ?: extractField(json, "expires_at")
        return RealtimeEvent.RideRequestCreated(requestId, tripId, expiresAt)
    }

    private fun parseRideRequestCancelled(json: String): RealtimeEvent {
        val requestId = extractField(json, "requestId") ?: extractField(json, "request_id") ?: ""
        val reason = extractField(json, "reason")
        return RealtimeEvent.RideRequestCancelled(requestId, reason)
    }

    private fun parseRideRequestExpired(json: String): RealtimeEvent {
        val requestId = extractField(json, "requestId") ?: extractField(json, "request_id") ?: ""
        return RealtimeEvent.RideRequestExpired(requestId)
    }

    private fun parseRideCreated(json: String): RealtimeEvent {
        val payload = extractJsonObject(json, "payload") ?: json
        val rideId = extractField(payload, "rideId") ?: extractField(payload, "ride_id") ?: ""
        val tripId = extractField(payload, "tripId") ?: extractField(payload, "trip_id") ?: ""
        return RealtimeEvent.RideCreated(rideId, tripId)
    }

    private fun parseRideDriverArriving(json: String): RealtimeEvent {
        val payload = extractJsonObject(json, "payload") ?: json
        val rideId = extractField(payload, "rideId") ?: extractField(payload, "ride_id") ?: ""
        val tripId = extractField(payload, "tripId") ?: extractField(payload, "trip_id")
        return RealtimeEvent.RideDriverArriving(rideId, tripId)
    }

    private fun parseRidePickedUp(json: String): RealtimeEvent {
        val payload = extractJsonObject(json, "payload") ?: json
        val rideId = extractField(payload, "rideId") ?: extractField(payload, "ride_id") ?: ""
        val tripId = extractField(payload, "tripId") ?: extractField(payload, "trip_id")
        return RealtimeEvent.RidePickedUp(rideId, tripId)
    }

    private fun parseRideStarted(json: String): RealtimeEvent {
        val payload = extractJsonObject(json, "payload") ?: json
        val rideId = extractField(payload, "rideId") ?: extractField(payload, "ride_id") ?: ""
        val tripId = extractField(payload, "tripId") ?: extractField(payload, "trip_id")
        return RealtimeEvent.RideStarted(rideId, tripId)
    }

    private fun parseRideCompleted(json: String): RealtimeEvent {
        val payload = extractJsonObject(json, "payload") ?: json
        val rideId = extractField(payload, "rideId") ?: extractField(payload, "ride_id") ?: ""
        val tripId = extractField(payload, "tripId") ?: extractField(payload, "trip_id")
        return RealtimeEvent.RideCompleted(rideId, tripId)
    }

    private fun parseRideCancelled(json: String): RealtimeEvent {
        val payload = extractJsonObject(json, "payload") ?: json
        val rideId = extractField(payload, "rideId") ?: extractField(payload, "ride_id") ?: ""
        val reason = extractField(payload, "reason")
        return RealtimeEvent.RideCancelled(rideId, reason)
    }

    private fun parseDiscovery(json: String): RealtimeEvent {
        val tripId = extractField(json, "tripId") ?: extractField(json, "trip_id") ?: ""
        val routeSummary = extractField(json, "routeSummary") ?: extractField(json, "route_summary") ?: ""
        return RealtimeEvent.NearbyTripDiscovered(tripId, routeSummary)
    }

    private fun parseVoice(json: String): RealtimeEvent {
        val text = extractField(json, "partialText") ?: extractField(json, "text") ?: ""
        val isFinal = json.contains("\"isFinal\"\\s*:\\s*true".toRegex()) ||
                json.contains("\"is_final\"\\s*:\\s*true".toRegex())
        return RealtimeEvent.VoiceTranscription(text, isFinal)
    }


    private fun parseRideTrackingSubscribed(json: String): RealtimeEvent {
        val payload = extractJsonObject(json, "payload") ?: json
        val rideId = extractField(payload, "rideId") ?: extractField(payload, "ride_id") ?: ""
        val driverId = extractField(payload, "driverId") ?: extractField(payload, "driver_id")
        val timestamp = extractField(payload, "timestamp")
        return RealtimeEvent.RideTrackingSubscribed(rideId, driverId, timestamp)
    }

    private fun parseTrackingSnapshot(json: String): RealtimeEvent {
        val payload = extractJsonObject(json, "payload") ?: json
        val rideId = extractField(payload, "rideId") ?: extractField(payload, "ride_id") ?: ""
        val status = extractField(payload, "status") ?: "UNKNOWN"
        val trackingState = extractField(payload, "trackingState") ?: extractField(payload, "tracking_state") ?: "UNAVAILABLE"
        val updatedAt = extractField(payload, "updatedAt") ?: extractField(payload, "updated_at") ?: ""

        val driver = extractDriverInfo(payload)
        val route = extractRouteInfo(payload)
        val distanceToPickup = extractNumericField(payload, "distanceToPickupMeters")
            ?: extractNumericField(payload, "distance_to_pickup_meters")
        val distanceToDest = extractNumericField(payload, "distanceToDestinationMeters")
            ?: extractNumericField(payload, "distance_to_destination_meters")
        val eta = extractEtaInfo(payload)

        val snapshot = TrackingResponseDto(
            rideId = rideId,
            status = status,
            trackingState = trackingState,
            driver = driver,
            route = route,
            distanceToPickupMeters = distanceToPickup,
            distanceToDestinationMeters = distanceToDest,
            eta = eta,
            updatedAt = updatedAt
        )
        return RealtimeEvent.TrackingSnapshot(snapshot)
    }

    private fun parseRideTrackingUpdated(json: String): RealtimeEvent {
        val payload = extractJsonObject(json, "payload") ?: json
        val rideId = extractField(payload, "rideId") ?: extractField(payload, "ride_id") ?: ""
        val driverLocation = extractCoordinateObject(payload, "driverLocation")
            ?: extractCoordinateObject(payload, "driver_location")
            ?: extractCoordinateObject(payload, "location")
        val freshness = extractField(payload, "freshness")
        val trackingState = extractField(payload, "trackingState") ?: extractField(payload, "tracking_state")
        val routeProgress = extractRouteProgressSummary(payload)
        val distanceToPickup = extractNumericField(payload, "distanceToPickupMeters")
            ?: extractNumericField(payload, "distance_to_pickup_meters")
        val distanceToDest = extractNumericField(payload, "distanceToDestinationMeters")
            ?: extractNumericField(payload, "distance_to_destination_meters")
        val eta = extractEtaInfo(payload)
        val recordedAt = extractField(payload, "recordedAt") ?: extractField(payload, "recorded_at")

        val update = RideTrackingUpdatedPayloadDto(
            rideId = rideId,
            driverLocation = driverLocation,
            freshness = freshness,
            trackingState = trackingState,
            routeProgress = routeProgress,
            distanceToPickupMeters = distanceToPickup,
            distanceToDestinationMeters = distanceToDest,
            eta = eta,
            recordedAt = recordedAt
        )
        return RealtimeEvent.RideTrackingUpdated(update)
    }

    private fun parseRideTrackingEnded(json: String): RealtimeEvent {
        val payload = extractJsonObject(json, "payload") ?: json
        val rideId = extractField(payload, "rideId") ?: extractField(payload, "ride_id") ?: ""
        val status = extractField(payload, "status") ?: "COMPLETED"
        val reason = extractField(payload, "reason")
        val timestamp = extractField(payload, "timestamp")
        return RealtimeEvent.RideTrackingEnded(rideId, status, reason, timestamp)
    }

    private fun parseRideTrackingError(json: String): RealtimeEvent {
        val payload = extractJsonObject(json, "payload") ?: json
        val rideId = extractField(payload, "rideId") ?: extractField(payload, "ride_id")
        val code = extractField(payload, "code")
        val message = extractField(payload, "message")
        return RealtimeEvent.RideTrackingError(rideId, code, message)
    }

    private fun parseDriverLocationUpdated(json: String): RealtimeEvent {
        val payload = extractJsonObject(json, "payload") ?: json
        val rideId = extractField(payload, "rideId") ?: extractField(payload, "ride_id") ?: ""
        val loc = extractCoordinateObject(payload, "location")
            ?: extractCoordinateObject(payload, "coordinates")
            ?: TrackingCoordinatesDto(0.0, 0.0)
        val recordedAt = extractField(payload, "recordedAt") ?: extractField(payload, "recorded_at")
        val isStale = extractBooleanField(payload, "isStale")
            ?: extractBooleanField(payload, "is_stale")
            ?: false
        return RealtimeEvent.DriverLocationUpdated(rideId, loc, recordedAt, isStale)
    }

    private fun extractDriverInfo(json: String): TrackingDriverInfoDto? {
        val driverJson = extractJsonObject(json, "driver") ?: return null
        val location = extractCoordinateObject(driverJson, "location")
            ?: extractCoordinateObject(driverJson, "coordinates")
        val accuracy = extractNumericField(driverJson, "accuracyMeters")
            ?: extractNumericField(driverJson, "accuracy_meters")
        val heading = extractNumericField(driverJson, "headingDegrees")
            ?: extractNumericField(driverJson, "heading_degrees")
        val speed = extractNumericField(driverJson, "speedMps")
            ?: extractNumericField(driverJson, "speed_mps")
        val recordedAt = extractField(driverJson, "recordedAt") ?: extractField(driverJson, "recorded_at")
        val receivedAt = extractField(driverJson, "receivedAt") ?: extractField(driverJson, "received_at")
        val freshness = extractField(driverJson, "freshness")

        return TrackingDriverInfoDto(
            location = location,
            accuracyMeters = accuracy,
            headingDegrees = heading,
            speedMps = speed,
            recordedAt = recordedAt,
            receivedAt = receivedAt,
            freshness = freshness
        )
    }

    private fun extractRouteInfo(json: String): TrackingRouteInfoDto? {
        val routeJson = extractJsonObject(json, "route") ?: return null
        val distance = extractNumericField(routeJson, "distanceMeters")
            ?: extractNumericField(routeJson, "distance_meters") ?: 0.0
        val completed = extractNumericField(routeJson, "completedDistanceMeters")
            ?: extractNumericField(routeJson, "completed_distance_meters") ?: 0.0
        val remaining = extractNumericField(routeJson, "remainingDistanceMeters")
            ?: extractNumericField(routeJson, "remaining_distance_meters") ?: 0.0
        val progress = extractNumericField(routeJson, "progressPercent")
            ?: extractNumericField(routeJson, "progress_percent") ?: 0.0
        val distFromRoute = extractNumericField(routeJson, "distanceFromRouteMeters")
            ?: extractNumericField(routeJson, "distance_from_route_meters") ?: 0.0
        val isOffRoute = extractBooleanField(routeJson, "isOffRoute")
            ?: extractBooleanField(routeJson, "is_off_route") ?: false

        return TrackingRouteInfoDto(
            distanceMeters = distance,
            completedDistanceMeters = completed,
            remainingDistanceMeters = remaining,
            progressPercent = progress,
            distanceFromRouteMeters = distFromRoute,
            isOffRoute = isOffRoute
        )
    }

    private fun extractRouteProgressSummary(json: String): TrackingRouteProgressSummaryDto? {
        val progressJson = extractJsonObject(json, "routeProgress")
            ?: extractJsonObject(json, "route_progress") ?: return null
        val completed = extractNumericField(progressJson, "completedDistanceMeters")
            ?: extractNumericField(progressJson, "completed_distance_meters") ?: 0.0
        val remaining = extractNumericField(progressJson, "remainingDistanceMeters")
            ?: extractNumericField(progressJson, "remaining_distance_meters") ?: 0.0
        val percent = extractNumericField(progressJson, "progressPercent")
            ?: extractNumericField(progressJson, "progress_percent") ?: 0.0

        return TrackingRouteProgressSummaryDto(
            completedDistanceMeters = completed,
            remainingDistanceMeters = remaining,
            progressPercent = percent
        )
    }

    private fun extractEtaInfo(json: String): TrackingEtaInfoDto? {
        val etaJson = extractJsonObject(json, "eta") ?: return null
        val available = extractBooleanField(etaJson, "available") ?: false
        val seconds = extractNumericField(etaJson, "seconds")?.toInt()
        val source = extractField(etaJson, "source")
        val confidence = extractField(etaJson, "confidence")

        return TrackingEtaInfoDto(
            available = available,
            seconds = seconds,
            source = source,
            confidence = confidence
        )
    }

    private fun extractCoordinateObject(json: String, key: String): TrackingCoordinatesDto? {
        val obj = extractJsonObject(json, key) ?: return null
        val lat = extractNumericField(obj, "latitude") ?: extractNumericField(obj, "lat")
        val lng = extractNumericField(obj, "longitude") ?: extractNumericField(obj, "lng") ?: extractNumericField(obj, "lon")
        if (lat != null && lng != null) {
            return TrackingCoordinatesDto(latitude = lat, longitude = lng)
        }
        return null
    }

    private fun extractBooleanField(json: String, key: String): Boolean? {
        val regexTrue = Regex("\"$key\"\\s*:\\s*true")
        val regexFalse = Regex("\"$key\"\\s*:\\s*false")
        return when {
            regexTrue.containsMatchIn(json) -> true
            regexFalse.containsMatchIn(json) -> false
            else -> null
        }
    }

    private fun extractJsonObject(json: String, key: String): String? {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return null
        val colonIndex = json.indexOf(':', keyIndex)
        if (colonIndex == -1) return null
        val braceIndex = json.indexOf('{', colonIndex)
        if (braceIndex == -1) return null
        val endIndex = findClosingChar(json, braceIndex, '{', '}')
        if (endIndex == -1) return null
        return json.substring(braceIndex, endIndex + 1)
    }

    private fun findClosingChar(json: String, startIdx: Int, openChar: Char, closeChar: Char): Int {
        var depth = 0
        var inString = false
        var escaped = false

        for (i in startIdx until json.length) {
            val c = json[i]
            if (escaped) {
                escaped = false
                continue
            }
            if (c == '\\') {
                escaped = true
                continue
            }
            if (c == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (c == openChar) depth++
                else if (c == closeChar) {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }

    private fun extractField(json: String, key: String): String? {
        val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return regex.find(json)?.groupValues?.get(1)
    }

    private fun extractNumericField(json: String, key: String): Double? {
        val regex = Regex("\"$key\"\\s*:\\s*(-?[0-9]+(?:\\.[0-9]+)?)")
        return regex.find(json)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractCoordinateArray(json: String, key: String): GeoJsonCoordinate? {
        val regex = Regex("\"$key\"\\s*:\\s*\\[\\s*(-?[0-9]+(?:\\.[0-9]+)?)\\s*,\\s*(-?[0-9]+(?:\\.[0-9]+)?)\\s*\\]")
        val match = regex.find(json) ?: return null
        val lng = match.groupValues[1].toDoubleOrNull() ?: return null
        val lat = match.groupValues[2].toDoubleOrNull() ?: return null

        return try {
            GeoJsonCoordinate(longitude = lng, latitude = lat)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun parseSosCreated(json: String): RealtimeEvent {
        val dataJson = extractJsonObject(json, "data") ?: json
        val eventId = extractField(dataJson, "eventId") ?: extractField(json, "eventId") ?: ""
        val rideId = extractField(dataJson, "rideId") ?: extractField(json, "rideId") ?: ""
        val emergencyType = extractField(dataJson, "emergencyType") ?: extractField(json, "emergencyType") ?: "SOS"
        val status = extractField(dataJson, "status") ?: extractField(json, "status") ?: "ACTIVE"
        val triggeredByUserId = extractField(dataJson, "triggeredByUserId") ?: extractField(json, "triggeredByUserId") ?: ""
        val triggeredByRole = extractField(dataJson, "triggeredByRole") ?: extractField(json, "triggeredByRole") ?: "USER"
        val triggeredAt = extractField(dataJson, "triggeredAt") ?: extractField(json, "triggeredAt") ?: ""

        val locObj = extractJsonObject(dataJson, "locationSnapshot") ?: extractJsonObject(json, "locationSnapshot")
        val locSnapshot = if (locObj != null) {
            val coords = extractCoordinateArray(locObj, "coordinates")
            val coordList = if (coords != null) listOf(coords.longitude, coords.latitude) else null
            val accuracy = extractNumericField(locObj, "accuracyMeters")
            val heading = extractNumericField(locObj, "headingDegrees")
            val speed = extractNumericField(locObj, "speedMps")
            val isStale = locObj.contains("\"isStale\"\\s*:\\s*true".toRegex())
            val capturedAt = extractField(locObj, "capturedAt")
            val provider = extractField(locObj, "provider") ?: "driver_profile"
            com.ishara.app.data.remote.dto.SafetyLocationSnapshotDto(
                coordinates = coordList,
                accuracyMeters = accuracy,
                headingDegrees = heading,
                speedMps = speed,
                isStale = isStale,
                capturedAt = capturedAt,
                provider = provider
            )
        } else null

        return RealtimeEvent.SosCreated(
            eventId = eventId,
            rideId = rideId,
            emergencyType = emergencyType,
            status = status,
            triggeredByUserId = triggeredByUserId,
            triggeredByRole = triggeredByRole,
            locationSnapshot = locSnapshot,
            triggeredAt = triggeredAt
        )
    }

    private fun parseSosCancelled(json: String): RealtimeEvent {
        val dataJson = extractJsonObject(json, "data") ?: json
        val eventId = extractField(dataJson, "eventId") ?: extractField(json, "eventId") ?: ""
        val rideId = extractField(dataJson, "rideId") ?: extractField(json, "rideId") ?: ""
        val emergencyType = extractField(dataJson, "emergencyType") ?: extractField(json, "emergencyType") ?: "SOS"
        val status = extractField(dataJson, "status") ?: extractField(json, "status") ?: "CANCELLED"
        val cancelledAt = extractField(dataJson, "cancelledAt") ?: extractField(json, "cancelledAt")
        val cancellationReason = extractField(dataJson, "cancellationReason") ?: extractField(json, "cancellationReason")

        return RealtimeEvent.SosCancelled(
            eventId = eventId,
            rideId = rideId,
            emergencyType = emergencyType,
            status = status,
            cancelledAt = cancelledAt,
            cancellationReason = cancellationReason
        )
    }
}

