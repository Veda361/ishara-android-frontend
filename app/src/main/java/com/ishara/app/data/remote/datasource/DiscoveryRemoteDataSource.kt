package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.DiscoveryCoordinateDto
import com.ishara.app.data.remote.dto.DiscoveryDriverDto
import com.ishara.app.data.remote.dto.DiscoveryEndpointDto
import com.ishara.app.data.remote.dto.DiscoveryGeoJsonPointDto
import com.ishara.app.data.remote.dto.DiscoveryItemDto
import com.ishara.app.data.remote.dto.DiscoveryMatchDto
import com.ishara.app.data.remote.dto.DiscoveryOptionsDto
import com.ishara.app.data.remote.dto.DiscoveryPaginationDto
import com.ishara.app.data.remote.dto.DiscoveryResponseDto
import com.ishara.app.data.remote.dto.DiscoveryRouteSummaryDto
import com.ishara.app.data.remote.dto.DiscoverySearchRequestDto
import com.ishara.app.data.remote.dto.DiscoveryEstimatedFareDto
import com.ishara.app.data.remote.dto.DiscoveryVehicleDto
import com.ishara.app.data.remote.dto.PublicTripDriverDto
import com.ishara.app.data.remote.dto.PublicTripLocationDto
import com.ishara.app.data.remote.dto.PublicTripResponseDto
import com.ishara.app.data.remote.dto.PublicTripRouteDto
import com.ishara.app.data.remote.dto.PublicTripRouteGeometryDto
import com.ishara.app.data.remote.dto.PublicTripVehicleDto

interface DiscoveryRemoteDataSource {
    suspend fun discoverTrips(
        request: DiscoverySearchRequestDto,
        token: String?
    ): IshaaraResult<DiscoveryResponseDto>

    suspend fun getPassengerTripDetails(
        tripId: String,
        token: String?
    ): IshaaraResult<PublicTripResponseDto> = IshaaraResult.failure(IshaaraError.NotFound())
}

class DiscoveryRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : DiscoveryRemoteDataSource {

