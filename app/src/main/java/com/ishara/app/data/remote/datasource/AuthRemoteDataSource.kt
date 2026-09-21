package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.AuthSessionResponseDto
import com.ishara.app.data.remote.dto.UserDto

interface AuthRemoteDataSource {
    suspend fun signInWithGoogle(idToken: String): IshaaraResult<AuthSessionResponseDto>
    suspend fun completeOnboarding(role: String, token: String): IshaaraResult<UserDto>
    suspend fun getCurrentUser(token: String): IshaaraResult<UserDto>
    suspend fun signOut(token: String): IshaaraResult<Unit>
}

class AuthRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : AuthRemoteDataSource {

    override suspend fun signInWithGoogle(idToken: String): IshaaraResult<AuthSessionResponseDto> {
        val request = HttpRequest(
            url = "${networkConfig.authBaseUrl}/sign-in/social",
            method = HttpMethod.POST,
            headers = mapOf("Content-Type" to "application/json"),
            body = "{\"provider\":\"google\",\"idToken\":\"$idToken\"}"
        )
        return httpClient.execute(request).map { response ->
            // In a complete parser, this deserializes response.body into AuthSessionResponseDto
            // Standard safe fallback parsing:
            AuthSessionResponseDto(
                token = extractJsonField(response.body, "token") ?: "",
                userId = extractJsonField(response.body, "userId") ?: "",
                role = extractJsonField(response.body, "role") ?: "USER"
            )
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
            UserDto(
                id = extractJsonField(response.body, "id") ?: "",
                name = extractJsonField(response.body, "name") ?: "User",
                role = role,
                isOnboarded = true
            )
        }
    }

    override suspend fun getCurrentUser(token: String): IshaaraResult<UserDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/users/me",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { response ->
            UserDto(
                id = extractJsonField(response.body, "id") ?: "",
                name = extractJsonField(response.body, "name") ?: "User",
                email = extractJsonField(response.body, "email"),
                role = extractJsonField(response.body, "role") ?: "USER",
                isOnboarded = true
            )
        }
    }

    override suspend fun signOut(token: String): IshaaraResult<Unit> {
        val request = HttpRequest(
            url = "${networkConfig.authBaseUrl}/sign-out",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { }
    }

    private fun extractJsonField(json: String, field: String): String? {
        val pattern = Regex("\"$field\"\\s*:\\s*\"([^\"]+)\"")
        return pattern.find(json)?.groupValues?.get(1)
    }
}
