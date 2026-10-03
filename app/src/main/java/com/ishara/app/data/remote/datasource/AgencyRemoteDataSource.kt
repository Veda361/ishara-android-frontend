package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.CancelMembershipResponseDto
import com.ishara.app.data.remote.dto.CleanDriverMembershipResponseDto
import com.ishara.app.data.remote.dto.CleanPublicAgencyResponseDto
import java.net.URLEncoder

/**
 * Remote data source interface for agency discovery and driver agency memberships.
 */
interface AgencyRemoteDataSource {
    suspend fun listAgencies(
        search: String? = null,
        city: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): IshaaraResult<List<CleanPublicAgencyResponseDto>>

    suspend fun getAgencyById(agencyId: String): IshaaraResult<CleanPublicAgencyResponseDto>

    suspend fun getCurrentDriverMembership(token: String): IshaaraResult<CleanDriverMembershipResponseDto?>

    suspend fun listDriverMemberships(
        token: String,
        status: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): IshaaraResult<List<CleanDriverMembershipResponseDto>>

    suspend fun requestMembership(
        agencyId: String,
        notes: String?,
        token: String
    ): IshaaraResult<CleanDriverMembershipResponseDto>

    suspend fun cancelMembership(
        membershipId: String,
        token: String
    ): IshaaraResult<CancelMembershipResponseDto>
}

/**
 * Production implementation of AgencyRemoteDataSource.
 */
class AgencyRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : AgencyRemoteDataSource {

    override suspend fun listAgencies(
        search: String?,
        city: String?,
        page: Int,
        limit: Int
    ): IshaaraResult<List<CleanPublicAgencyResponseDto>> {
        val queryParams = mutableListOf<String>()
        if (!search.isNullOrBlank()) {
            queryParams.add("search=${URLEncoder.encode(search.trim(), "UTF-8")}")
        }
        if (!city.isNullOrBlank()) {
            queryParams.add("city=${URLEncoder.encode(city.trim(), "UTF-8")}")
        }
        queryParams.add("page=$page")
        queryParams.add("limit=$limit")

        val url = "${networkConfig.fullApiBaseUrl}/agencies?${queryParams.joinToString("&")}"
        val request = HttpRequest(url = url, method = HttpMethod.GET)

        return httpClient.execute(request).map { response ->
            parsePublicAgenciesList(response.body)
        }
    }

    override suspend fun getAgencyById(agencyId: String): IshaaraResult<CleanPublicAgencyResponseDto> {
        val url = "${networkConfig.fullApiBaseUrl}/agencies/${URLEncoder.encode(agencyId, "UTF-8")}"
        val request = HttpRequest(url = url, method = HttpMethod.GET)

        return httpClient.execute(request).map { response ->
            parseSingleAgency(response.body)
        }
    }

    override suspend fun getCurrentDriverMembership(token: String): IshaaraResult<CleanDriverMembershipResponseDto?> {
        val url = "${networkConfig.fullApiBaseUrl}/drivers/me/memberships/current"
        val request = HttpRequest(
            url = url,
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )

        return httpClient.execute(request).map { response ->
            parseCurrentDriverMembership(response.body)
        }
    }

    override suspend fun listDriverMemberships(
        token: String,
        status: String?,
        page: Int,
        limit: Int
    ): IshaaraResult<List<CleanDriverMembershipResponseDto>> {
        val queryParams = mutableListOf<String>()
        if (!status.isNullOrBlank()) {
            queryParams.add("status=${URLEncoder.encode(status.trim(), "UTF-8")}")
        }
        queryParams.add("page=$page")
        queryParams.add("limit=$limit")

        val url = "${networkConfig.fullApiBaseUrl}/drivers/me/memberships?${queryParams.joinToString("&")}"
        val request = HttpRequest(
            url = url,
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )

        return httpClient.execute(request).map { response ->
            parseDriverMembershipsList(response.body)
        }
    }

    override suspend fun requestMembership(
        agencyId: String,
        notes: String?,
        token: String
    ): IshaaraResult<CleanDriverMembershipResponseDto> {
        val fields = mutableListOf<String>()
        fields.add("\"agencyId\": \"${escapeJson(agencyId.trim())}\"")
        if (!notes.isNullOrBlank()) {
            fields.add("\"notes\": \"${escapeJson(notes.trim())}\"")
        }
        val jsonBody = "{${fields.joinToString(", ")}}"

        val url = "${networkConfig.fullApiBaseUrl}/drivers/me/memberships"
        val request = HttpRequest(
            url = url,
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = jsonBody
        )

        return httpClient.execute(request).map { response ->
            parseSingleDriverMembership(response.body)
        }
    }

    override suspend fun cancelMembership(
        membershipId: String,
        token: String
    ): IshaaraResult<CancelMembershipResponseDto> {
        val url = "${networkConfig.fullApiBaseUrl}/drivers/me/memberships/${URLEncoder.encode(membershipId, "UTF-8")}"
        val request = HttpRequest(
            url = url,
            method = HttpMethod.DELETE,
            headers = mapOf("Authorization" to "Bearer $token")
        )

        return httpClient.execute(request).map { response ->
            parseCancelMembershipResponse(response.body)
        }
    }

    // =========================================================================
    // JSON Parsing Helpers
    // =========================================================================

