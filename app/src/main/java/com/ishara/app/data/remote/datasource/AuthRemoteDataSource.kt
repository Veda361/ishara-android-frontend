package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.*
import kotlinx.serialization.json.Json

interface AuthRemoteDataSource {
    suspend fun signInWithSocial(provider: String, idToken: String): IshaaraResult<AuthSessionResponseDto>
    suspend fun sendEmailOtp(email: String): IshaaraResult<Unit>
    suspend fun signInWithEmailOtp(email: String, otp: String): IshaaraResult<AuthSessionResponseDto>
    suspend fun signOut(token: String): IshaaraResult<Unit>
    suspend fun getSession(token: String): IshaaraResult<SessionResponseDto>
    suspend fun completeOnboarding(token: String, request: OnboardingRequestDto): IshaaraResult<UserDto>
    suspend fun getCurrentUser(token: String): IshaaraResult<ApiResponse<UserDto>>
    suspend fun updateProfile(token: String, request: UpdateUserRequestDto): IshaaraResult<UserDto>
}

class AuthRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : AuthRemoteDataSource {

    override suspend fun signInWithSocial(provider: String, idToken: String): IshaaraResult<AuthSessionResponseDto> {
        val body = json.encodeToString(GoogleSignInRequestDto.serializer(), GoogleSignInRequestDto(provider, idToken))
        val request = HttpRequest(
            url = "${networkConfig.authBaseUrl}/sign-in/social",
            method = HttpMethod.POST,
            headers = mapOf("Content-Type" to "application/json"),
            body = body
        )
        return httpClient.execute(request).map { response ->
            json.decodeFromString<AuthSessionResponseDto>(response.body)
        }
    }

    override suspend fun sendEmailOtp(email: String): IshaaraResult<Unit> {
        val body = json.encodeToString(EmailOtpSendRequestDto.serializer(), EmailOtpSendRequestDto(email))
        val request = HttpRequest(
            url = "${networkConfig.authBaseUrl}/email-otp/send-verification-otp",
            method = HttpMethod.POST,
            headers = mapOf("Content-Type" to "application/json"),
            body = body
        )
        return httpClient.execute(request).map { }
    }

    override suspend fun signInWithEmailOtp(email: String, otp: String): IshaaraResult<AuthSessionResponseDto> {
        val body = json.encodeToString(EmailOtpSignInRequestDto.serializer(), EmailOtpSignInRequestDto(email, otp))
        val request = HttpRequest(
            url = "${networkConfig.authBaseUrl}/sign-in/email-otp",
            method = HttpMethod.POST,
            headers = mapOf("Content-Type" to "application/json"),
            body = body
        )
        return httpClient.execute(request).map { response ->
            json.decodeFromString<AuthSessionResponseDto>(response.body)
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

    override suspend fun getSession(token: String): IshaaraResult<SessionResponseDto> {
        val request = HttpRequest(
            url = "${networkConfig.authBaseUrl}/get-session",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { response ->
            json.decodeFromString<SessionResponseDto>(response.body)
        }
    }

    override suspend fun completeOnboarding(token: String, request: OnboardingRequestDto): IshaaraResult<UserDto> {
        val body = json.encodeToString(OnboardingRequestDto.serializer(), request)
        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/users/me/onboarding",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = body
        )
        return httpClient.execute(httpRequest).map { response ->
            json.decodeFromString<UserDto>(response.body)
        }
    }

    override suspend fun getCurrentUser(token: String): IshaaraResult<ApiResponse<UserDto>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/users/me",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { response ->
            json.decodeFromString<ApiResponse<UserDto>>(response.body)
        }
    }

    override suspend fun updateProfile(token: String, request: UpdateUserRequestDto): IshaaraResult<UserDto> {
        val body = json.encodeToString(UpdateUserRequestDto.serializer(), request)
        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/users/me",
            method = HttpMethod.PATCH,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = body
        )
        return httpClient.execute(httpRequest).map { response ->
            json.decodeFromString<UserDto>(response.body)
        }
    }
}