    override suspend fun discoverTrips(
        request: DiscoverySearchRequestDto,
        token: String?
    ): IshaaraResult<DiscoveryResponseDto> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val body = serializeRequest(request)

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/discovery/trips",
            method = HttpMethod.POST,
            headers = headers,
            body = body
        )

        return httpClient.execute(httpRequest).map { response ->
            parseDiscoveryResponse(response.body)
        }
    }

    override suspend fun getPassengerTripDetails(
        tripId: String,
        token: String?
    ): IshaaraResult<PublicTripResponseDto> {
        val headers = mutableMapOf<String, String>()
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/$tripId",
            method = HttpMethod.GET,
            headers = headers
        )

        return httpClient.execute(httpRequest).map { response ->
            val dataJson = extractJsonObject(response.body, "data") ?: response.body
            parsePublicTripResponse(dataJson)
        }
    }

    /**
     * Serializes request DTO strictly to JSON without any extra unrecognized fields.
     */
    internal fun serializeRequest(request: DiscoverySearchRequestDto): String {
        val sb = StringBuilder()
        sb.append("{")

        // Origin
        sb.append("\"origin\":{")
        sb.append("\"latitude\":").append(request.origin.latitude).append(",")
        sb.append("\"longitude\":").append(request.origin.longitude)
        if (!request.origin.name.isNullOrBlank()) {
            sb.append(",\"name\":\"").append(escapeJson(request.origin.name)).append("\"")
        }
        if (!request.origin.formattedAddress.isNullOrBlank()) {
            sb.append(",\"formattedAddress\":\"").append(escapeJson(request.origin.formattedAddress)).append("\"")
        }
        sb.append("},")

        // Destination
        sb.append("\"destination\":{")
        sb.append("\"latitude\":").append(request.destination.latitude).append(",")
        sb.append("\"longitude\":").append(request.destination.longitude)
        if (!request.destination.name.isNullOrBlank()) {
            sb.append(",\"name\":\"").append(escapeJson(request.destination.name)).append("\"")
        }
        if (!request.destination.formattedAddress.isNullOrBlank()) {
            sb.append(",\"formattedAddress\":\"").append(escapeJson(request.destination.formattedAddress)).append("\"")
        }
        sb.append("}")

        // Options
        if (request.options != null) {
            val opt = request.options
            val optParts = mutableListOf<String>()
            if (opt.maxPickupDistanceMeters != null) {
                optParts.add("\"maxPickupDistanceMeters\":${opt.maxPickupDistanceMeters}")
            }
            if (opt.maxDestinationDeviationMeters != null) {
                optParts.add("\"maxDestinationDeviationMeters\":${opt.maxDestinationDeviationMeters}")
            }
            if (opt.maxResults != null) {
                optParts.add("\"maxResults\":${opt.maxResults}")
            }
            if (!opt.cursor.isNullOrBlank()) {
                optParts.add("\"cursor\":\"${escapeJson(opt.cursor)}\"")
            }

            if (optParts.isNotEmpty()) {
                sb.append(",\"options\":{")
                sb.append(optParts.joinToString(","))
                sb.append("}")
            }
        }

        sb.append("}")
        return sb.toString()
    }

    /**
     * Pure Kotlin JSON parser for DiscoveryResponseDto.
     */
    internal fun parseDiscoveryResponse(json: String): DiscoveryResponseDto {
        if (json.isBlank()) {
            return DiscoveryResponseDto("", emptyList(), DiscoveryPaginationDto())
        }

        val sessionId = extractStringValue(json, "discoverySessionId") ?: ""

        // Extract items array
        val items = mutableListOf<DiscoveryItemDto>()
        val itemsIndex = json.indexOf("\"items\"")
        if (itemsIndex != -1) {
            val arrayStart = json.indexOf('[', itemsIndex)
            if (arrayStart != -1) {
                val arrayEnd = findClosingBracket(json, arrayStart)
                if (arrayEnd > arrayStart) {
                    val arrayContent = json.substring(arrayStart + 1, arrayEnd).trim()
                    val rawObjects = splitJsonObjects(arrayContent)
                    for (rawObj in rawObjects) {
                        val item = parseDiscoveryItem(rawObj)
                        if (item != null) {
                            items.add(item)
                        }
                    }
                }
            }
        }

        // Extract pagination
        val paginationObj = extractJsonObject(json, "pagination")
        val pagination = if (paginationObj != null) {
            DiscoveryPaginationDto(
                limit = extractIntValue(paginationObj, "limit") ?: 20,
                hasMore = extractBooleanValue(paginationObj, "hasMore") ?: false,
                nextCursor = extractStringValue(paginationObj, "nextCursor")
            )
        } else {
            DiscoveryPaginationDto(limit = 20, hasMore = false, nextCursor = null)
        }

        return DiscoveryResponseDto(
            discoverySessionId = sessionId,
            items = items,
            pagination = pagination
        )
    }

    private fun parseDiscoveryItem(json: String): DiscoveryItemDto? {
        val tripId = extractStringValue(json, "tripId") ?: return null

        // Driver
        val driverObj = extractJsonObject(json, "driver") ?: ""
        val driverId = extractStringValue(driverObj, "id") ?: ""
        val driverName = extractStringValue(driverObj, "name") ?: "Verified Driver"
        val driverImage = extractStringValue(driverObj, "image")
        val driver = DiscoveryDriverDto(id = driverId, name = driverName, image = driverImage)

        // Vehicle
        val vehicleObj = extractJsonObject(json, "vehicle") ?: ""
        val vehicleId = extractStringValue(vehicleObj, "id") ?: ""
        val regNumber = extractStringValue(vehicleObj, "registrationNumber") ?: ""
        val vehicleType = extractStringValue(vehicleObj, "vehicleType") ?: "BUS"
        val make = extractStringValue(vehicleObj, "make")
        val model = extractStringValue(vehicleObj, "model")
        val vehicle = DiscoveryVehicleDto(
            id = vehicleId,
            registrationNumber = regNumber,
            vehicleType = vehicleType,
            make = make,
            model = model
        )

        // Origin
        val originObj = extractJsonObject(json, "origin") ?: ""
        val originName = extractStringValue(originObj, "name")
        val originAddr = extractStringValue(originObj, "formattedAddress") ?: ""
        val originCoords = extractCoordinates(originObj)
        val origin = DiscoveryEndpointDto(
            name = originName,
            formattedAddress = originAddr,
            coordinates = DiscoveryGeoJsonPointDto(coordinates = originCoords)
        )

        // Destination
        val destObj = extractJsonObject(json, "destination") ?: ""
        val destName = extractStringValue(destObj, "name")
        val destAddr = extractStringValue(destObj, "formattedAddress") ?: ""
        val destCoords = extractCoordinates(destObj)
        val destination = DiscoveryEndpointDto(
            name = destName,
            formattedAddress = destAddr,
            coordinates = DiscoveryGeoJsonPointDto(coordinates = destCoords)
        )

        // Route Summary
        val routeObj = extractJsonObject(json, "routeSummary")
        val routeSummary = if (routeObj != null) {
            DiscoveryRouteSummaryDto(
                distanceMeters = extractDoubleValue(routeObj, "distanceMeters"),
                durationSeconds = extractDoubleValue(routeObj, "durationSeconds")
            )
        } else {
            DiscoveryRouteSummaryDto()
        }

        // Match
        val matchObj = extractJsonObject(json, "match")
        val match = if (matchObj != null) {
            DiscoveryMatchDto(
                pickupDistanceMeters = extractDoubleValue(matchObj, "pickupDistanceMeters") ?: 0.0,
                destinationDistanceMeters = extractDoubleValue(matchObj, "destinationDistanceMeters") ?: 0.0,
                directionDifferenceDegrees = extractDoubleValue(matchObj, "directionDifferenceDegrees") ?: 0.0,
                pickupRouteProgress = extractDoubleValue(matchObj, "pickupRouteProgress") ?: 0.0,
                destinationRouteProgress = extractDoubleValue(matchObj, "destinationRouteProgress") ?: 0.0,
                estimatedDetourMeters = extractDoubleValue(matchObj, "estimatedDetourMeters") ?: 0.0,
                compatibility = extractStringValue(matchObj, "compatibility") ?: "UNKNOWN",
                score = extractDoubleValue(matchObj, "score") ?: 0.0
            )
        } else {
            DiscoveryMatchDto()
        }

        // Estimated Fare (authoritative backend calculation if returned)
        val fareObj = extractJsonObject(json, "estimatedFare")
        val estimatedFare = if (fareObj != null) {
            val amountMinor = extractDoubleValue(fareObj, "amountMinor")?.toLong() ?: 0L
            val currency = extractStringValue(fareObj, "currency") ?: "INR"
            val formatted = extractStringValue(fareObj, "formatted") ?: ""
            DiscoveryEstimatedFareDto(amountMinor = amountMinor, currency = currency, formatted = formatted)
        } else {
            null
        }

        return DiscoveryItemDto(
            tripId = tripId,
            driver = driver,
            vehicle = vehicle,
            origin = origin,
            destination = destination,
            routeSummary = routeSummary,
            match = match,
            estimatedFare = estimatedFare
        )
    }

    internal fun parsePublicTripResponse(json: String): PublicTripResponseDto {
        val tripId = extractStringValue(json, "id") ?: extractStringValue(json, "_id") ?: ""
        val status = extractStringValue(json, "status") ?: "CREATED"

        // Origin
        val originObj = extractJsonObject(json, "origin") ?: ""
        val origin = parsePublicTripLocation(originObj)

        // Destination
        val destObj = extractJsonObject(json, "destination") ?: ""
        val destination = parsePublicTripLocation(destObj)

        // Route
        val routeObj = extractJsonObject(json, "route")
        val route = routeObj?.let { parsePublicTripRoute(it) }

        val startedAt = extractStringValue(json, "startedAt")
        val scheduledDepartureAt = extractStringValue(json, "scheduledDepartureAt")
        val createdAt = extractStringValue(json, "createdAt") ?: ""

        // Driver
        val driverObj = extractJsonObject(json, "driver") ?: ""
        val driver = PublicTripDriverDto(
            id = extractStringValue(driverObj, "id") ?: "",
            name = extractStringValue(driverObj, "name"),
            image = extractStringValue(driverObj, "image")
        )

        // Vehicle
        val vehicleObj = extractJsonObject(json, "vehicle") ?: ""
        val vehicle = PublicTripVehicleDto(
            id = extractStringValue(vehicleObj, "id") ?: "",
            registrationNumber = extractStringValue(vehicleObj, "registrationNumber") ?: "",
            vehicleType = extractStringValue(vehicleObj, "vehicleType") ?: "BUS",
            make = extractStringValue(vehicleObj, "make") ?: "",
            model = extractStringValue(vehicleObj, "model") ?: ""
        )

        return PublicTripResponseDto(
            id = tripId,
            status = status,
            origin = origin,
            destination = destination,
            route = route,
            startedAt = startedAt,
            scheduledDepartureAt = scheduledDepartureAt,
            createdAt = createdAt,
            driver = driver,
            vehicle = vehicle
        )
    }

    private fun parsePublicTripLocation(json: String): PublicTripLocationDto {
        val name = extractStringValue(json, "name")
        val formattedAddress = extractStringValue(json, "formattedAddress") ?: ""
        val googlePlaceId = extractStringValue(json, "googlePlaceId")
        val serpApiDataId = extractStringValue(json, "serpApiDataId")
        val coordinates = extractCoordinates(json)

        return PublicTripLocationDto(
            name = name,
            formattedAddress = formattedAddress,
            coordinates = DiscoveryGeoJsonPointDto(coordinates = coordinates),
            googlePlaceId = googlePlaceId,
            serpApiDataId = serpApiDataId
        )
    }

    private fun parsePublicTripRoute(json: String): PublicTripRouteDto {
        val distanceMeters = extractDoubleValue(json, "distanceMeters")
        val durationSeconds = extractDoubleValue(json, "durationSeconds")
        val provider = extractStringValue(json, "provider")

        val geometryObj = extractJsonObject(json, "geometry")
        val geometry = geometryObj?.let { geom ->
            val type = extractStringValue(geom, "type") ?: "LineString"
            val coordsList = mutableListOf<DoubleArray>()
            val coordsIndex = geom.indexOf("\"coordinates\"")
            if (coordsIndex != -1) {
                val arrayStart = geom.indexOf('[', coordsIndex)
                if (arrayStart != -1) {
                    val arrayEnd = findClosingBracket(geom, arrayStart)
                    if (arrayEnd > arrayStart) {
                        val subContent = geom.substring(arrayStart + 1, arrayEnd).trim()
                        val subArrays = splitJsonSubArrays(subContent)
                        for (sub in subArrays) {
                            val nums = sub.trim().removePrefix("[").removeSuffix("]")
                                .split(",").mapNotNull { it.trim().toDoubleOrNull() }
                            coordsList.add(nums.toDoubleArray())
                        }
                    }
                }
            }
            PublicTripRouteGeometryDto(type = type, coordinates = coordsList)
        }

        return PublicTripRouteDto(
            geometry = geometry,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            provider = provider
        )
    }

    private fun splitJsonSubArrays(content: String): List<String> {
        val list = mutableListOf<String>()
        var depth = 0
        var start = -1
        for (i in content.indices) {
            val c = content[i]
            if (c == '[') {
                if (depth == 0) start = i
                depth++
            } else if (c == ']') {
                depth--
                if (depth == 0 && start != -1) {
                    list.add(content.substring(start, i + 1))
                    start = -1
                }
            }
        }
        return list
    }


    private fun extractCoordinates(json: String): DoubleArray {
        val coordsIndex = json.indexOf("\"coordinates\"")
        if (coordsIndex == -1) return DoubleArray(0)

        val arrayStart = json.indexOf('[', coordsIndex)
        if (arrayStart == -1) return DoubleArray(0)

        // Could be { "coordinates": { "coordinates": [lng, lat] } }
        val nextBracket = json.indexOf('[', arrayStart + 1)
        val actualStart = if (nextBracket != -1 && nextBracket < json.indexOf(']', arrayStart)) {
            nextBracket
        } else {
            arrayStart
        }

        val arrayEnd = json.indexOf(']', actualStart)
        if (arrayEnd == -1) return DoubleArray(0)

        val content = json.substring(actualStart + 1, arrayEnd).trim()
        val parts = content.split(",").mapNotNull { it.trim().toDoubleOrNull() }
        return parts.toDoubleArray()
    }

    private fun splitJsonObjects(content: String): List<String> {
        val list = mutableListOf<String>()
        var depth = 0
        var start = -1
        var inString = false
        var escaped = false

        for (i in content.indices) {
            val c = content[i]
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
                if (c == '{') {
                    if (depth == 0) start = i
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0 && start != -1) {
                        list.add(content.substring(start, i + 1))
                        start = -1
                    }
                }
            }
        }
        return list
    }

    private fun findClosingBracket(json: String, startBracketIndex: Int): Int {
        var depth = 0
        var inString = false
        var escaped = false
        val openChar = json[startBracketIndex]
        val closeChar = if (openChar == '[') ']' else '}'

        for (i in startBracketIndex until json.length) {
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

    private fun extractJsonObject(json: String, key: String): String? {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return null

        val colonIndex = json.indexOf(':', keyIndex)
        if (colonIndex == -1) return null

        val braceIndex = json.indexOf('{', colonIndex)
        if (braceIndex == -1) return null

        val endIndex = findClosingBracket(json, braceIndex)
        if (endIndex == -1) return null

        return json.substring(braceIndex, endIndex + 1)
    }

    private fun extractStringValue(json: String, key: String): String? {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return null

        val colonIndex = json.indexOf(':', keyIndex)
        if (colonIndex == -1) return null

        val remaining = json.substring(colonIndex + 1).trimStart()
        if (remaining.startsWith("null")) return null

        val quoteStart = remaining.indexOf('"')
        if (quoteStart == -1) return null

        val sb = StringBuilder()
        var escaped = false
        for (i in (quoteStart + 1) until remaining.length) {
            val c = remaining[i]
            if (escaped) {
                sb.append(c)
                escaped = false
            } else if (c == '\\') {
                escaped = true
            } else if (c == '"') {
                break
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun extractDoubleValue(json: String, key: String): Double? {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return null

        val colonIndex = json.indexOf(':', keyIndex)
        if (colonIndex == -1) return null

        val remaining = json.substring(colonIndex + 1).trimStart()
        if (remaining.startsWith("null")) return null

        val sb = StringBuilder()
        for (c in remaining) {
            if (c.isDigit() || c == '.' || c == '-') {
                sb.append(c)
            } else if (sb.isNotEmpty()) {
                break
            }
        }
        return sb.toString().toDoubleOrNull()
    }

    private fun extractIntValue(json: String, key: String): Int? {
        return extractDoubleValue(json, key)?.toInt()
    }

    private fun extractBooleanValue(json: String, key: String): Boolean? {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return null

        val colonIndex = json.indexOf(':', keyIndex)
        if (colonIndex == -1) return null

        val remaining = json.substring(colonIndex + 1).trimStart()
        return when {
            remaining.startsWith("true") -> true
            remaining.startsWith("false") -> false
            else -> null
        }
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