    private fun parseCurrentDriverMembership(json: String): CleanDriverMembershipResponseDto? {
        val dataMatch = Regex("\"data\"\\s*:\\s*(null|\\{[^}]*\\})", RegexOption.DOT_MATCHES_ALL).find(json)
        val dataContent = dataMatch?.groupValues?.get(1)?.trim() ?: return null
        if (dataContent == "null") return null

        return parseSingleDriverMembership(json)
    }

    private fun parseSingleDriverMembership(json: String): CleanDriverMembershipResponseDto {
        val id = extractJsonString(json, "id") ?: ""
        val agencyId = extractJsonString(json, "agencyId") ?: ""
        val agencyName = extractJsonString(json, "agencyName") ?: ""
        val agencyCity = extractJsonString(json, "agencyCity")
        val agencyState = extractJsonString(json, "agencyState")
        val agencyContactEmail = extractJsonString(json, "agencyContactEmail")
        val status = extractJsonString(json, "status") ?: "PENDING"
        val requestedAt = extractJsonString(json, "requestedAt")
        val respondedAt = extractJsonString(json, "respondedAt")
        val rejectionReason = extractJsonString(json, "rejectionReason")
        val notes = extractJsonString(json, "notes")
        val createdAt = extractJsonString(json, "createdAt")
        val updatedAt = extractJsonString(json, "updatedAt")

        return CleanDriverMembershipResponseDto(
            id = id,
            agencyId = agencyId,
            agencyName = agencyName,
            agencyCity = agencyCity,
            agencyState = agencyState,
            agencyContactEmail = agencyContactEmail,
            status = status,
            requestedAt = requestedAt,
            respondedAt = respondedAt,
            rejectionReason = rejectionReason,
            notes = notes,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun parseDriverMembershipsList(json: String): List<CleanDriverMembershipResponseDto> {
        val itemsRegex = Regex("\"items\"\\s*:\\s*\\[(.*?)\\]", RegexOption.DOT_MATCHES_ALL)
        val itemsContent = itemsRegex.find(json)?.groupValues?.get(1) ?: return emptyList()

        val objectRegex = Regex("\\{[^{}]*\\}")
        return objectRegex.findAll(itemsContent).map { match ->
            val obj = match.value
            CleanDriverMembershipResponseDto(
                id = extractJsonString(obj, "id") ?: "",
                agencyId = extractJsonString(obj, "agencyId") ?: "",
                agencyName = extractJsonString(obj, "agencyName") ?: "",
                agencyCity = extractJsonString(obj, "agencyCity"),
                agencyState = extractJsonString(obj, "agencyState"),
                agencyContactEmail = extractJsonString(obj, "agencyContactEmail"),
                status = extractJsonString(obj, "status") ?: "PENDING",
                requestedAt = extractJsonString(obj, "requestedAt"),
                respondedAt = extractJsonString(obj, "respondedAt"),
                rejectionReason = extractJsonString(obj, "rejectionReason"),
                notes = extractJsonString(obj, "notes"),
                createdAt = extractJsonString(obj, "createdAt"),
                updatedAt = extractJsonString(obj, "updatedAt")
            )
        }.toList()
    }

    private fun parseSingleAgency(json: String): CleanPublicAgencyResponseDto {
        return CleanPublicAgencyResponseDto(
            id = extractJsonString(json, "id") ?: "",
            name = extractJsonString(json, "name") ?: "",
            businessName = extractJsonString(json, "businessName"),
            city = extractJsonString(json, "city"),
            state = extractJsonString(json, "state"),
            contactPhoneMasked = extractJsonString(json, "contactPhoneMasked"),
            contactEmail = extractJsonString(json, "contactEmail"),
            status = extractJsonString(json, "status") ?: "ACTIVE",
            createdAt = extractJsonString(json, "createdAt")
        )
    }

    private fun parsePublicAgenciesList(json: String): List<CleanPublicAgencyResponseDto> {
        val itemsRegex = Regex("\"items\"\\s*:\\s*\\[(.*?)\\]", RegexOption.DOT_MATCHES_ALL)
        val itemsContent = itemsRegex.find(json)?.groupValues?.get(1) ?: return emptyList()

        val objectRegex = Regex("\\{[^{}]*\\}")
        return objectRegex.findAll(itemsContent).map { match ->
            val obj = match.value
            CleanPublicAgencyResponseDto(
                id = extractJsonString(obj, "id") ?: "",
                name = extractJsonString(obj, "name") ?: "",
                businessName = extractJsonString(obj, "businessName"),
                city = extractJsonString(obj, "city"),
                state = extractJsonString(obj, "state"),
                contactPhoneMasked = extractJsonString(obj, "contactPhoneMasked"),
                contactEmail = extractJsonString(obj, "contactEmail"),
                status = extractJsonString(obj, "status") ?: "ACTIVE",
                createdAt = extractJsonString(obj, "createdAt")
            )
        }.toList()
    }

    private fun parseCancelMembershipResponse(json: String): CancelMembershipResponseDto {
        val message = extractJsonString(json, "message") ?: "Membership request cancelled."
        val cancelledMembershipId = extractJsonString(json, "cancelledMembershipId") ?: ""
        return CancelMembershipResponseDto(message, cancelledMembershipId)
    }

    private fun extractJsonString(json: String, key: String): String? {
        val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun escapeJson(input: String): String {
        return input.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
