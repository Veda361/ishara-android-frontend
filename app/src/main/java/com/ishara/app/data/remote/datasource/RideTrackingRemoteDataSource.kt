package com.ishara.app.data.remote.datasource

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.RideDriverLocationResponseDto
import com.ishara.app.data.remote.dto.TrackingCoordinatesDto
import com.ishara.app.data.remote.dto.TrackingDriverInfoDto
import com.ishara.app.data.remote.dto.TrackingEtaInfoDto
import com.ishara.app.data.remote.dto.TrackingResponseDto
import com.ishara.app.data.remote.dto.TrackingRouteInfoDto

/**
 * Remote data source abstraction for Live Passenger Ride Tracking endpoints.
 * Strictly implements verified endpoints:
 * - GET /api/v1/rides/:rideId/tracking
 * - GET /api/v1/rides/:rideId/driver-location
 */
interface RideTrackingRemoteDataSource {
    suspend fun getRideTracking(
        rideId: String,
        token: String
    ): IshaaraResult<TrackingResponseDto>

    suspend fun getDriverLocation(
        rideId: String,
        token: String
    ): IshaaraResult<RideDriverLocationResponseDto>
}

class DefaultRideTrackingRemoteDataSource(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : RideTrackingRemoteDataSource {

    private val tag = "RideTrackingRemoteDataSource"

    override suspend fun getRideTracking(
        rideId: String,
        token: String
    ): IshaaraResult<TrackingResponseDto> {
        val url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/tracking"
        val request = HttpRequest(
            url = url,
            method = HttpMethod.GET,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Accept" to "application/json"
            )
        )

        return when (val result = httpClient.execute(request)) {
            is IshaaraResult.Success -> {
                try {
                    val body = result.data.body
                    val dataJson = extractJsonObject(body, "data") ?: body
                    val dto = parseTrackingResponseDto(dataJson)
                    IshaaraResult.success(dto)
                } catch (e: Exception) {
                    IshaaraLogger.e(tag, "Failed to parse tracking response", e)
                    IshaaraResult.failure(IshaaraError.Unknown("Failed to parse tracking snapshot: ${e.message}"))
                }
            }
            is IshaaraResult.Failure -> IshaaraResult.failure(result.error)
        }
    }

    override suspend fun getDriverLocation(
        rideId: String,
        token: String
    ): IshaaraResult<RideDriverLocationResponseDto> {
        val url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/driver-location"
        val request = HttpRequest(
            url = url,
            method = HttpMethod.GET,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Accept" to "application/json"
            )
        )

        return when (val result = httpClient.execute(request)) {
            is IshaaraResult.Success -> {
                try {
                    val body = result.data.body
                    val dataJson = extractJsonObject(body, "data") ?: body
                    val dto = parseDriverLocationResponseDto(dataJson)
                    IshaaraResult.success(dto)
                } catch (e: Exception) {
                    IshaaraLogger.e(tag, "Failed to parse driver location response", e)
                    IshaaraResult.failure(IshaaraError.Unknown("Failed to parse driver location: ${e.message}"))
                }
            }
            is IshaaraResult.Failure -> IshaaraResult.failure(result.error)
        }
    }

    private fun parseTrackingResponseDto(json: String): TrackingResponseDto {
        val rideId = extractField(json, "rideId") ?: extractField(json, "ride_id") ?: ""
        val status = extractField(json, "status") ?: "UNKNOWN"
        val trackingState = extractField(json, "trackingState") ?: extractField(json, "tracking_state") ?: "UNAVAILABLE"
        val updatedAt = extractField(json, "updatedAt") ?: extractField(json, "updated_at") ?: ""

        val driver = extractDriverInfo(json)
        val route = extractRouteInfo(json)
        val distanceToPickup = extractNumericField(json, "distanceToPickupMeters")
            ?: extractNumericField(json, "distance_to_pickup_meters")
        val distanceToDest = extractNumericField(json, "distanceToDestinationMeters")
            ?: extractNumericField(json, "distance_to_destination_meters")
        val eta = extractEtaInfo(json)

        return TrackingResponseDto(
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
    }

    private fun parseDriverLocationResponseDto(json: String): RideDriverLocationResponseDto {
        val rideId = extractField(json, "rideId") ?: extractField(json, "ride_id") ?: ""
        val driverId = extractField(json, "driverId") ?: extractField(json, "driver_id") ?: ""
        val location = extractCoordinateObject(json, "location")
            ?: extractCoordinateObject(json, "coordinates")
        val accuracy = extractNumericField(json, "accuracyMeters")
            ?: extractNumericField(json, "accuracy_meters")
        val heading = extractNumericField(json, "headingDegrees")
            ?: extractNumericField(json, "heading_degrees")
        val speed = extractNumericField(json, "speedMps")
            ?: extractNumericField(json, "speed_mps")
        val altitude = extractNumericField(json, "altitudeMeters")
            ?: extractNumericField(json, "altitude_meters")
        val recordedAt = extractField(json, "recordedAt") ?: extractField(json, "recorded_at")
        val receivedAt = extractField(json, "receivedAt") ?: extractField(json, "received_at")
        val isStale = extractBooleanField(json, "isStale")
            ?: extractBooleanField(json, "is_stale") ?: false
        val status = extractField(json, "status") ?: "UNAVAILABLE"

        return RideDriverLocationResponseDto(
            rideId = rideId,
            driverId = driverId,
            location = location,
            accuracyMeters = accuracy,
            headingDegrees = heading,
            speedMps = speed,
            altitudeMeters = altitude,
            recordedAt = recordedAt,
            receivedAt = receivedAt,
            isStale = isStale,
            status = status
        )
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
}
