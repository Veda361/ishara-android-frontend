package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.DriverOperationalReadinessResponseDto
import com.ishara.app.data.remote.dto.DriverReadinessAgencyDto
import com.ishara.app.data.remote.dto.DriverReadinessRequirementsDto
import com.ishara.app.data.remote.dto.DriverReadinessVehicleDto

/**
 * Remote data source for Driver Operational Readiness (Phase 07).
 */
interface DriverReadinessRemoteDataSource {
    suspend fun getOperationalReadiness(token: String): IshaaraResult<DriverOperationalReadinessResponseDto>
}

/**
 * Production implementation of [DriverReadinessRemoteDataSource].
 */
class DriverReadinessRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : DriverReadinessRemoteDataSource {

    override suspend fun getOperationalReadiness(token: String): IshaaraResult<DriverOperationalReadinessResponseDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/readiness",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )

        return httpClient.execute(request).map { response ->
            parseOperationalReadinessResponse(response.body)
        }
    }

    private fun parseOperationalReadinessResponse(json: String): DriverOperationalReadinessResponseDto {
        val dataObj = extractJsonObject(json, "data") ?: json

        val driverId = extractJsonString(dataObj, "driverId") ?: ""
        val userId = extractJsonString(dataObj, "userId") ?: ""
        val authorized = extractJsonBoolean(dataObj, "authorized") ?: false
        val status = extractJsonString(dataObj, "status") ?: "NOT_READY"
        val operatingType = extractJsonString(dataObj, "operatingType").orEmpty()

        val reasons = extractJsonStringArray(dataObj, "reasons")

        val reqObj = extractJsonObject(dataObj, "requirements")
        val requirements = DriverReadinessRequirementsDto(
            platformVerification = extractJsonBoolean(reqObj ?: "", "platformVerification") ?: false,
            agencyMembership = extractJsonBoolean(reqObj ?: "", "agencyMembership") ?: false,
            profileComplete = extractJsonBoolean(reqObj ?: "", "profileComplete") ?: false,
            notSuspended = extractJsonBoolean(reqObj ?: "", "notSuspended") ?: false,
            vehicleAssigned = extractJsonBoolean(reqObj ?: "", "vehicleAssigned") ?: false
        )

        val agencyObj = extractJsonObject(dataObj, "agency")
        val agency = if (agencyObj != null && agencyObj != "null") {
            DriverReadinessAgencyDto(
                membershipStatus = extractJsonString(agencyObj, "membershipStatus"),
                agencyId = extractJsonString(agencyObj, "agencyId"),
                agencyName = extractJsonString(agencyObj, "agencyName")
            )
        } else null

        val vehicleObj = extractJsonObject(dataObj, "activeVehicle")
        val activeVehicle = if (vehicleObj != null && vehicleObj != "null") {
            val vId = extractJsonString(vehicleObj, "id") ?: extractJsonString(vehicleObj, "_id") ?: ""
            val regNumber = extractJsonString(vehicleObj, "registrationNumber") ?: ""
            val make = extractJsonString(vehicleObj, "make")
            val model = extractJsonString(vehicleObj, "model")
            DriverReadinessVehicleDto(vId, regNumber, make, model)
        } else null

        return DriverOperationalReadinessResponseDto(
            driverId = driverId,
            userId = userId,
            authorized = authorized,
            status = status,
            reasons = reasons,
            requirements = requirements,
            operatingType = operatingType,
            agency = agency,
            activeVehicle = activeVehicle
        )
    }

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun extractJsonBoolean(json: String, key: String): Boolean? {
        val pattern = Regex("\"$key\"\\s*:\\s*(true|false)")
        return pattern.find(json)?.groupValues?.get(1)?.toBooleanStrictOrNull()
    }

    private fun extractJsonStringArray(json: String, key: String): List<String> {
        val arrayRegex = Regex("\"$key\"\\s*:\\s*\\[([^\\]]*)\\]", RegexOption.DOT_MATCHES_ALL)
        val arrayContent = arrayRegex.find(json)?.groupValues?.get(1) ?: return emptyList()
        val itemRegex = Regex("\"([^\"]*)\"")
        return itemRegex.findAll(arrayContent).map { it.groupValues[1] }.toList()
    }

    private fun extractJsonObject(json: String, key: String): String? {
        val keyIndex = json.indexOf("\"$key\"")
        if (keyIndex == -1) return null
        val colonIndex = json.indexOf(':', keyIndex)
        if (colonIndex == -1) return null

        // Check if value is null
        val afterColon = json.substring(colonIndex + 1).trimStart()
        if (afterColon.startsWith("null")) return null

        val braceIndex = json.indexOf('{', colonIndex)
        if (braceIndex == -1) return null
        val endIndex = findClosingChar(json, braceIndex, '{', '}')
        if (endIndex == -1) return null
        return json.substring(braceIndex, endIndex + 1)
    }

    private fun findClosingChar(text: String, startIndex: Int, openChar: Char, closeChar: Char): Int {
        var depth = 0
        var inQuotes = false
        var isEscaped = false

        for (i in startIndex until text.length) {
            val char = text[i]
            if (isEscaped) {
                isEscaped = false
                continue
            }
            if (char == '\\') {
                isEscaped = true
                continue
            }
            if (char == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (!inQuotes) {
                if (char == openChar) depth++
                if (char == closeChar) {
                    depth--
                    if (depth == 0) return i
                }
            }
        }
        return -1
    }
}
