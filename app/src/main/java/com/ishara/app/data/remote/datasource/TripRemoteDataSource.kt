package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.CancelTripRequestDto
import com.ishara.app.data.remote.dto.CleanTripResponseDto
import com.ishara.app.data.remote.dto.CreateTripRequestDto
import com.ishara.app.data.remote.dto.DiscoverTripsRequestDto
import com.ishara.app.data.remote.dto.TripDto
import com.ishara.app.data.remote.dto.TripLocationDto
import com.ishara.app.data.remote.dto.TripRouteDto
import com.ishara.app.data.remote.dto.TripRouteGeometryDto

/**
 * Remote data source interface for Trips and Dispatch operations.
 */
interface TripRemoteDataSource {
    // Legacy methods
    suspend fun discoverTrips(request: DiscoverTripsRequestDto, token: String?): IshaaraResult<List<TripDto>> =
        IshaaraResult.success(emptyList())
    suspend fun getTripById(tripId: String, token: String?): IshaaraResult<TripDto> =
        IshaaraResult.failure(com.ishara.app.core.result.IshaaraError.NotFound())
    suspend fun startTrip(tripId: String, token: String): IshaaraResult<TripDto> =
        IshaaraResult.failure(com.ishara.app.core.result.IshaaraError.Unknown())
    suspend fun completeTrip(tripId: String, token: String): IshaaraResult<TripDto> =
        IshaaraResult.failure(com.ishara.app.core.result.IshaaraError.Unknown())

    // Phase A08: Driver Trip Operational Foundation
    suspend fun listDriverTrips(
        token: String,
        page: Int = 1,
        limit: Int = 20,
        status: String? = null
    ): IshaaraResult<List<CleanTripResponseDto>> = IshaaraResult.success(emptyList())

    suspend fun getDriverTripDetails(
        tripId: String,
        token: String
    ): IshaaraResult<CleanTripResponseDto> = IshaaraResult.failure(com.ishara.app.core.result.IshaaraError.NotFound())

    suspend fun createDriverTrip(
        request: CreateTripRequestDto,
        token: String
    ): IshaaraResult<CleanTripResponseDto> = IshaaraResult.failure(com.ishara.app.core.result.IshaaraError.Unknown())

    suspend fun startDriverTrip(
        tripId: String,
        token: String
    ): IshaaraResult<CleanTripResponseDto> = IshaaraResult.failure(com.ishara.app.core.result.IshaaraError.Unknown())

    suspend fun completeDriverTrip(
        tripId: String,
        token: String
    ): IshaaraResult<CleanTripResponseDto> = IshaaraResult.failure(com.ishara.app.core.result.IshaaraError.Unknown())

    suspend fun cancelDriverTrip(
        tripId: String,
        request: CancelTripRequestDto?,
        token: String
    ): IshaaraResult<CleanTripResponseDto> = IshaaraResult.failure(com.ishara.app.core.result.IshaaraError.Unknown())
}

/**
 * Production implementation of [TripRemoteDataSource].
 */
class TripRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : TripRemoteDataSource {

    override suspend fun discoverTrips(
        request: DiscoverTripsRequestDto,
        token: String?
    ): IshaaraResult<List<TripDto>> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (token != null) headers["Authorization"] = "Bearer $token"

