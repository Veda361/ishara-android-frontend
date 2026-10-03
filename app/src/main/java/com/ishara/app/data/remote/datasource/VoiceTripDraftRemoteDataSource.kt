package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.CreateVoiceTripDraftRequestDto
import com.ishara.app.data.remote.dto.ResolvedLocationDto
import com.ishara.app.data.remote.dto.VoiceTripDraftEndpointDto
import com.ishara.app.data.remote.dto.VoiceTripDraftResponseDto

interface VoiceTripDraftRemoteDataSource {
    suspend fun createVoiceTripDraft(
        request: CreateVoiceTripDraftRequestDto,
        token: String?
    ): IshaaraResult<VoiceTripDraftResponseDto>

    suspend fun cancelVoiceTripDraft(
        draftId: String,
        token: String?
    ): IshaaraResult<Unit>
}

class VoiceTripDraftRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : VoiceTripDraftRemoteDataSource {

    override suspend fun createVoiceTripDraft(
        request: CreateVoiceTripDraftRequestDto,
        token: String?
    ): IshaaraResult<VoiceTripDraftResponseDto> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val jsonBody = serializeCreateRequest(request)

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/voice/trip-drafts",
            method = HttpMethod.POST,
            headers = headers,
            body = jsonBody
        )

        return httpClient.execute(httpRequest).map { response ->
            parseVoiceTripDraftResponse(response.body)
        }
    }

    override suspend fun cancelVoiceTripDraft(
        draftId: String,
        token: String?
    ): IshaaraResult<Unit> {
        val headers = mutableMapOf<String, String>()
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/voice/trip-drafts/$draftId/cancel",
            method = HttpMethod.POST,
            headers = headers,
            body = "{}"
        )

        return httpClient.execute(httpRequest).map { }
    }

    /**
     * Serializes CreateVoiceTripDraftRequestDto strictly to JSON matching backend Zod schema.
     * No extra or unrecognized fields permitted.
     */
    internal fun serializeCreateRequest(dto: CreateVoiceTripDraftRequestDto): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"inputMode\":\"").append(escapeJson(dto.inputMode)).append("\",")
        sb.append("\"transcript\":\"").append(escapeJson(dto.transcript)).append("\"")
        if (!dto.languageHint.isNullOrBlank()) {
            sb.append(",\"languageHint\":\"").append(escapeJson(dto.languageHint)).append("\"")
        }
        sb.append("}")
        return sb.toString()
    }

    /**
     * Pure Kotlin JSON parser for VoiceTripDraftResponseDto.
     */
    internal fun parseVoiceTripDraftResponse(json: String): VoiceTripDraftResponseDto {
        val dataObj = extractJsonObject(json, "data") ?: json

        val id = extractStringValue(dataObj, "id") ?: ""
        val driverId = extractStringValue(dataObj, "driverId")
        val inputMode = extractStringValue(dataObj, "inputMode") ?: "DEVICE_TRANSCRIPT"
        val originalTranscript = extractStringValue(dataObj, "originalTranscript") ?: ""
        val normalizedTranscript = extractStringValue(dataObj, "normalizedTranscript") ?: ""
        val intent = extractStringValue(dataObj, "intent") ?: "CREATE_TRIP"
        val status = extractStringValue(dataObj, "status") ?: "CREATED"
        val tripId = extractStringValue(dataObj, "tripId")
        val expiresAt = extractStringValue(dataObj, "expiresAt") ?: ""
        val createdAt = extractStringValue(dataObj, "createdAt") ?: ""
        val updatedAt = extractStringValue(dataObj, "updatedAt") ?: ""

        val originObj = extractJsonObject(dataObj, "origin") ?: ""
        val originQuery = extractStringValue(originObj, "query") ?: ""
        val originResolvedObj = extractJsonObject(originObj, "resolved") ?: ""
        val originResolved = parseResolvedLocation(originResolvedObj)

        val destinationObj = extractJsonObject(dataObj, "destination") ?: ""
        val destinationQuery = extractStringValue(destinationObj, "query") ?: ""
        val destinationResolvedObj = extractJsonObject(destinationObj, "resolved") ?: ""
        val destinationResolved = parseResolvedLocation(destinationResolvedObj)

        return VoiceTripDraftResponseDto(
            id = id,
            driverId = driverId,
            inputMode = inputMode,
            originalTranscript = originalTranscript,
            normalizedTranscript = normalizedTranscript,
            intent = intent,
            origin = VoiceTripDraftEndpointDto(query = originQuery, resolved = originResolved),
            destination = VoiceTripDraftEndpointDto(query = destinationQuery, resolved = destinationResolved),
            status = status,
            tripId = tripId,
            expiresAt = expiresAt,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun parseResolvedLocation(json: String): ResolvedLocationDto {
        return ResolvedLocationDto(
            latitude = extractDoubleValue(json, "latitude") ?: 0.0,
            longitude = extractDoubleValue(json, "longitude") ?: 0.0,
            formattedAddress = extractStringValue(json, "formattedAddress") ?: "",
            displayName = extractStringValue(json, "displayName"),
            provider = extractStringValue(json, "provider"),
            googlePlaceId = extractStringValue(json, "googlePlaceId"),
            serpApiDataId = extractStringValue(json, "serpApiDataId"),
            serpApiDataCid = extractStringValue(json, "serpApiDataCid"),
            city = extractStringValue(json, "city"),
            state = extractStringValue(json, "state"),
            country = extractStringValue(json, "country")
        )
    }

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
