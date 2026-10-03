package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.AssignVehicleRequestDto
import com.ishara.app.data.remote.dto.AssignedVehicleResponseDto
import com.ishara.app.data.remote.dto.CreateVehicleRequestDto
import com.ishara.app.data.remote.dto.DriverVehicleAssignmentDto
import com.ishara.app.data.remote.dto.UnassignVehicleRequestDto
import com.ishara.app.data.remote.dto.VehicleDto

/**
 * Remote data source interface for Vehicle & Driver-Vehicle Assignment domain (Phase A07).
 */
interface VehicleRemoteDataSource {
    suspend fun getMyAssignedVehicle(token: String): IshaaraResult<AssignedVehicleResponseDto>
    suspend fun listMyVehicles(token: String): IshaaraResult<List<VehicleDto>>
    suspend fun getVehicleById(vehicleId: String, token: String): IshaaraResult<VehicleDto>
    suspend fun createVehicle(request: CreateVehicleRequestDto, token: String): IshaaraResult<VehicleDto>
    suspend fun assignVehicle(vehicleId: String, request: AssignVehicleRequestDto, token: String): IshaaraResult<DriverVehicleAssignmentDto>
    suspend fun unassignVehicle(vehicleId: String, request: UnassignVehicleRequestDto, token: String): IshaaraResult<DriverVehicleAssignmentDto?>
    suspend fun getVehicleAssignmentHistory(vehicleId: String, token: String): IshaaraResult<List<DriverVehicleAssignmentDto>>
}

/**
 * Production implementation of [VehicleRemoteDataSource].
 */
class VehicleRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : VehicleRemoteDataSource {

    override suspend fun getMyAssignedVehicle(token: String): IshaaraResult<AssignedVehicleResponseDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/vehicles/me/assigned",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )

