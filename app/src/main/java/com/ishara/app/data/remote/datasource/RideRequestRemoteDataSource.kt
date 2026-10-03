package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.CreateRideRequestDto
import com.ishara.app.data.remote.dto.DiscoveryGeoJsonPointDto
import com.ishara.app.data.remote.dto.ListRideRequestsResponseDto
import com.ishara.app.data.remote.dto.LocationWaypointDto
import com.ishara.app.data.remote.dto.LocationWaypointResponseDto
import com.ishara.app.data.remote.dto.RideRequestResponseDto

interface RideRequestRemoteDataSource {
    suspend fun submitRideRequest(
        request: CreateRideRequestDto,
        token: String?,
        idempotencyKey: String? = null
    ): IshaaraResult<RideRequestResponseDto>

    suspend fun getRideRequest(
        requestId: String,
        token: String?
    ): IshaaraResult<RideRequestResponseDto> = IshaaraResult.failure(IshaaraError.NotFound())

    suspend fun cancelRideRequest(
        requestId: String,
        reason: String?,
        token: String?
    ): IshaaraResult<RideRequestResponseDto> = IshaaraResult.failure(IshaaraError.NotFound())

    suspend fun listUserRequests(
        status: String? = null,
        tripId: String? = null,
        limit: Int? = null,
        page: Int? = null,
        token: String? = null
    ): IshaaraResult<ListRideRequestsResponseDto> = IshaaraResult.success(ListRideRequestsResponseDto(emptyList()))
}

class RideRequestRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : RideRequestRemoteDataSource {

