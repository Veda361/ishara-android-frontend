package com.ishara.app.data.remote.datasource

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.DriverRatingSummaryResponseDto
import com.ishara.app.data.remote.dto.RatingEligibilityResponseDto
import com.ishara.app.data.remote.dto.RatingResponseDto
import com.ishara.app.data.remote.dto.SubmitRatingRequestDto

/**
 * Remote data source for Ratings & Reviews.
 * Strictly adheres to verified backend routes:
 * - GET  /api/v1/rides/:rideId/rating-eligibility
 * - POST /api/v1/rides/:rideId/ratings
 * - GET  /api/v1/rides/:rideId/ratings
 * - GET  /api/v1/drivers/me/rating-summary
 */
interface RatingRemoteDataSource {

    suspend fun checkRatingEligibility(
        rideId: String,
        token: String?
    ): IshaaraResult<RatingEligibilityResponseDto>

    suspend fun submitRating(
        rideId: String,
        request: SubmitRatingRequestDto,
        idempotencyKey: String?,
        token: String?
    ): IshaaraResult<RatingResponseDto>

    suspend fun getRatingsByRide(
        rideId: String,
        token: String?
    ): IshaaraResult<List<RatingResponseDto>>

    suspend fun getMyRatingSummary(
        token: String?
    ): IshaaraResult<DriverRatingSummaryResponseDto>
}

class RatingRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : RatingRemoteDataSource {

    private val tag = "RatingRemoteDataSource"

    override suspend fun checkRatingEligibility(
        rideId: String,
        token: String?
    ): IshaaraResult<RatingEligibilityResponseDto> {
        val headers = mutableMapOf<String, String>()
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/rating-eligibility",
            method = HttpMethod.GET,
            headers = headers
        )