        return httpClient.execute(request).map { response ->
            parseAssignedVehicleResponse(response.body)
        }
    }

    override suspend fun listMyVehicles(token: String): IshaaraResult<List<VehicleDto>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/vehicles",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )

        return httpClient.execute(request).map { response ->
            parseVehicleList(response.body)
        }
    }

    override suspend fun getVehicleById(vehicleId: String, token: String): IshaaraResult<VehicleDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/vehicles/$vehicleId",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )

        return httpClient.execute(request).map { response ->
            parseSingleVehicle(extractJsonObject(response.body, "data") ?: response.body)
        }
    }

    override suspend fun createVehicle(request: CreateVehicleRequestDto, token: String): IshaaraResult<VehicleDto> {
        val capacityField = if (request.capacity != null) ",\"capacity\":${request.capacity}" else ""
        val bodyJson = """{"registrationNumber":"${escapeJson(request.registrationNumber)}","vehicleType":"${escapeJson(request.vehicleType)}","make":"${escapeJson(request.make)}","model":"${escapeJson(request.model)}"$capacityField}"""

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/vehicles",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = bodyJson
        )

        return httpClient.execute(httpRequest).map { response ->
            parseSingleVehicle(extractJsonObject(response.body, "data") ?: response.body)
        }
    }

    override suspend fun assignVehicle(
        vehicleId: String,
        request: AssignVehicleRequestDto,
        token: String
    ): IshaaraResult<DriverVehicleAssignmentDto> {
        val bodyJson = """{"driverId":"${escapeJson(request.driverId)}"}"""

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/vehicles/$vehicleId/assignments",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = bodyJson
        )

        return httpClient.execute(httpRequest).map { response ->
            parseSingleAssignment(extractJsonObject(response.body, "data") ?: response.body)
        }
    }

    override suspend fun unassignVehicle(
        vehicleId: String,
        request: UnassignVehicleRequestDto,
        token: String
    ): IshaaraResult<DriverVehicleAssignmentDto?> {
        val reasonField = if (request.reason != null) """{"reason":"${escapeJson(request.reason)}"}""" else "{}"

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/vehicles/$vehicleId/unassign",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = reasonField
        )

        return httpClient.execute(httpRequest).map { response ->
            val dataObj = extractJsonObject(response.body, "data")
            if (dataObj == null || dataObj == "null") null else parseSingleAssignment(dataObj)
        }
    }

    override suspend fun getVehicleAssignmentHistory(
        vehicleId: String,
        token: String
    ): IshaaraResult<List<DriverVehicleAssignmentDto>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/vehicles/$vehicleId/assignments",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )

        return httpClient.execute(request).map { response ->
            parseAssignmentList(response.body)
        }
    }

    // =========================================================================
    // JSON Parsing & Helpers
    // =========================================================================

    private fun parseAssignedVehicleResponse(json: String): AssignedVehicleResponseDto {
        val dataObj = extractJsonObject(json, "data") ?: json

        val vehicleObj = extractJsonObject(dataObj, "vehicle")
        val vehicle = if (vehicleObj != null && vehicleObj != "null") {
            parseSingleVehicle(vehicleObj)
        } else null

        val assignmentObj = extractJsonObject(dataObj, "assignment")
        val assignment = if (assignmentObj != null && assignmentObj != "null") {
            parseSingleAssignment(assignmentObj)
        } else null

        return AssignedVehicleResponseDto(
            vehicle = vehicle,
            assignment = assignment
        )
    }

    private fun parseSingleVehicle(json: String): VehicleDto {
        return VehicleDto(
            id = extractJsonString(json, "id") ?: extractJsonString(json, "_id") ?: "",
            agencyId = extractJsonString(json, "agencyId"),
            operatorId = extractJsonString(json, "operatorId"),
            assignedDriverId = extractJsonString(json, "assignedDriverId") ?: extractJsonString(json, "driverId"),
            registrationNumber = extractJsonString(json, "registrationNumber") ?: "",
            vehicleType = extractJsonString(json, "vehicleType") ?: "OTHER",
            make = extractJsonString(json, "make") ?: "",
            model = extractJsonString(json, "model") ?: "",
            capacity = extractJsonInt(json, "capacity"),
            ownershipType = extractJsonString(json, "ownershipType") ?: "INDIVIDUAL",
            isVerified = extractJsonBoolean(json, "isVerified") ?: false,
            isActive = extractJsonBoolean(json, "isActive") ?: true,
            createdAt = extractJsonString(json, "createdAt"),
            updatedAt = extractJsonString(json, "updatedAt")
        )
    }

    private fun parseSingleAssignment(json: String): DriverVehicleAssignmentDto {
        val vehicleObj = extractJsonObject(json, "vehicle")
        val nestedVehicle = if (vehicleObj != null && vehicleObj != "null") {
            parseSingleVehicle(vehicleObj)
        } else null

        return DriverVehicleAssignmentDto(
            id = extractJsonString(json, "id") ?: extractJsonString(json, "_id") ?: "",
            driverId = extractJsonString(json, "driverId") ?: "",
            vehicleId = extractJsonString(json, "vehicleId") ?: "",
            agencyId = extractJsonString(json, "agencyId"),
            status = extractJsonString(json, "status") ?: "ACTIVE",
            assignedAt = extractJsonString(json, "assignedAt") ?: "",
            unassignedAt = extractJsonString(json, "unassignedAt"),
            assignedBy = extractJsonString(json, "assignedBy") ?: "ADMIN",
            assignedByRole = extractJsonString(json, "assignedByRole") ?: "DRIVER",
            unassignedBy = extractJsonString(json, "unassignedBy"),
            unassignedByRole = extractJsonString(json, "unassignedByRole"),
            reason = extractJsonString(json, "reason"),
            vehicle = nestedVehicle,
            createdAt = extractJsonString(json, "createdAt"),
            updatedAt = extractJsonString(json, "updatedAt")
        )
    }

    private fun parseVehicleList(json: String): List<VehicleDto> {
        val dataArray = extractJsonArray(json, "data") ?: return emptyList()
        val objects = splitJsonObjects(dataArray)
        return objects.map { parseSingleVehicle(it) }
    }

    private fun parseAssignmentList(json: String): List<DriverVehicleAssignmentDto> {
        val dataArray = extractJsonArray(json, "data") ?: return emptyList()
        val objects = splitJsonObjects(dataArray)
        return objects.map { parseSingleAssignment(it) }
    }

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*(?:\"([^\"]*)\"|null)")
        val match = pattern.find(json) ?: return null
        return if (match.groupValues[1].isEmpty() && match.value.contains("null")) null else match.groupValues[1]
    }

    private fun extractJsonBoolean(json: String, key: String): Boolean? {
        val pattern = Regex("\"$key\"\\s*:\\s*(true|false)")
        return pattern.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }

    private fun extractJsonInt(json: String, key: String): Int? {
        val pattern = Regex("\"$key\"\\s*:\\s*(\\d+)")
        return pattern.find(json)?.groupValues?.get(1)?.toIntOrNull()
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
                        return json.substring(startIndex + 1, cursor)
                    }
                }
            }
            cursor++
        }
        return null
    }

    private fun splitJsonObjects(arrayContent: String): List<String> {
        val result = mutableListOf<String>()
        var cursor = 0
        var inQuotes = false
        var isEscaped = false
        var depth = 0
        var objectStart = -1

        while (cursor < arrayContent.length) {
            val c = arrayContent[cursor]
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
                        result.add(arrayContent.substring(objectStart, cursor + 1))
                        objectStart = -1
                    }
                }
            }
            cursor++
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
