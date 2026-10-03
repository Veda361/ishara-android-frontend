package com.ishara.app.data.remote.datasource

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.CancelSosRequestDto
import com.ishara.app.data.remote.dto.CreateEmergencyContactRequestDto
import com.ishara.app.data.remote.dto.CreateSosRequestDto
import com.ishara.app.data.remote.dto.EmergencyContactResponseDto
import com.ishara.app.data.remote.dto.EmergencyEventResponseDto
import com.ishara.app.data.remote.dto.SafetyLocationSnapshotDto
import com.ishara.app.data.remote.dto.UpdateEmergencyContactRequestDto

interface SafetyRemoteDataSource {

    suspend fun triggerSos(
        rideId: String,
        request: CreateSosRequestDto,
        idempotencyKey: String?,
        token: String?
    ): IshaaraResult<EmergencyEventResponseDto>

    suspend fun getActiveSosForRide(
        rideId: String,
        token: String?
    ): IshaaraResult<EmergencyEventResponseDto?>

    suspend fun listEventsForRide(
        rideId: String,
        limit: Int = 20,
        skip: Int = 0,
        token: String?
    ): IshaaraResult<List<EmergencyEventResponseDto>>

    suspend fun cancelSosByRide(
        rideId: String,
        request: CancelSosRequestDto,
        token: String?
    ): IshaaraResult<EmergencyEventResponseDto>

    suspend fun getSosById(
        eventId: String,
        token: String?
    ): IshaaraResult<EmergencyEventResponseDto>

    suspend fun cancelSosById(
        eventId: String,
        request: CancelSosRequestDto,
        token: String?
    ): IshaaraResult<EmergencyEventResponseDto>

    suspend fun listEmergencyContacts(
        token: String?
    ): IshaaraResult<List<EmergencyContactResponseDto>>

    suspend fun createEmergencyContact(
        request: CreateEmergencyContactRequestDto,
        token: String?
    ): IshaaraResult<EmergencyContactResponseDto>

    suspend fun updateEmergencyContact(
        contactId: String,
        request: UpdateEmergencyContactRequestDto,
        token: String?
    ): IshaaraResult<EmergencyContactResponseDto>

    suspend fun deleteEmergencyContact(
        contactId: String,
        token: String?
    ): IshaaraResult<Unit>
}

class SafetyRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : SafetyRemoteDataSource {

    private val tag = "SafetyRemoteDataSource"

