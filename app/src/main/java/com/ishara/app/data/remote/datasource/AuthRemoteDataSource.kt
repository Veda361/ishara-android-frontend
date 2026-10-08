package com.ishara.app.data.remote.datasource

import android.util.Log
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.*
import kotlinx.serialization.json.Json

interface AuthRemoteDataSource {
    suspend fun signInWithSocial(provider: String, idToken: String): IshaaraResult<AuthSessionResponseDto>
    suspend fun sendEmailOtp(email: String): IshaaraResult<Unit>
    suspend fun signInWithEmailOtp(email: String, otp: String): IshaaraResult<AuthSessionResponseDto>
    suspend fun signOut(token: String): IshaaraResult<Unit>
    suspend fun getSession(token: String): IshaaraResult<SessionResponseDto>
    suspend fun checkHealth(): IshaaraResult<Unit>
    suspend fun completeOnboarding(token: String, request: OnboardingRequestDto): IshaaraResult<ApiResponse<UserDto>>
    suspend fun getCurrentUser(token: String): IshaaraResult<ApiResponse<UserDto>>
    suspend fun updateProfile(token: String, request: UpdateUserRequestDto): IshaaraResult<ApiResponse<UserDto>>
}

class AuthRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : AuthRemoteDataSource {

    override suspend fun signInWithSocial(provider: String, idToken: String): IshaaraResult<AuthSessionResponseDto> {
        val body = json.encodeToString(GoogleSignInRequestDto.serializer(), GoogleSignInRequestDto(provider, idToken))
        val url = "${networkConfig.authBaseUrl}/sign-in/social"
        
        Log.d("AUTH_DEBUG", "AuthRemoteDataSource: POST $url")
        
        val request = HttpRequest(
            url = url,
            method = HttpMethod.POST,
            headers = mapOf("Content-Type" to "application/json"),
            body = body
        )
        return httpClient.execute(request).flatMap { response ->
            try {
                val dto = json.decodeFromString<AuthSessionResponseDto>(response.body)
                IshaaraResult.Success(dto)
            } catch (e: Exception) {
                Log.e("AUTH_DEBUG", "AuthRemoteDataSource: Failed to parse signInWithSocial response. Body: ${response.body}", e)
                IshaaraResult.Failure(IshaaraError.Unknown("Response parsing error", e))
            }
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
        return httpClient.execute(request).flatMap { response ->
            try {
                IshaaraResult.Success(json.decodeFromString<AuthSessionResponseDto>(response.body))
            } catch (e: Exception) {
                IshaaraResult.Failure(IshaaraError.Unknown("Response parsing error", e))
            }
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
        val url = "${networkConfig.authBaseUrl}/get-session"
        val request = HttpRequest(
            url = url,
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).flatMap { response ->
            try {
                IshaaraResult.Success(json.decodeFromString<SessionResponseDto>(response.body))
            } catch (e: Exception) {
                Log.e("AUTH_DEBUG", "AuthRemoteDataSource: Failed to parse getSession response. Body: ${response.body}", e)
                IshaaraResult.Failure(IshaaraError.Unknown("Response parsing error", e))
            }
        }
    }

    override suspend fun checkHealth(): IshaaraResult<Unit> {
        val request = HttpRequest(
            url = "${networkConfig.authBaseUrl}/ok",
            method = HttpMethod.GET
        )
        return httpClient.execute(request).map { }
    }

    override suspend fun completeOnboarding(token: String, request: OnboardingRequestDto): IshaaraResult<ApiResponse<UserDto>> {
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
        return httpClient.execute(httpRequest).flatMap { response ->
            try {
                IshaaraResult.Success(json.decodeFromString<ApiResponse<UserDto>>(response.body))
            } catch (e: Exception) {
                IshaaraResult.Failure(IshaaraError.Unknown("Response parsing error", e))
            }
        }
    }

    override suspend fun getCurrentUser(token: String): IshaaraResult<ApiResponse<UserDto>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/users/me",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).flatMap { response ->
            try {
                IshaaraResult.Success(json.decodeFromString<ApiResponse<UserDto>>(response.body))
            } catch (e: Exception) {
                IshaaraResult.Failure(IshaaraError.Unknown("Response parsing error", e))
            }
        }
    }

    override suspend fun updateProfile(token: String, request: UpdateUserRequestDto): IshaaraResult<ApiResponse<UserDto>> {
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
        return httpClient.execute(httpRequest).flatMap { response ->
            try {
                IshaaraResult.Success(json.decodeFromString<ApiResponse<UserDto>>(response.body))
            } catch (e: Exception) {
                IshaaraResult.Failure(IshaaraError.Unknown("Response parsing error", e))
            }
        }
    }
}
