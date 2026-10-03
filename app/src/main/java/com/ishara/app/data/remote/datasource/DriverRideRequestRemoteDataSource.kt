package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.CancelRideRequestDto
import com.ishara.app.data.remote.dto.DriverRideRequestListResponse
import com.ishara.app.data.remote.dto.DriverRideResponseDto
import com.ishara.app.data.remote.dto.LocationDto
import com.ishara.app.data.remote.dto.RejectRideRequestRequestDto
import com.ishara.app.data.remote.dto.RideRequestItemDto

/**
 * Remote data source abstraction for Driver Ride Requests and Passenger Boarding lifecycle.
 * Strictly adheres to verified backend contract in ISHAARA_DRIVER_RIDE_REQUEST_BACKEND_CONTRACT.md.
 */
interface DriverRideRequestRemoteDataSource {
    suspend fun getDriverRideRequests(
        status: String? = null,
        tripId: String? = null,
        page: Int = 1,
        limit: Int = 20,
        cursor: String? = null,
        token: String
    ): IshaaraResult<DriverRideRequestListResponse>

    suspend fun getRideRequestDetails(
        requestId: String,
        token: String
    ): IshaaraResult<RideRequestItemDto>

    suspend fun acceptRideRequest(
        requestId: String,
        token: String
    ): IshaaraResult<RideRequestItemDto>

    suspend fun rejectRideRequest(
        requestId: String,
        reason: String?,
        token: String
    ): IshaaraResult<RideRequestItemDto>

    suspend fun getDriverRides(
        status: String? = null,
        page: Int = 1,
        limit: Int = 20,
        token: String
    ): IshaaraResult<List<DriverRideResponseDto>>

    suspend fun markDriverArrived(
        rideId: String,
        token: String
    ): IshaaraResult<DriverRideResponseDto>

    suspend fun markPassengerBoarded(
        rideId: String,
        token: String
    ): IshaaraResult<DriverRideResponseDto>

    suspend fun startRide(
        rideId: String,
        token: String
    ): IshaaraResult<DriverRideResponseDto>

    suspend fun completeRide(
        rideId: String,
        token: String
    ): IshaaraResult<DriverRideResponseDto>

    suspend fun cancelRide(
        rideId: String,
        reason: String,
        token: String
    ): IshaaraResult<DriverRideResponseDto>
}

/**
 * Production implementation of DriverRideRequestRemoteDataSource using pure Kotlin JSON parsing.
 */
class DriverRideRequestRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : DriverRideRequestRemoteDataSource {

