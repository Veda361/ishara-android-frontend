package com.ishara.app.data.remote.datasource

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.FareEstimateDto
import com.ishara.app.data.remote.dto.FareSnapshotDto
import com.ishara.app.data.remote.dto.RideFareResponseDto

/**
 * Remote data source abstraction for Phase A12 — Fare & Authoritative Pricing.
 * Directly integrates backend endpoint: `GET /api/v1/rides/:rideId/fare`.
 */
interface FareRemoteDataSource {
    suspend fun getRideFare(
        rideId: String,
        token: String
    ): IshaaraResult<RideFareResponseDto>
}

class DefaultFareRemoteDataSource(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : FareRemoteDataSource {

    private val tag = "FareRemoteDataSource"

    override suspend fun getRideFare(
        rideId: String,
        token: String
    ): IshaaraResult<RideFareResponseDto> {
        val url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/fare"
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
                val response = result.data
                if (response.statusCode !in 200..299) {
                    return IshaaraResult.failure(mapHttpError(response.statusCode, response.body))
                }

                try {
                    val body = response.body
                    val dataJson = extractJsonObject(body, "data") ?: body

                    val rideIdValue = extractString(dataJson, "rideId") ?: rideId
                    val status = extractString(dataJson, "status") ?: "UNKNOWN"
                    val currency = extractString(dataJson, "currency") ?: "INR"
                    val currentFareMinor = extractLong(dataJson, "currentFareMinor") ?: 0L
                    val isFinal = extractBoolean(dataJson, "isFinal") ?: false

                    val estimate = parseFareEstimate(dataJson)
                    val snapshot = parseFareSnapshot(dataJson)

                    val dto = RideFareResponseDto(
                        rideId = rideIdValue,
                        status = status,
                        currency = currency,
                        currentFareMinor = currentFareMinor,
                        isFinal = isFinal,
                        fareEstimate = estimate,
                        fareSnapshot = snapshot
                    )
                    IshaaraResult.success(dto)
                } catch (e: Exception) {
                    IshaaraLogger.e(tag, "Failed to parse ride fare response", e)
                    IshaaraResult.failure(
                        IshaaraError.Validation(message = "Malformed fare response payload: ${e.message}")
                    )
                }
            }
            is IshaaraResult.Failure -> {
                IshaaraResult.failure(result.error)
            }
        }
    }

    private fun mapHttpError(statusCode: Int, body: String): IshaaraError {
        val message = extractString(body, "message")
        val code = extractString(body, "code")

        return when (statusCode) {
            400 -> IshaaraError.Validation(message = message ?: "Invalid ride ID format.", errorCode = code)
            401 -> IshaaraError.Authentication(message = message ?: "Authentication required to view fare.", errorCode = code)
            403 -> IshaaraError.Forbidden(message = message ?: "You do not have permission to view the fare for this ride.", errorCode = code)
            404 -> IshaaraError.NotFound(message = message ?: "Ride not found.")
            429 -> IshaaraError.Network("Rate limit exceeded. Please wait a moment before refreshing fare.")
            else -> IshaaraError.Server(statusCode, message ?: "Failed to retrieve ride fare.")
        }
    }

    private fun parseFareEstimate(json: String): FareEstimateDto? {
        val obj = extractJsonObject(json, "fareEstimate") ?: return null
        val currency = extractString(obj, "currency") ?: "INR"
        val policyVersion = extractString(obj, "pricingPolicyVersion") ?: "default"
        val distanceMeters = extractLong(obj, "distanceMeters") ?: 0L
        val estimatedDuration = extractLong(obj, "estimatedDurationSeconds")
        val baseFare = extractLong(obj, "baseFareMinor") ?: 0L
        val distFare = extractLong(obj, "distanceComponentMinor") ?: 0L
        val timeFare = extractLong(obj, "timeComponentMinor") ?: 0L
        val subtotal = extractLong(obj, "subtotalMinor") ?: (baseFare + distFare + timeFare)
        val serviceFee = extractLong(obj, "serviceFeeMinor") ?: 0L
        val tax = extractLong(obj, "taxMinor") ?: 0L
        val total = extractLong(obj, "totalMinor") ?: subtotal
        val providerAmount = extractLong(obj, "providerAmountMinor") ?: 0L
        val isEstimate = extractBoolean(obj, "isEstimate") ?: true
        val calculatedAt = extractString(obj, "calculatedAt") ?: ""

        return FareEstimateDto(
            currency = currency,
            pricingPolicyVersion = policyVersion,
            distanceMeters = distanceMeters,
            estimatedDurationSeconds = estimatedDuration,
            baseFareMinor = baseFare,
            distanceComponentMinor = distFare,
            timeComponentMinor = timeFare,
            subtotalMinor = subtotal,
            serviceFeeMinor = serviceFee,
            taxMinor = tax,
            totalMinor = total,
            providerAmountMinor = providerAmount,
            isEstimate = isEstimate,
            calculatedAt = calculatedAt
        )
    }

    private fun parseFareSnapshot(json: String): FareSnapshotDto? {
        val obj = extractJsonObject(json, "fareSnapshot") ?: return null
        val currency = extractString(obj, "currency") ?: "INR"
        val policyVersion = extractString(obj, "pricingPolicyVersion") ?: "default"
        val distanceMeters = extractLong(obj, "distanceMeters") ?: 0L
        val actualDuration = extractLong(obj, "actualDurationSeconds")
        val baseFare = extractLong(obj, "baseFareMinor") ?: 0L
        val distFare = extractLong(obj, "distanceComponentMinor") ?: 0L
        val timeFare = extractLong(obj, "timeComponentMinor") ?: 0L
        val subtotal = extractLong(obj, "subtotalMinor") ?: (baseFare + distFare + timeFare)
        val serviceFee = extractLong(obj, "serviceFeeMinor") ?: 0L
        val tax = extractLong(obj, "taxMinor") ?: 0L
        val discount = extractLong(obj, "discountMinor") ?: 0L
        val total = extractLong(obj, "totalMinor") ?: subtotal
        val providerAmount = extractLong(obj, "providerAmountMinor") ?: 0L
        val isEstimate = extractBoolean(obj, "isEstimate") ?: false
        val calculatedAt = extractString(obj, "calculatedAt") ?: ""

        return FareSnapshotDto(
            currency = currency,
            pricingPolicyVersion = policyVersion,
            distanceMeters = distanceMeters,
            actualDurationSeconds = actualDuration,
            baseFareMinor = baseFare,
            distanceComponentMinor = distFare,
            timeComponentMinor = timeFare,
            subtotalMinor = subtotal,
            serviceFeeMinor = serviceFee,
            taxMinor = tax,
            discountMinor = discount,
            totalMinor = total,
            providerAmountMinor = providerAmount,
            isEstimate = isEstimate,
            calculatedAt = calculatedAt
        )
    }

    private fun extractString(json: String, key: String): String? {
        val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return regex.find(json)?.groupValues?.get(1)
    }

    private fun extractLong(json: String, key: String): Long? {
        val regex = Regex("\"$key\"\\s*:\\s*(-?[0-9]+)")
        return regex.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun extractBoolean(json: String, key: String): Boolean? {
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

        var depth = 0
        var inString = false
        var escaped = false

        for (i in braceIndex until json.length) {
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
                if (c == '{') depth++
                else if (c == '}') {
                    depth--
                    if (depth == 0) return json.substring(braceIndex, i + 1)
                }
            }
        }
        return null
    }
}