        val body = """
            {
                "pickup": {
                    "address": "${request.pickup.address}",
                    "coordinates": [${request.pickup.coordinates[0]}, ${request.pickup.coordinates[1]}]
                },
                "destination": {
                    "address": "${request.destination.address}",
                    "coordinates": [${request.destination.coordinates[0]}, ${request.destination.coordinates[1]}]
                },
                "passengerCount": ${request.passengerCount}
            }
        """.trimIndent()

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/discovery/trips",
            method = HttpMethod.POST,
            headers = headers,
            body = body
        )

        return httpClient.execute(httpRequest).map { response ->
            parseLegacyTripList(response.body)
        }
    }

    override suspend fun getTripById(tripId: String, token: String?): IshaaraResult<TripDto> {
        val headers = mutableMapOf<String, String>()
        if (token != null) headers["Authorization"] = "Bearer $token"

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/$tripId",
            method = HttpMethod.GET,
            headers = headers
        )
        return httpClient.execute(httpRequest).map { response ->
            val clean = parseCleanTrip(extractJsonObject(response.body, "data") ?: response.body)
            TripDto(
                id = clean.id,
                driverId = clean.driverId,
                vehicleId = clean.vehicleId,
                origin = clean.origin,
                destination = clean.destination,
                status = clean.status
            )
        }
    }

    override suspend fun startTrip(tripId: String, token: String): IshaaraResult<TripDto> {
        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/$tripId/start",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token", "Content-Type" to "application/json"),
            body = "{}"
        )
        return httpClient.execute(httpRequest).map { response ->
            val clean = parseCleanTrip(extractJsonObject(response.body, "data") ?: response.body)
            TripDto(
                id = clean.id,
                driverId = clean.driverId,
                vehicleId = clean.vehicleId,
                origin = clean.origin,
                destination = clean.destination,
                status = clean.status
            )
        }
    }

    override suspend fun completeTrip(tripId: String, token: String): IshaaraResult<TripDto> {
        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/$tripId/complete",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token", "Content-Type" to "application/json"),
            body = "{}"
        )
        return httpClient.execute(httpRequest).map { response ->
            val clean = parseCleanTrip(extractJsonObject(response.body, "data") ?: response.body)
            TripDto(
                id = clean.id,
                driverId = clean.driverId,
                vehicleId = clean.vehicleId,
                origin = clean.origin,
                destination = clean.destination,
                status = clean.status
            )
        }
    }

    // =========================================================================
    // Phase A08: Driver-side Trip & Dispatch Operations
    // =========================================================================

    override suspend fun listDriverTrips(
        token: String,
        page: Int,
        limit: Int,
        status: String?
    ): IshaaraResult<List<CleanTripResponseDto>> {
        val queryParams = mutableMapOf(
            "page" to page.toString(),
            "limit" to limit.toString()
        )
        if (!status.isNullOrBlank()) {
            queryParams["status"] = status
        }

        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/trips",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token"),
            queryParams = queryParams
        )

        return httpClient.execute(request).map { response ->
            parseCleanTripList(response.body)
        }
    }

    override suspend fun getDriverTripDetails(
        tripId: String,
        token: String
    ): IshaaraResult<CleanTripResponseDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/$tripId",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )

        return httpClient.execute(request).map { response ->
            parseCleanTrip(extractJsonObject(response.body, "data") ?: response.body)
        }
    }

    override suspend fun createDriverTrip(
        request: CreateTripRequestDto,
        token: String
    ): IshaaraResult<CleanTripResponseDto> {
        val bodyJson = buildCreateTripJson(request)

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = bodyJson
        )

        return httpClient.execute(httpRequest).map { response ->
            parseCleanTrip(extractJsonObject(response.body, "data") ?: response.body)
        }
    }

    override suspend fun startDriverTrip(
        tripId: String,
        token: String
    ): IshaaraResult<CleanTripResponseDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/$tripId/start",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = "{}"
        )

        return httpClient.execute(request).map { response ->
            parseCleanTrip(extractJsonObject(response.body, "data") ?: response.body)
        }
    }

    override suspend fun completeDriverTrip(
        tripId: String,
        token: String
    ): IshaaraResult<CleanTripResponseDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/$tripId/complete",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = "{}"
        )

        return httpClient.execute(request).map { response ->
            parseCleanTrip(extractJsonObject(response.body, "data") ?: response.body)
        }
    }

    override suspend fun cancelDriverTrip(
        tripId: String,
        request: CancelTripRequestDto?,
        token: String
    ): IshaaraResult<CleanTripResponseDto> {
        val bodyJson = if (request?.reason != null) {
            """{"reason":"${escapeJson(request.reason)}"}"""
        } else {
            "{}"
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/$tripId/cancel",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = bodyJson
        )

        return httpClient.execute(httpRequest).map { response ->
            parseCleanTrip(extractJsonObject(response.body, "data") ?: response.body)
        }
    }

    // =========================================================================
    // JSON Serialization & Parsing Helpers
    // =========================================================================

    private fun buildCreateTripJson(request: CreateTripRequestDto): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"vehicleId\":\"").append(escapeJson(request.vehicleId)).append("\",")
        sb.append("\"origin\":").append(buildLocationJson(request.origin)).append(",")
        sb.append("\"destination\":").append(buildLocationJson(request.destination))

        if (request.scheduledDepartureAt != null) {
            sb.append(",\"scheduledDepartureAt\":\"").append(escapeJson(request.scheduledDepartureAt)).append("\"")
        }
        sb.append("}")
        return sb.toString()
    }

    private fun buildLocationJson(location: com.ishara.app.data.remote.dto.LocationInputDto): String {
        val sb = StringBuilder()
        sb.append("{")
        if (location.name != null) {
            sb.append("\"name\":\"").append(escapeJson(location.name)).append("\",")
        }
        sb.append("\"formattedAddress\":\"").append(escapeJson(location.formattedAddress)).append("\",")
        sb.append("\"latitude\":").append(location.latitude).append(",")
        sb.append("\"longitude\":").append(location.longitude)
        if (location.googlePlaceId != null) {
            sb.append(",\"googlePlaceId\":\"").append(escapeJson(location.googlePlaceId)).append("\"")
        }
        if (location.serpApiDataId != null) {
            sb.append(",\"serpApiDataId\":\"").append(escapeJson(location.serpApiDataId)).append("\"")
        }
        sb.append("}")
        return sb.toString()
    }

    private fun parseCleanTripList(json: String): List<CleanTripResponseDto> {
        val dataArray = extractJsonArray(json, "data")
            ?: (if (json.trim().startsWith("[")) json.trim() else null)
            ?: return emptyList()

        val objects = splitJsonObjects(dataArray)
        return objects.mapNotNull { obj ->
            try {
                parseCleanTrip(obj)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun parseCleanTrip(json: String): CleanTripResponseDto {
        val originObj = extractJsonObject(json, "origin") ?: ""
        val destObj = extractJsonObject(json, "destination") ?: ""
        val routeObj = extractJsonObject(json, "route")

        return CleanTripResponseDto(
            id = extractJsonString(json, "id") ?: extractJsonString(json, "_id") ?: "",
            driverId = extractJsonString(json, "driverId") ?: "",
            vehicleId = extractJsonString(json, "vehicleId") ?: "",
            agencyId = extractJsonString(json, "agencyId"),
            operatorId = extractJsonString(json, "operatorId"),
            origin = parseTripLocation(originObj),
            destination = parseTripLocation(destObj),
            route = routeObj?.let { parseTripRoute(it) },
            status = extractJsonString(json, "status") ?: "CREATED",
            scheduledDepartureAt = extractJsonString(json, "scheduledDepartureAt"),
            startedAt = extractJsonString(json, "startedAt"),
            completedAt = extractJsonString(json, "completedAt"),
            cancelledAt = extractJsonString(json, "cancelledAt"),
            cancellationReason = extractJsonString(json, "cancellationReason"),
            cancelledBy = extractJsonString(json, "cancelledBy"),
            cancelledByRole = extractJsonString(json, "cancelledByRole"),
            createdBy = extractJsonString(json, "createdBy"),
            createdByRole = extractJsonString(json, "createdByRole"),
            createdAt = extractJsonString(json, "createdAt") ?: "",
            updatedAt = extractJsonString(json, "updatedAt") ?: ""
        )
    }

    private fun parseTripLocation(json: String): TripLocationDto {
        val name = extractJsonString(json, "name")
        val formattedAddress = extractJsonString(json, "formattedAddress") ?: extractJsonString(json, "address") ?: ""
        val googlePlaceId = extractJsonString(json, "googlePlaceId")
        val serpApiDataId = extractJsonString(json, "serpApiDataId")

        // Parse coordinates from GeoJSON point coordinates: [longitude, latitude]
        val coordinatesObj = extractJsonObject(json, "coordinates")
        val coordsArray = (if (coordinatesObj != null) extractJsonArray(coordinatesObj, "coordinates") else null)
            ?: extractJsonArray(json, "coordinates")

        val coords = if (coordsArray != null) {
            parseNumberArray(coordsArray)
        } else {
            val lat = extractJsonDouble(json, "latitude") ?: 0.0
            val lng = extractJsonDouble(json, "longitude") ?: 0.0
            doubleArrayOf(lng, lat)
        }

        return TripLocationDto(
            address = formattedAddress,
            coordinates = coords,
            name = name,
            googlePlaceId = googlePlaceId,
            serpApiDataId = serpApiDataId
        )
    }

    private fun parseTripRoute(json: String): TripRouteDto {
        val distanceMeters = extractJsonDouble(json, "distanceMeters")
        val durationSeconds = extractJsonDouble(json, "durationSeconds")
        val provider = extractJsonString(json, "provider")

        val geometryObj = extractJsonObject(json, "geometry")
        val geometry = geometryObj?.let { geom ->
            val type = extractJsonString(geom, "type") ?: "LineString"
            val coordsArrayStr = extractJsonArray(geom, "coordinates")
            val coordsList = mutableListOf<DoubleArray>()
            if (coordsArrayStr != null) {
                val subArrays = splitJsonSubArrays(coordsArrayStr)
                for (sub in subArrays) {
                    coordsList.add(parseNumberArray(sub))
                }
            }
            TripRouteGeometryDto(type = type, coordinates = coordsList)
        }

        return TripRouteDto(
            geometry = geometry,
            distanceMeters = distanceMeters,
            durationSeconds = durationSeconds,
            provider = provider
        )
    }

    private fun parseLegacyTripList(json: String): List<TripDto> {
        val dataArray = extractJsonArray(json, "data")
            ?: (if (json.trim().startsWith("[")) json.trim() else null)
            ?: return emptyList()

        val objects = splitJsonObjects(dataArray)
        return objects.map { obj ->
            val clean = parseCleanTrip(obj)
            TripDto(
                id = clean.id,
                driverId = clean.driverId,
                vehicleId = clean.vehicleId,
                origin = clean.origin,
                destination = clean.destination,
                status = clean.status
            )
        }
    }

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*(?:\"([^\"]*)\"|null)")
        val match = pattern.find(json) ?: return null
        return if (match.groupValues[1].isEmpty() && match.value.contains("null")) null else match.groupValues[1]
    }

    private fun extractJsonDouble(json: String, key: String): Double? {
        val pattern = Regex("\"$key\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        return pattern.find(json)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractJsonObject(json: String, key: String): String? {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return null

        val colonIndex = json.indexOf(':', keyIndex)
        if (colonIndex == -1) return null

        var cursor = colonIndex + 1
        while (cursor < json.length && json[cursor].isWhitespace()) {
            cursor++
        }

        if (cursor >= json.length) return null
        if (json.startsWith("null", cursor)) return null
        if (json[cursor] != '{') return null

        val startIndex = cursor
        var depth = 0
        var inQuotes = false
        var isEscaped = false

        while (cursor < json.length) {
            val c = json[cursor]
            if (isEscaped) {
                isEscaped = false
            } else if (c == '\\') {
                isEscaped = true
            } else if (c == '"') {
                inQuotes = !inQuotes
            } else if (!inQuotes) {
                if (c == '{') depth++
                else if (c == '}') {
                    depth--
                    if (depth == 0) {
                        return json.substring(startIndex, cursor + 1)
                    }
                }
            }
            cursor++
        }
        return null
    }

    private fun extractJsonArray(json: String, key: String): String? {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return null

        val colonIndex = json.indexOf(':', keyIndex)
        if (colonIndex == -1) return null

        var cursor = colonIndex + 1
        while (cursor < json.length && json[cursor].isWhitespace()) {
            cursor++
        }

        if (cursor >= json.length) return null
        if (json.startsWith("null", cursor)) return null
        if (json[cursor] != '[') return null

        val startIndex = cursor
        var depth = 0
        var inQuotes = false
        var isEscaped = false

        while (cursor < json.length) {
            val c = json[cursor]
            if (isEscaped) {
                isEscaped = false
            } else if (c == '\\') {
                isEscaped = true
            } else if (c == '"') {
                inQuotes = !inQuotes
            } else if (!inQuotes) {
                if (c == '[') depth++
                else if (c == ']') {
                    depth--
                    if (depth == 0) {
                        return json.substring(startIndex, cursor + 1)
                    }
                }
            }
            cursor++
        }
        return null
    }

    private fun splitJsonObjects(arrayJson: String): List<String> {
        val list = mutableListOf<String>()
        val trimmed = arrayJson.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return list

        val inner = trimmed.substring(1, trimmed.length - 1).trim()
        var cursor = 0
        var inQuotes = false
        var isEscaped = false
        var depth = 0
        var objectStart = -1

        while (cursor < inner.length) {
            val c = inner[cursor]
            if (isEscaped) {
                isEscaped = false
            } else if (c == '\\') {
                isEscaped = true
            } else if (c == '"') {
                inQuotes = !inQuotes
            } else if (!inQuotes) {
                if (c == '{') {
                    if (depth == 0) objectStart = cursor
                    depth++
                } else if (c == '}') {
                    depth--
                    if (depth == 0 && objectStart != -1) {
                        list.add(inner.substring(objectStart, cursor + 1))
                        objectStart = -1
                    }
                }
            }
            cursor++
        }
        return list
    }

    private fun splitJsonSubArrays(arrayJson: String): List<String> {
        val list = mutableListOf<String>()
        val trimmed = arrayJson.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return list

        val inner = trimmed.substring(1, trimmed.length - 1).trim()
        var cursor = 0
        var inQuotes = false
        var isEscaped = false
        var depth = 0
        var arrayStart = -1

        while (cursor < inner.length) {
            val c = inner[cursor]
            if (isEscaped) {
                isEscaped = false
            } else if (c == '\\') {
                isEscaped = true
            } else if (c == '"') {
                inQuotes = !inQuotes
            } else if (!inQuotes) {
                if (c == '[') {
                    if (depth == 0) arrayStart = cursor
                    depth++
                } else if (c == ']') {
                    depth--
                    if (depth == 0 && arrayStart != -1) {
                        list.add(inner.substring(arrayStart, cursor + 1))
                        arrayStart = -1
                    }
                }
            }
            cursor++
        }
        return list
    }

    private fun parseNumberArray(arrayJson: String): DoubleArray {
        val trimmed = arrayJson.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return doubleArrayOf(0.0, 0.0)
        val content = trimmed.substring(1, trimmed.length - 1).trim()
        if (content.isEmpty()) return doubleArrayOf(0.0, 0.0)

        val parts = content.split(",")
        val result = DoubleArray(parts.size)
        for (i in parts.indices) {
            result[i] = parts[i].trim().toDoubleOrNull() ?: 0.0
        }
        return result
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