    override suspend fun getDriverRideRequests(
        status: String?,
        tripId: String?,
        page: Int,
        limit: Int,
        cursor: String?,
        token: String
    ): IshaaraResult<DriverRideRequestListResponse> {
        val queryParams = mutableMapOf<String, String>()
        if (!status.isNullOrBlank()) queryParams["status"] = status
        if (!tripId.isNullOrBlank()) queryParams["tripId"] = tripId
        queryParams["page"] = page.toString()
        queryParams["limit"] = limit.toString()
        if (!cursor.isNullOrBlank()) queryParams["cursor"] = cursor

        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/ride-requests",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token"),
            queryParams = queryParams
        )

        return httpClient.execute(request).map { response ->
            parseRideRequestList(response.body)
        }
    }

    override suspend fun getRideRequestDetails(
        requestId: String,
        token: String
    ): IshaaraResult<RideRequestItemDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/ride-requests/$requestId",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )

        return httpClient.execute(request).map { response ->
            parseRideRequestItem(extractDataJson(response.body))
        }
    }

    override suspend fun acceptRideRequest(
        requestId: String,
        token: String
    ): IshaaraResult<RideRequestItemDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/ride-requests/$requestId/accept",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = "{}"
        )

        return httpClient.execute(request).map { response ->
            parseRideRequestItem(extractDataJson(response.body))
        }
    }

    override suspend fun rejectRideRequest(
        requestId: String,
        reason: String?,
        token: String
    ): IshaaraResult<RideRequestItemDto> {
        val body = if (reason.isNullOrBlank()) {
            "{}"
        } else {
            """{"reason":"${escapeJson(reason)}"}"""
        }

        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/ride-requests/$requestId/reject",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = body
        )

        return httpClient.execute(request).map { response ->
            parseRideRequestItem(extractDataJson(response.body))
        }
    }

    override suspend fun getDriverRides(
        status: String?,
        page: Int,
        limit: Int,
        token: String
    ): IshaaraResult<List<DriverRideResponseDto>> {
        val queryParams = mutableMapOf<String, String>()
        if (!status.isNullOrBlank()) queryParams["status"] = status
        queryParams["page"] = page.toString()
        queryParams["limit"] = limit.toString()

        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/rides",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token"),
            queryParams = queryParams
        )

        return httpClient.execute(request).map { response ->
            parseRideList(response.body)
        }
    }

    override suspend fun markDriverArrived(
        rideId: String,
        token: String
    ): IshaaraResult<DriverRideResponseDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/arrive",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = "{}"
        )

        return httpClient.execute(request).map { response ->
            parseRideItem(extractDataJson(response.body))
        }
    }

    override suspend fun markPassengerBoarded(
        rideId: String,
        token: String
    ): IshaaraResult<DriverRideResponseDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/pickup",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = "{}"
        )

        return httpClient.execute(request).map { response ->
            parseRideItem(extractDataJson(response.body))
        }
    }

    override suspend fun startRide(
        rideId: String,
        token: String
    ): IshaaraResult<DriverRideResponseDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/start",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = "{}"
        )

        return httpClient.execute(request).map { response ->
            parseRideItem(extractDataJson(response.body))
        }
    }

    override suspend fun completeRide(
        rideId: String,
        token: String
    ): IshaaraResult<DriverRideResponseDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/complete",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = "{}"
        )

        return httpClient.execute(request).map { response ->
            parseRideItem(extractDataJson(response.body))
        }
    }

    override suspend fun cancelRide(
        rideId: String,
        reason: String,
        token: String
    ): IshaaraResult<DriverRideResponseDto> {
        val body = """{"reason":"${escapeJson(reason)}"}"""
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/cancel",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = body
        )

        return httpClient.execute(request).map { response ->
            parseRideItem(extractDataJson(response.body))
        }
    }

    // --- JSON PARSING HELPERS ---

    private fun extractDataJson(json: String): String {
        return extractJsonObject(json, "data") ?: json
    }

    internal fun parseRideRequestList(json: String): DriverRideRequestListResponse {
        val dataObj = extractDataJson(json)
        val itemsArray = extractJsonArray(dataObj, "items") ?: "[]"
        val total = extractInt(dataObj, "total") ?: 0
        val page = extractInt(dataObj, "page") ?: 1
        val limit = extractInt(dataObj, "limit") ?: 20
        val hasMore = extractBoolean(dataObj, "hasMore") ?: false

        val items = parseArrayOfObjects(itemsArray).map { parseRideRequestItem(it) }

        return DriverRideRequestListResponse(
            items = items,
            total = total,
            page = page,
            limit = limit,
            hasMore = hasMore
        )
    }

    internal fun parseRideList(json: String): List<DriverRideResponseDto> {
        val dataObj = extractDataJson(json)
        val itemsArray = extractJsonArray(dataObj, "items") ?: if (dataObj.trim().startsWith("[")) dataObj else "[]"
        return parseArrayOfObjects(itemsArray).map { parseRideItem(it) }
    }

    internal fun parseRideRequestItem(json: String): RideRequestItemDto {
        val id = extractStringValue(json, "id") ?: ""
        val tripId = extractStringValue(json, "tripId") ?: ""
        val driverId = extractStringValue(json, "driverId") ?: ""
        val userId = extractStringValue(json, "userId") ?: ""
        val status = extractStringValue(json, "status") ?: "PENDING"
        val requestedAt = extractStringValue(json, "requestedAt") ?: ""
        val expiresAt = extractStringValue(json, "expiresAt")
        val respondedAt = extractStringValue(json, "respondedAt")
        val rejectionReason = extractStringValue(json, "rejectionReason")
        val cancellationReason = extractStringValue(json, "cancellationReason")

        val pickupObj = extractJsonObject(json, "pickup") ?: "{}"
        val pickup = LocationDto(
            formattedAddress = extractStringValue(pickupObj, "formattedAddress"),
            coordinates = extractCoordinates(pickupObj)
        )

        val destinationObj = extractJsonObject(json, "destination") ?: "{}"
        val destination = LocationDto(
            formattedAddress = extractStringValue(destinationObj, "formattedAddress"),
            coordinates = extractCoordinates(destinationObj)
        )

        return RideRequestItemDto(
            id = id,
            tripId = tripId,
            driverId = driverId,
            userId = userId,
            pickup = pickup,
            destination = destination,
            status = status,
            requestedAt = requestedAt,
            expiresAt = expiresAt,
            respondedAt = respondedAt,
            rejectionReason = rejectionReason,
            cancellationReason = cancellationReason
        )
    }

    internal fun parseRideItem(json: String): DriverRideResponseDto {
        val id = extractStringValue(json, "id") ?: ""
        val rideRequestId = extractStringValue(json, "rideRequestId")
        val tripId = extractStringValue(json, "tripId") ?: ""
        val driverId = extractStringValue(json, "driverId") ?: ""
        val userId = extractStringValue(json, "userId") ?: ""
        val status = extractStringValue(json, "status") ?: "CREATED"
        val pickupTime = extractStringValue(json, "pickupTime")
        val startTime = extractStringValue(json, "startTime")
        val completionTime = extractStringValue(json, "completionTime")
        val cancellationReason = extractStringValue(json, "cancellationReason")
        val createdAt = extractStringValue(json, "createdAt")
        val updatedAt = extractStringValue(json, "updatedAt")

        val pickupObj = extractJsonObject(json, "pickup") ?: "{}"
        val pickup = LocationDto(
            formattedAddress = extractStringValue(pickupObj, "formattedAddress"),
            coordinates = extractCoordinates(pickupObj)
        )

        val destinationObj = extractJsonObject(json, "destination") ?: "{}"
        val destination = LocationDto(
            formattedAddress = extractStringValue(destinationObj, "formattedAddress"),
            coordinates = extractCoordinates(destinationObj)
        )

        return DriverRideResponseDto(
            id = id,
            rideRequestId = rideRequestId,
            tripId = tripId,
            driverId = driverId,
            userId = userId,
            pickup = pickup,
            destination = destination,
            status = status,
            pickupTime = pickupTime,
            startTime = startTime,
            completionTime = completionTime,
            cancellationReason = cancellationReason,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun extractCoordinates(json: String): List<Double> {
        val coordsIndex = json.indexOf("\"coordinates\"")
        if (coordsIndex == -1) return emptyList()

        val arrayStart = json.indexOf('[', coordsIndex)
        if (arrayStart == -1) return emptyList()

        val arrayEnd = json.indexOf(']', arrayStart)
        if (arrayEnd == -1) return emptyList()

        val content = json.substring(arrayStart + 1, arrayEnd).trim()
        if (content.isEmpty()) return emptyList()
        return content.split(",").mapNotNull { it.trim().toDoubleOrNull() }
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

    private fun extractJsonArray(json: String, key: String): String? {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return null
        val colonIndex = json.indexOf(':', keyIndex)
        if (colonIndex == -1) return null
        val bracketIndex = json.indexOf('[', colonIndex)
        if (bracketIndex == -1) return null
        val endIndex = findClosingChar(json, bracketIndex, '[', ']')
        if (endIndex == -1) return null
        return json.substring(bracketIndex, endIndex + 1)
    }

    private fun parseArrayOfObjects(arrayJson: String): List<String> {
        val results = mutableListOf<String>()
        var i = 0
        while (i < arrayJson.length) {
            if (arrayJson[i] == '{') {
                val end = findClosingChar(arrayJson, i, '{', '}')
                if (end != -1) {
                    results.add(arrayJson.substring(i, end + 1))
                    i = end + 1
                    continue
                }
            }
            i++
        }
        return results
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

    private fun extractInt(json: String, key: String): Int? {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return null
        val colonIndex = json.indexOf(':', keyIndex)
        if (colonIndex == -1) return null
        val remaining = json.substring(colonIndex + 1).trimStart()
        val numStr = remaining.takeWhile { it.isDigit() || it == '-' }
        return numStr.toIntOrNull()
    }

    private fun extractBoolean(json: String, key: String): Boolean? {
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