        return httpClient.execute(httpRequest).map { response ->
            parseRatingEligibilityResponse(response.body)
        }
    }

    override suspend fun submitRating(
        rideId: String,
        request: SubmitRatingRequestDto,
        idempotencyKey: String?,
        token: String?
    ): IshaaraResult<RatingResponseDto> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }
        if (!idempotencyKey.isNullOrBlank()) {
            headers["Idempotency-Key"] = idempotencyKey
        }

        val jsonBody = serializeSubmitRatingRequest(request)

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/ratings",
            method = HttpMethod.POST,
            headers = headers,
            body = jsonBody
        )

        return httpClient.execute(httpRequest).map { response ->
            parseRatingResponse(response.body)
        }
    }

    override suspend fun getRatingsByRide(
        rideId: String,
        token: String?
    ): IshaaraResult<List<RatingResponseDto>> {
        val headers = mutableMapOf<String, String>()
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/ratings",
            method = HttpMethod.GET,
            headers = headers
        )

        return httpClient.execute(httpRequest).map { response ->
            parseRatingListResponse(response.body)
        }
    }

    override suspend fun getMyRatingSummary(
        token: String?
    ): IshaaraResult<DriverRatingSummaryResponseDto> {
        val headers = mutableMapOf<String, String>()
        if (!token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/rating-summary",
            method = HttpMethod.GET,
            headers = headers
        )

        return httpClient.execute(httpRequest).map { response ->
            parseDriverRatingSummaryResponse(response.body)
        }
    }

    // ── Serialization Helpers ──

    fun serializeSubmitRatingRequest(dto: SubmitRatingRequestDto): String {
        val parts = mutableListOf<String>()
        parts.add(""""score":${dto.score}""")
        if (dto.review != null && dto.review.isNotBlank()) {
            parts.add(""""review":"${escapeJson(dto.review.trim())}"""")
        }
        return "{${parts.joinToString(",")}}"
    }

    // ── Parsing Helpers ──

    fun parseRatingEligibilityResponse(json: String): RatingEligibilityResponseDto {
        val dataObj = extractJsonObject(json, "data") ?: json
        val eligible = extractBooleanValue(dataObj, "eligible") ?: false
        val alreadyRated = extractBooleanValue(dataObj, "alreadyRated") ?: false
        val reason = extractStringValue(dataObj, "reason")
        return RatingEligibilityResponseDto(
            eligible = eligible,
            alreadyRated = alreadyRated,
            reason = reason
        )
    }

    fun parseRatingResponse(json: String): RatingResponseDto {
        val dataObj = extractJsonObject(json, "data") ?: json
        return parseSingleRatingObject(dataObj)
    }

    fun parseRatingListResponse(json: String): List<RatingResponseDto> {
        val dataArrayStr = extractJsonArray(json, "data") ?: json
        val itemObjects = splitJsonObjectsInArray(dataArrayStr)
        return itemObjects.mapNotNull {
            try {
                parseSingleRatingObject(it)
            } catch (e: Exception) {
                IshaaraLogger.w(tag, "Failed to parse rating in list", e)
                null
            }
        }
    }

    fun parseDriverRatingSummaryResponse(json: String): DriverRatingSummaryResponseDto {
        val dataObj = extractJsonObject(json, "data") ?: json
        val driverId = extractStringValue(dataObj, "driverId") ?: ""
        val averageScore = extractDoubleValue(dataObj, "averageScore")
        val ratingCount = extractIntValue(dataObj, "ratingCount") ?: 0
        return DriverRatingSummaryResponseDto(
            driverId = driverId,
            averageScore = averageScore,
            ratingCount = ratingCount
        )
    }

    private fun parseSingleRatingObject(obj: String): RatingResponseDto {
        val id = extractStringValue(obj, "id") ?: extractStringValue(obj, "_id") ?: ""
        val rideId = extractStringValue(obj, "rideId") ?: ""
        val score = extractIntValue(obj, "score") ?: 0
        val review = extractStringValue(obj, "review")
        val createdAt = extractStringValue(obj, "createdAt") ?: ""
        return RatingResponseDto(
            id = id,
            rideId = rideId,
            score = score,
            review = review,
            createdAt = createdAt
        )
    }

    // ── Pure Kotlin JSON String Extraction Utilities ──

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

    private fun extractIntValue(json: String, key: String): Int? {
        val pattern = "\"$key\"\\s*:\\s*([0-9-]+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toIntOrNull()
    }

    private fun extractDoubleValue(json: String, key: String): Double? {
        val pattern = "\"$key\"\\s*:\\s*([0-9.-]+)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun extractBooleanValue(json: String, key: String): Boolean? {
        val pattern = "\"$key\"\\s*:\\s*(true|false)".toRegex()
        return pattern.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }

    private fun extractJsonObject(json: String, key: String): String? {
        val keyPattern = "\"$key\"\\s*:\\s*\\{".toRegex()
        val match = keyPattern.find(json) ?: return null
        val startIndex = match.range.last

        var braceCount = 1
        var inQuotes = false
        var escape = false

        for (i in (startIndex + 1) until json.length) {
            val char = json[i]
            if (escape) {
                escape = false
                continue
            }
            if (char == '\\') {
                escape = true
                continue
            }
            if (char == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (!inQuotes) {
                if (char == '{') braceCount++
                if (char == '}') {
                    braceCount--
                    if (braceCount == 0) {
                        return json.substring(startIndex, i + 1)
                    }
                }
            }
        }
        return null
    }

    private fun extractJsonArray(json: String, key: String): String? {
        val keyPattern = "\"$key\"\\s*:\\s*\\[".toRegex()
        val match = keyPattern.find(json) ?: return null
        val startIndex = match.range.last

        var bracketCount = 1
        var inQuotes = false
        var escape = false

        for (i in (startIndex + 1) until json.length) {
            val char = json[i]
            if (escape) {
                escape = false
                continue
            }
            if (char == '\\') {
                escape = true
                continue
            }
            if (char == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (!inQuotes) {
                if (char == '[') bracketCount++
                if (char == ']') {
                    bracketCount--
                    if (bracketCount == 0) {
                        return json.substring(startIndex, i + 1)
                    }
                }
            }
        }
        return null
    }

    private fun splitJsonObjectsInArray(arrayJson: String): List<String> {
        val trimmed = arrayJson.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) return emptyList()
        val inner = trimmed.substring(1, trimmed.length - 1).trim()
        if (inner.isEmpty()) return emptyList()

        val results = mutableListOf<String>()
        var braceCount = 0
        var startIndex = -1
        var inQuotes = false
        var escape = false

        for (i in inner.indices) {
            val char = inner[i]
            if (escape) {
                escape = false
                continue
            }
            if (char == '\\') {
                escape = true
                continue
            }
            if (char == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (!inQuotes) {
                if (char == '{') {
                    if (braceCount == 0) startIndex = i
                    braceCount++
                } else if (char == '}') {
                    braceCount--
                    if (braceCount == 0 && startIndex != -1) {
                        results.add(inner.substring(startIndex, i + 1))
                        startIndex = -1
                    }
                }
            }
        }
        return results
    }
}