    override suspend fun triggerSos(
        rideId: String,
        request: CreateSosRequestDto,
        idempotencyKey: String?,
        token: String?
    ): IshaaraResult<EmergencyEventResponseDto> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }
        if (!idempotencyKey.isNullOrBlank()) {
            headers["Idempotency-Key"] = idempotencyKey
        }

        val jsonBody = serializeCreateSosRequest(request)

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/safety/sos",
            method = HttpMethod.POST,
            headers = headers,
            body = jsonBody
        )

        return httpClient.execute(httpRequest).map { response ->
            parseEmergencyEventResponse(response.body)
        }
    }

    override suspend fun getActiveSosForRide(
        rideId: String,
        token: String?
    ): IshaaraResult<EmergencyEventResponseDto?> {
        val headers = mutableMapOf<String, String>()
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/safety/active",
            method = HttpMethod.GET,
            headers = headers
        )

        return httpClient.execute(httpRequest).map { response ->
            parseNullableEmergencyEventResponse(response.body)
        }
    }

    override suspend fun listEventsForRide(
        rideId: String,
        limit: Int,
        skip: Int,
        token: String?
    ): IshaaraResult<List<EmergencyEventResponseDto>> {
        val headers = mutableMapOf<String, String>()
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/safety/events",
            method = HttpMethod.GET,
            headers = headers,
            queryParams = mapOf(
                "limit" to limit.toString(),
                "skip" to skip.toString()
            )
        )

        return httpClient.execute(httpRequest).map { response ->
            parseEmergencyEventListResponse(response.body)
        }
    }

    override suspend fun cancelSosByRide(
        rideId: String,
        request: CancelSosRequestDto,
        token: String?
    ): IshaaraResult<EmergencyEventResponseDto> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val jsonBody = serializeCancelSosRequest(request)

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/safety/cancel",
            method = HttpMethod.POST,
            headers = headers,
            body = jsonBody
        )

        return httpClient.execute(httpRequest).map { response ->
            parseEmergencyEventResponse(response.body)
        }
    }

    override suspend fun getSosById(
        eventId: String,
        token: String?
    ): IshaaraResult<EmergencyEventResponseDto> {
        val headers = mutableMapOf<String, String>()
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/safety/events/$eventId",
            method = HttpMethod.GET,
            headers = headers
        )

        return httpClient.execute(httpRequest).map { response ->
            parseEmergencyEventResponse(response.body)
        }
    }

    override suspend fun cancelSosById(
        eventId: String,
        request: CancelSosRequestDto,
        token: String?
    ): IshaaraResult<EmergencyEventResponseDto> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val jsonBody = serializeCancelSosRequest(request)

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/safety/events/$eventId/cancel",
            method = HttpMethod.POST,
            headers = headers,
            body = jsonBody
        )

        return httpClient.execute(httpRequest).map { response ->
            parseEmergencyEventResponse(response.body)
        }
    }

    override suspend fun listEmergencyContacts(
        token: String?
    ): IshaaraResult<List<EmergencyContactResponseDto>> {
        val headers = mutableMapOf<String, String>()
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/users/me/emergency-contacts",
            method = HttpMethod.GET,
            headers = headers
        )

        return httpClient.execute(httpRequest).map { response ->
            parseEmergencyContactListResponse(response.body)
        }
    }

    override suspend fun createEmergencyContact(
        request: CreateEmergencyContactRequestDto,
        token: String?
    ): IshaaraResult<EmergencyContactResponseDto> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val jsonBody = serializeCreateEmergencyContactRequest(request)

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/users/me/emergency-contacts",
            method = HttpMethod.POST,
            headers = headers,
            body = jsonBody
        )

        return httpClient.execute(httpRequest).map { response ->
            parseEmergencyContactResponse(response.body)
        }
    }

    override suspend fun updateEmergencyContact(
        contactId: String,
        request: UpdateEmergencyContactRequestDto,
        token: String?
    ): IshaaraResult<EmergencyContactResponseDto> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val jsonBody = serializeUpdateEmergencyContactRequest(request)

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/users/me/emergency-contacts/$contactId",
            method = HttpMethod.PATCH,
            headers = headers,
            body = jsonBody
        )

        return httpClient.execute(httpRequest).map { response ->
            parseEmergencyContactResponse(response.body)
        }
    }

    override suspend fun deleteEmergencyContact(
        contactId: String,
        token: String?
    ): IshaaraResult<Unit> {
        val headers = mutableMapOf<String, String>()
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/users/me/emergency-contacts/$contactId",
            method = HttpMethod.DELETE,
            headers = headers
        )

        return httpClient.execute(httpRequest).map { }
    }

    // ── Serialization Helpers ──

    fun serializeCreateSosRequest(dto: CreateSosRequestDto): String {
        return """{"emergencyType":"${escapeJson(dto.emergencyType)}"}"""
    }

    fun serializeCancelSosRequest(dto: CancelSosRequestDto): String {
        return if (dto.reason != null) {
            """{"reason":"${escapeJson(dto.reason)}"}"""
        } else {
            "{}"
        }
    }

    fun serializeCreateEmergencyContactRequest(dto: CreateEmergencyContactRequestDto): String {
        return """{"name":"${escapeJson(dto.name)}","phoneNumber":"${escapeJson(dto.phoneNumber)}","relationship":"${escapeJson(dto.relationship)}"}"""
    }

    fun serializeUpdateEmergencyContactRequest(dto: UpdateEmergencyContactRequestDto): String {
        val parts = mutableListOf<String>()
        if (dto.name != null) parts.add(""""name":"${escapeJson(dto.name)}"""")
        if (dto.phoneNumber != null) parts.add(""""phoneNumber":"${escapeJson(dto.phoneNumber)}"""")
        if (dto.relationship != null) parts.add(""""relationship":"${escapeJson(dto.relationship)}"""")
        if (dto.isActive != null) parts.add(""""isActive":${dto.isActive}""")
        return "{${parts.joinToString(",")}}"
    }

    // ── Parsing Helpers ──

    fun parseNullableEmergencyEventResponse(json: String): EmergencyEventResponseDto? {
        val dataObj = extractJsonObject(json, "data")
        if (dataObj == null) {
            // Check if "data": null
            if (json.contains("\"data\"\\s*:\\s*null".toRegex())) {
                return null
            }
            // Root object might be the event itself or null
            return if (json.contains("\"eventId\"")) parseSingleEmergencyEventObject(json) else null
        }
        return parseSingleEmergencyEventObject(dataObj)
    }

    fun parseEmergencyEventResponse(json: String): EmergencyEventResponseDto {
        val dataObj = extractJsonObject(json, "data") ?: json
        return parseSingleEmergencyEventObject(dataObj)
    }

    private fun parseSingleEmergencyEventObject(obj: String): EmergencyEventResponseDto {
        val id = extractStringValue(obj, "id") ?: extractStringValue(obj, "_id") ?: ""
        val eventId = extractStringValue(obj, "eventId")
            ?: throw IllegalArgumentException("Missing eventId in safety response")
        val rideId = extractStringValue(obj, "rideId") ?: ""
        val tripId = extractStringValue(obj, "tripId")
        val triggeredByUserId = extractStringValue(obj, "triggeredByUserId") ?: ""
        val triggeredByRole = extractStringValue(obj, "triggeredByRole") ?: "USER"
        val driverId = extractStringValue(obj, "driverId") ?: ""
        val passengerUserId = extractStringValue(obj, "passengerUserId") ?: ""
        val emergencyType = extractStringValue(obj, "emergencyType") ?: "SOS"
        val status = extractStringValue(obj, "status") ?: "ACTIVE"

        val locSnapshotObj = extractJsonObject(obj, "locationSnapshot")
        val locationSnapshot = if (locSnapshotObj != null) {
            parseLocationSnapshot(locSnapshotObj)
        } else {
            SafetyLocationSnapshotDto()
        }

        val triggeredAt = extractStringValue(obj, "triggeredAt") ?: ""
        val acknowledgedAt = extractStringValue(obj, "acknowledgedAt")
        val resolvedAt = extractStringValue(obj, "resolvedAt")
        val cancelledAt = extractStringValue(obj, "cancelledAt")
        val cancellationReason = extractStringValue(obj, "cancellationReason")
        val createdAt = extractStringValue(obj, "createdAt") ?: triggeredAt
        val updatedAt = extractStringValue(obj, "updatedAt") ?: triggeredAt

        return EmergencyEventResponseDto(
            id = id,
            eventId = eventId,
            rideId = rideId,
            tripId = tripId,
            triggeredByUserId = triggeredByUserId,
            triggeredByRole = triggeredByRole,
            driverId = driverId,
            passengerUserId = passengerUserId,
            emergencyType = emergencyType,
            status = status,
            locationSnapshot = locationSnapshot,
            triggeredAt = triggeredAt,
            acknowledgedAt = acknowledgedAt,
            resolvedAt = resolvedAt,
            cancelledAt = cancelledAt,
            cancellationReason = cancellationReason,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun parseLocationSnapshot(obj: String): SafetyLocationSnapshotDto {
        val coordinates = extractNumberArray(obj, "coordinates")
        val accuracy = extractDoubleValue(obj, "accuracyMeters")
        val heading = extractDoubleValue(obj, "headingDegrees")
        val speed = extractDoubleValue(obj, "speedMps")
        val isStale = extractBooleanValue(obj, "isStale") ?: false
        val capturedAt = extractStringValue(obj, "capturedAt")
        val provider = extractStringValue(obj, "provider") ?: "driver_profile"

        return SafetyLocationSnapshotDto(
            coordinates = coordinates,
            accuracyMeters = accuracy,
            headingDegrees = heading,
            speedMps = speed,
            isStale = isStale,
            capturedAt = capturedAt,
            provider = provider
        )
    }

    fun parseEmergencyEventListResponse(json: String): List<EmergencyEventResponseDto> {
        val dataArrayStr = extractJsonArray(json, "data") ?: json
        val itemObjects = splitJsonObjectsInArray(dataArrayStr)
        return itemObjects.mapNotNull {
            try {
                parseSingleEmergencyEventObject(it)
            } catch (e: Exception) {
                IshaaraLogger.w(tag, "Failed to parse emergency event in list", e)
                null
            }
        }
    }

    fun parseEmergencyContactResponse(json: String): EmergencyContactResponseDto {
        val dataObj = extractJsonObject(json, "data") ?: json
        return parseSingleEmergencyContactObject(dataObj)
    }

    fun parseEmergencyContactListResponse(json: String): List<EmergencyContactResponseDto> {
        val dataArrayStr = extractJsonArray(json, "data") ?: json
        val itemObjects = splitJsonObjectsInArray(dataArrayStr)
        return itemObjects.mapNotNull {
            try {
                parseSingleEmergencyContactObject(it)
            } catch (e: Exception) {
                IshaaraLogger.w(tag, "Failed to parse emergency contact in list", e)
                null
            }
        }
    }

    private fun parseSingleEmergencyContactObject(obj: String): EmergencyContactResponseDto {
        val id = extractStringValue(obj, "id") ?: extractStringValue(obj, "_id") ?: ""
        val name = extractStringValue(obj, "name") ?: ""
        val phoneNumber = extractStringValue(obj, "phoneNumber") ?: ""
        val relationship = extractStringValue(obj, "relationship") ?: "OTHER"
        val isVerified = extractBooleanValue(obj, "isVerified") ?: false
        val isActive = extractBooleanValue(obj, "isActive") ?: true
        val createdAt = extractStringValue(obj, "createdAt") ?: ""
        val updatedAt = extractStringValue(obj, "updatedAt") ?: ""

        return EmergencyContactResponseDto(
            id = id,
            name = name,
            phoneNumber = phoneNumber,
            relationship = relationship,
            isVerified = isVerified,
            isActive = isActive,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    // ── Low-Level Pure Kotlin JSON String Helpers ──

    private fun escapeJson(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun extractStringValue(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]*)\"".toRegex()
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun extractDoubleValue(json: String, key: String): Double? {
        val pattern = "\"$key\"\\s*:\\s*([0-9.-]+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractBooleanValue(json: String, key: String): Boolean? {
        val pattern = "\"$key\"\\s*:\\s*(true|false)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }

    private fun extractNumberArray(json: String, key: String): List<Double>? {
        val pattern = "\"$key\"\\s*:\\s*\\[([^\\]]*)\\]".toRegex()
        val match = pattern.find(json)?.groupValues?.get(1) ?: return null
        if (match.isBlank()) return emptyList()
        return match.split(",").mapNotNull { it.trim().toDoubleOrNull() }
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

    private fun extractJsonArray(json: String, key: String): String? {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return null

        val colonIndex = json.indexOf(':', keyIndex)
        if (colonIndex == -1) return null

        val bracketStartIndex = json.indexOf('[', colonIndex)
        if (bracketStartIndex == -1) return null

        var depth = 0
        var inString = false
        var isEscaped = false

        for (i in bracketStartIndex until json.length) {
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
                if (char == '[') depth++
                if (char == ']') {
                    depth--
                    if (depth == 0) {
                        return json.substring(bracketStartIndex, i + 1)
                    }
                }
            }
        }
        return null
    }

    private fun splitJsonObjectsInArray(arrayJson: String): List<String> {
        val trimmed = arrayJson.trim()
        val startIndex = trimmed.indexOf('[')
        val endIndex = trimmed.lastIndexOf(']')
        if (startIndex == -1 || endIndex == -1 || startIndex >= endIndex) return emptyList()

        val content = trimmed.substring(startIndex + 1, endIndex)
        val objects = mutableListOf<String>()
        var depth = 0
        var objStart = -1
        var inString = false
        var isEscaped = false

        for (i in content.indices) {
            val char = content[i]
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
                if (char == '{') {
                    if (depth == 0) objStart = i
                    depth++
                } else if (char == '}') {
                    depth--
                    if (depth == 0 && objStart != -1) {
                        objects.add(content.substring(objStart, i + 1).trim())
                        objStart = -1
                    }
                }
            }
        }
        return objects
    }
}