    override suspend fun submitRideRequest(
        request: CreateRideRequestDto,
        token: String?,
        idempotencyKey: String?
    ): IshaaraResult<RideRequestResponseDto> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }
        if (!idempotencyKey.isNullOrBlank()) {
            headers["Idempotency-Key"] = idempotencyKey
        }

        val body = serializeRequest(request)

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/ride-requests",
            method = HttpMethod.POST,
            headers = headers,
            body = body
        )

        val responseResult = httpClient.execute(httpRequest)
        return when (responseResult) {
            is IshaaraResult.Success -> {
                val response = responseResult.data
                try {
                    val parsed = parseRideRequestResponse(response.body)
                    IshaaraResult.success(parsed)
                } catch (e: Exception) {
                    IshaaraResult.failure(IshaaraError.Unknown(cause = e))
                }
            }
            is IshaaraResult.Failure -> {
                val error = responseResult.error
                // Map specific backend errors
                val mappedError = when {
                    error is IshaaraError.Conflict || error.code == 409 -> {
                        IshaaraError.Conflict(
                            message = "You already have an active pending ride request for this trip."
                        )
                    }
                    error.code == 400 && error.message.contains("TRIP_NOT_ELIGIBLE", ignoreCase = true) -> {
                        IshaaraError.Validation(
                            field = "tripId",
                            message = "This trip is no longer active. Please choose another available trip."
                        )
                    }
                    error.code == 400 && error.message.contains("SAME_ORIGIN_DESTINATION", ignoreCase = true) -> {
                        IshaaraError.Validation(
                            field = "destination",
                            message = "Pickup and destination cannot be the same physical location (minimum 50m separation required)."
                        )
                    }
                    error.code == 403 -> {
                        IshaaraError.Forbidden(
                            message = "Ride requests can only be submitted by passenger accounts."
                        )
                    }
                    else -> error
                }
                IshaaraResult.failure(mappedError)
            }
        }
    }

    override suspend fun getRideRequest(
        requestId: String,
        token: String?
    ): IshaaraResult<RideRequestResponseDto> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/ride-requests/$requestId",
            method = HttpMethod.GET,
            headers = headers
        )

        return when (val responseResult = httpClient.execute(httpRequest)) {
            is IshaaraResult.Success -> {
                val response = responseResult.data
                try {
                    val parsed = parseRideRequestResponse(response.body)
                    IshaaraResult.success(parsed)
                } catch (e: Exception) {
                    IshaaraResult.failure(IshaaraError.Unknown(cause = e))
                }
            }
            is IshaaraResult.Failure -> {
                val error = responseResult.error
                val mappedError = when {
                    error.code == 401 -> IshaaraError.Authentication(401, "Sign in required.")
                    error.code == 403 -> IshaaraError.Forbidden("You do not have permission to view this ride request.")
                    error.code == 404 -> IshaaraError.NotFound("Ride request not found.")
                    else -> error
                }
                IshaaraResult.failure(mappedError)
            }
        }
    }

    override suspend fun cancelRideRequest(
        requestId: String,
        reason: String?,
        token: String?
    ): IshaaraResult<RideRequestResponseDto> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val body = if (!reason.isNullOrBlank()) {
            "{\"reason\":\"${escapeJson(reason.trim())}\"}"
        } else {
            "{}"
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/ride-requests/$requestId/cancel",
            method = HttpMethod.POST,
            headers = headers,
            body = body
        )

        return when (val responseResult = httpClient.execute(httpRequest)) {
            is IshaaraResult.Success -> {
                val response = responseResult.data
                try {
                    val parsed = parseRideRequestResponse(response.body)
                    IshaaraResult.success(parsed)
                } catch (e: Exception) {
                    IshaaraResult.failure(IshaaraError.Unknown(cause = e))
                }
            }
            is IshaaraResult.Failure -> {
                val error = responseResult.error
                val mappedError = when {
                    error.code == 401 -> IshaaraError.Authentication(401, "Sign in required.")
                    error.code == 403 -> IshaaraError.Forbidden("You do not have permission to cancel this ride request.")
                    error.code == 404 -> IshaaraError.NotFound("Ride request not found.")
                    error is IshaaraError.Conflict || error.code == 409 -> {
                        when {
                            error.message.contains("accepted", ignoreCase = true) ->
                                IshaaraError.Conflict("Ride request has already been accepted by the driver.")
                            error.message.contains("expired", ignoreCase = true) ->
                                IshaaraError.Conflict("Ride request has expired and cannot be cancelled.")
                            else ->
                                IshaaraError.Conflict("Ride request is no longer pending and cannot be cancelled.")
                        }
                    }
                    else -> error
                }
                IshaaraResult.failure(mappedError)
            }
        }
    }

    override suspend fun listUserRequests(
        status: String?,
        tripId: String?,
        limit: Int?,
        page: Int?,
        token: String?
    ): IshaaraResult<ListRideRequestsResponseDto> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val queryParams = mutableMapOf<String, String>()
        if (!status.isNullOrBlank()) queryParams["status"] = status
        if (!tripId.isNullOrBlank()) queryParams["tripId"] = tripId
        if (limit != null) queryParams["limit"] = limit.toString()
        if (page != null) queryParams["page"] = page.toString()

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/ride-requests/me",
            method = HttpMethod.GET,
            headers = headers,
            queryParams = queryParams
        )

        return when (val responseResult = httpClient.execute(httpRequest)) {
            is IshaaraResult.Success -> {
                val response = responseResult.data
                try {
                    val parsed = parseListRideRequestsResponse(response.body)
                    IshaaraResult.success(parsed)
                } catch (e: Exception) {
                    IshaaraResult.failure(IshaaraError.Unknown(cause = e))
                }
            }
            is IshaaraResult.Failure -> {
                val error = responseResult.error
                val mappedError = when {
                    error.code == 401 -> IshaaraError.Authentication(401, "Sign in required.")
                    error.code == 403 -> IshaaraError.Forbidden("Only passenger accounts can list ride requests.")
                    else -> error
                }
                IshaaraResult.failure(mappedError)
            }
        }
    }

    /**
     * Strictly serializes CreateRideRequestDto to JSON.
     * Ensures NO extra unrecognized fields (fare, seatCapacity, etc.) are present.
     */
    internal fun serializeRequest(request: CreateRideRequestDto): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"tripId\":\"").append(escapeJson(request.tripId)).append("\",")

        sb.append("\"pickup\":")
        serializeWaypoint(sb, request.pickup)
        sb.append(",")

        sb.append("\"destination\":")
        serializeWaypoint(sb, request.destination)

        if (!request.discoverySessionId.isNullOrBlank()) {
            sb.append(",\"discoverySessionId\":\"").append(escapeJson(request.discoverySessionId)).append("\"")
        }

        sb.append("}")
        return sb.toString()
    }

    private fun serializeWaypoint(sb: StringBuilder, waypoint: LocationWaypointDto) {
        sb.append("{")
        sb.append("\"formattedAddress\":\"").append(escapeJson(waypoint.formattedAddress)).append("\",")
        sb.append("\"latitude\":").append(waypoint.latitude).append(",")
        sb.append("\"longitude\":").append(waypoint.longitude)

        if (!waypoint.name.isNullOrBlank()) {
            sb.append(",\"name\":\"").append(escapeJson(waypoint.name)).append("\"")
        }
        if (!waypoint.googlePlaceId.isNullOrBlank()) {
            sb.append(",\"googlePlaceId\":\"").append(escapeJson(waypoint.googlePlaceId)).append("\"")
        }
        if (!waypoint.serpApiDataId.isNullOrBlank()) {
            sb.append(",\"serpApiDataId\":\"").append(escapeJson(waypoint.serpApiDataId)).append("\"")
        }
        sb.append("}")
    }

    /**
     * Pure Kotlin JSON parser for RideRequestResponse.
     */
    internal fun parseRideRequestResponse(json: String): RideRequestResponseDto {
        val dataObj = extractJsonObject(json, "data") ?: json

        val id = extractStringValue(dataObj, "id") ?: ""
        val tripId = extractStringValue(dataObj, "tripId") ?: ""
        val driverId = extractStringValue(dataObj, "driverId") ?: ""
        val userId = extractStringValue(dataObj, "userId") ?: ""
        val status = extractStringValue(dataObj, "status") ?: "PENDING"
        val requestedAt = extractStringValue(dataObj, "requestedAt") ?: ""
        val respondedAt = extractStringValue(dataObj, "respondedAt")
        val expiresAt = extractStringValue(dataObj, "expiresAt") ?: ""
        val rejectionReason = extractStringValue(dataObj, "rejectionReason")
        val cancellationReason = extractStringValue(dataObj, "cancellationReason")
        val createdAt = extractStringValue(dataObj, "createdAt") ?: ""
        val updatedAt = extractStringValue(dataObj, "updatedAt") ?: ""

        // Pickup
        val pickupObj = extractJsonObject(dataObj, "pickup") ?: ""
        val pickupAddr = extractStringValue(pickupObj, "formattedAddress") ?: ""
        val pickupName = extractStringValue(pickupObj, "name")
        val pickupCoords = extractCoordinates(pickupObj)
        val pickup = LocationWaypointResponseDto(
            formattedAddress = pickupAddr,
            name = pickupName,
            coordinates = DiscoveryGeoJsonPointDto(coordinates = pickupCoords),
            googlePlaceId = extractStringValue(pickupObj, "googlePlaceId"),
            serpApiDataId = extractStringValue(pickupObj, "serpApiDataId")
        )

        // Destination
        val destObj = extractJsonObject(dataObj, "destination") ?: ""
        val destAddr = extractStringValue(destObj, "formattedAddress") ?: ""
        val destName = extractStringValue(destObj, "name")
        val destCoords = extractCoordinates(destObj)
        val destination = LocationWaypointResponseDto(
            formattedAddress = destAddr,
            name = destName,
            coordinates = DiscoveryGeoJsonPointDto(coordinates = destCoords),
            googlePlaceId = extractStringValue(destObj, "googlePlaceId"),
            serpApiDataId = extractStringValue(destObj, "serpApiDataId")
        )

        return RideRequestResponseDto(
            id = id,
            tripId = tripId,
            driverId = driverId,
            userId = userId,
            pickup = pickup,
            destination = destination,
            status = status,
            requestedAt = requestedAt,
            respondedAt = respondedAt,
            expiresAt = expiresAt,
            discoverySessionId = extractStringValue(dataObj, "discoverySessionId"),
            rejectionReason = rejectionReason,
            cancellationReason = cancellationReason,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    internal fun parseListRideRequestsResponse(json: String): ListRideRequestsResponseDto {
        val dataObj = extractJsonObject(json, "data") ?: json
        val itemsIndex = dataObj.indexOf("\"items\"")
        val items = mutableListOf<RideRequestResponseDto>()

        if (itemsIndex != -1) {
            val bracketStart = dataObj.indexOf('[', itemsIndex)
            val bracketEnd = findClosingBracket(dataObj, bracketStart)
            if (bracketStart != -1 && bracketEnd != -1) {
                val arrayContent = dataObj.substring(bracketStart + 1, bracketEnd)
                val objectStrings = splitJsonObjects(arrayContent)
                for (objStr in objectStrings) {
                    try {
                        items.add(parseRideRequestResponse(objStr))
                    } catch (_: Exception) {}
                }
            }
        }

        val total = extractIntValue(dataObj, "total")
        val page = extractIntValue(dataObj, "page")
        val limit = extractIntValue(dataObj, "limit") ?: 20
        val hasMore = dataObj.contains("\"hasMore\":true")
        val nextCursor = extractStringValue(dataObj, "nextCursor")

        return ListRideRequestsResponseDto(
            items = items,
            total = total,
            page = page,
            limit = limit,
            hasMore = hasMore,
            nextCursor = nextCursor
        )
    }

    private fun extractCoordinates(json: String): DoubleArray {
        val coordsIndex = json.indexOf("\"coordinates\"")
        if (coordsIndex == -1) return DoubleArray(0)

        val arrayStart = json.indexOf('[', coordsIndex)
        if (arrayStart == -1) return DoubleArray(0)

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

    private fun extractIntValue(json: String, key: String): Int? {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return null

        val colonIndex = json.indexOf(':', keyIndex)
        if (colonIndex == -1) return null

        val sb = StringBuilder()
        for (i in (colonIndex + 1) until json.length) {
            val c = json[i]
            if (c in '0'..'9') {
                sb.append(c)
            } else if (c == '-' && sb.isEmpty()) {
                sb.append(c)
            } else if (sb.isNotEmpty()) {
                break
            }
        }
        return sb.toString().toIntOrNull()
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
