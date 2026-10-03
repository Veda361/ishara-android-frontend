package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.TransitTripDriverDto
import com.ishara.app.data.remote.dto.TransitTripDto
import com.ishara.app.data.remote.dto.TransitTripLocationDto
import com.ishara.app.data.remote.dto.TransitTripRouteDto
import com.ishara.app.data.remote.dto.TransitTripVehicleDto

interface TransitRemoteDataSource {
    suspend fun listActiveTrips(token: String?): IshaaraResult<List<TransitTripDto>>
    suspend fun getTripById(tripId: String, token: String?): IshaaraResult<TransitTripDto>
}

class TransitRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : TransitRemoteDataSource {

    override suspend fun listActiveTrips(token: String?): IshaaraResult<List<TransitTripDto>> {
        val headers = mutableMapOf<String, String>()
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/active",
            method = HttpMethod.GET,
            headers = headers
        )

        return httpClient.execute(httpRequest).map { response ->
            parseActiveTripsResponse(response.body)
        }
    }

    override suspend fun getTripById(tripId: String, token: String?): IshaaraResult<TransitTripDto> {
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
            parseSingleTripResponse(response.body)
        }
    }

    internal fun parseActiveTripsResponse(json: String): List<TransitTripDto> {
        if (json.isBlank()) return emptyList()

        val dataContent = extractDataArrayContent(json) ?: return emptyList()
        val objects = splitJsonObjects(dataContent)
        return objects.mapNotNull { parseTripDto(it) }
    }

    internal fun parseSingleTripResponse(json: String): TransitTripDto {
        val dataObj = extractJsonObject(json, "data") ?: json
        return parseTripDto(dataObj)
            ?: throw IllegalArgumentException("Failed to parse transit trip from payload")
    }

    private fun parseTripDto(json: String): TransitTripDto? {
        val id = extractStringValue(json, "id") ?: extractStringValue(json, "_id") ?: return null
        val status = extractStringValue(json, "status") ?: "ACTIVE"
        val startedAt = extractStringValue(json, "startedAt")
        val createdAt = extractStringValue(json, "createdAt") ?: ""

        val originObj = extractJsonObject(json, "origin") ?: return null
        val origin = parseLocationDto(originObj) ?: return null

        val destObj = extractJsonObject(json, "destination") ?: return null
        val destination = parseLocationDto(destObj) ?: return null

        val routeObj = extractJsonObject(json, "route")
        val route = routeObj?.let { parseRouteDto(it) }

        val driverObj = extractJsonObject(json, "driver")
        val driver = TransitTripDriverDto(
            id = driverObj?.let { extractStringValue(it, "id") } ?: "",
            name = driverObj?.let { extractStringValue(it, "name") } ?: "Verified Driver",
            image = driverObj?.let { extractStringValue(it, "image") }
        )

        val vehicleObj = extractJsonObject(json, "vehicle")
        val vehicle = TransitTripVehicleDto(
            id = vehicleObj?.let { extractStringValue(it, "id") } ?: "",
            registrationNumber = vehicleObj?.let { extractStringValue(it, "registrationNumber") } ?: "UP65XXXXXX",
            vehicleType = vehicleObj?.let { extractStringValue(it, "vehicleType") } ?: "BUS",
            make = vehicleObj?.let { extractStringValue(it, "make") } ?: "Standard",
            model = vehicleObj?.let { extractStringValue(it, "model") } ?: "Vehicle"
        )

        return TransitTripDto(
            id = id,
            status = status,
            origin = origin,
            destination = destination,
            route = route,
            startedAt = startedAt,
            createdAt = createdAt,
            driver = driver,
            vehicle = vehicle
        )
    }

    private fun parseLocationDto(json: String): TransitTripLocationDto? {
        val address = extractStringValue(json, "formattedAddress")
            ?: extractStringValue(json, "address")
            ?: return null
        val name = extractStringValue(json, "name")
        val googlePlaceId = extractStringValue(json, "googlePlaceId")
        val serpApiDataId = extractStringValue(json, "serpApiDataId")

        val coordArray = extractCoordinates(json)
        return TransitTripLocationDto(
            name = name,
            formattedAddress = address,
            coordinates = coordArray,
            googlePlaceId = googlePlaceId,
            serpApiDataId = serpApiDataId
        )
    }

    private fun parseRouteDto(json: String): TransitTripRouteDto {
        val distance = extractDoubleValue(json, "distanceMeters")
        val duration = extractLongValue(json, "durationSeconds")
        val provider = extractStringValue(json, "provider")

        val geomObj = extractJsonObject(json, "geometry")
        val coordsList = mutableListOf<DoubleArray>()
        if (geomObj != null) {
            val coordsIndex = geomObj.indexOf("\"coordinates\"")
            if (coordsIndex != -1) {
                val arrayStart = geomObj.indexOf('[', coordsIndex)
                if (arrayStart != -1) {
                    val arrayEnd = findClosingBracket(geomObj, arrayStart)
                    if (arrayEnd > arrayStart) {
                        val inner = geomObj.substring(arrayStart + 1, arrayEnd).trim()
                        val pairs = splitJsonArrays(inner)
                        for (p in pairs) {
                            val nums = p.trim().removeSurrounding("[", "]").split(",")
                                .mapNotNull { it.trim().toDoubleOrNull() }
                            if (nums.size >= 2) {
                                coordsList.add(doubleArrayOf(nums[0], nums[1]))
                            }
                        }
                    }
                }
            }
        }

        return TransitTripRouteDto(
            geometryCoordinates = coordsList,
            distanceMeters = distance,
            durationSeconds = duration,
            provider = provider
        )
    }

    private fun extractCoordinates(json: String): DoubleArray {
        val coordIndex = json.indexOf("\"coordinates\"")
        if (coordIndex == -1) return DoubleArray(0)

        val arrayStart = json.indexOf('[', coordIndex)
        if (arrayStart == -1) return DoubleArray(0)

        val arrayEnd = json.indexOf(']', arrayStart)
        if (arrayEnd == -1) return DoubleArray(0)

        val content = json.substring(arrayStart + 1, arrayEnd).trim()
        val parts = content.split(",").mapNotNull { it.trim().toDoubleOrNull() }
        return parts.toDoubleArray()
    }

    private fun extractDataArrayContent(json: String): String? {
        val dataIndex = json.indexOf("\"data\"")
        val searchStart = if (dataIndex != -1) dataIndex else 0
        val arrayStart = json.indexOf('[', searchStart)
        if (arrayStart == -1) return null

        val arrayEnd = findClosingBracket(json, arrayStart)
        if (arrayEnd <= arrayStart) return null

        return json.substring(arrayStart + 1, arrayEnd).trim()
    }

    private fun findClosingBracket(json: String, startBracketIndex: Int): Int {
        var depth = 0
        var inString = false
        var escaped = false

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
                if (c == '[') depth++
                if (c == ']') {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
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

    private fun splitJsonArrays(content: String): List<String> {
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
        }
        return list
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

    private fun extractLongValue(json: String, key: String): Long? {
        return extractDoubleValue(json, key)?.toLong()
    }

    private fun extractJsonObject(json: String, key: String): String? {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return null

        val colonIndex = json.indexOf(':', keyIndex)
        if (colonIndex == -1) return null

        val braceStartIndex = json.indexOf('{', colonIndex)
        if (braceStartIndex == -1) return null

        var depth = 0
        var inString = false
        var isEscaped = false

        for (i in braceStartIndex until json.length) {
            val char = json[i]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (char == '\\') {
                isEscaped = true
                continue
            }
            if (char == '"') {
                inString = !inString
                continue
            }
            if (!inString) {
                if (char == '{') depth++
                if (char == '}') {
                    depth--
                    if (depth == 0) {
                        return json.substring(braceStartIndex, i + 1)
                    }
                }
            }
        }
        return null
    }
}
