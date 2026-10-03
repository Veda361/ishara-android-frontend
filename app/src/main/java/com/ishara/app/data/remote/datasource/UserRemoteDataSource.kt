package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.UserDto

/**
 * Remote data source for User and Onboarding API endpoints.
 * Interacts with:
 * - GET /api/v1/users/me
 * - POST /api/v1/users/me/onboarding
 * - PATCH /api/v1/users/me
 */
interface UserRemoteDataSource {
    suspend fun getCurrentUserProfile(token: String): IshaaraResult<UserDto>
    suspend fun completeOnboarding(role: String, token: String): IshaaraResult<UserDto>
    suspend fun updateUserProfile(
        name: String?,
        phoneNumber: String?,
        image: String?,
        token: String
    ): IshaaraResult<UserDto>
}

class UserRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : UserRemoteDataSource {

    override suspend fun getCurrentUserProfile(token: String): IshaaraResult<UserDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/users/me",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { response ->
            parseUserDto(response.body)
        }
    }

    override suspend fun completeOnboarding(role: String, token: String): IshaaraResult<UserDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/users/me/onboarding",
            method = HttpMethod.POST,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer $token"
            ),
            body = "{\"role\":\"$role\"}"
        )
        return httpClient.execute(request).map { response ->
            parseUserDto(response.body).copy(role = role, isOnboarded = true)
        }
    }

    override suspend fun updateUserProfile(
        name: String?,
        phoneNumber: String?,
        image: String?,
        token: String
    ): IshaaraResult<UserDto> {
        val bodyFields = mutableListOf<String>()
        if (name != null) bodyFields.add("\"name\":\"$name\"")
        if (phoneNumber != null) bodyFields.add("\"phoneNumber\":\"$phoneNumber\"")
        if (image != null) bodyFields.add("\"image\":\"$image\"")

        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/users/me",
            method = HttpMethod.PATCH,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer $token"
            ),
            body = "{${bodyFields.joinToString(",")}}"
        )
        return httpClient.execute(request).map { response ->
            parseUserDto(response.body)
        }
    }

    private fun parseUserDto(json: String): UserDto {
        val id = extractJsonField(json, "id") ?: extractJsonField(json, "userId") ?: ""
        val name = extractJsonField(json, "name") ?: "User"
        val email = extractJsonField(json, "email")
        val phoneNumber = extractJsonField(json, "phoneNumber")
        val rawRole = extractJsonField(json, "role")
        val role = if (rawRole.isNullOrBlank() || rawRole == "null") null else rawRole
        val image = extractJsonField(json, "image")
        val isOnboarded = extractJsonField(json, "onboardingCompleted")?.toBooleanStrictOrNull()
            ?: extractJsonField(json, "isOnboarded")?.toBooleanStrictOrNull()
            ?: (!role.isNullOrBlank())

        return UserDto(
            id = id,
            name = name,
            email = email,
            phoneNumber = phoneNumber,
            role = role,
            image = image,
            isOnboarded = isOnboarded,
            onboardingCompleted = isOnboarded
        )
    }

    private fun extractJsonField(json: String, field: String): String? {
        val pattern = Regex("\"$field\"\\s*:\\s*\"?([^,\"}]+)\"?")
        return pattern.find(json)?.groupValues?.get(1)?.trim()
    }
}
