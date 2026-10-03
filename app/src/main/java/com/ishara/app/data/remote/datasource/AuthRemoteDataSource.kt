package com.ishara.app.data.remote.datasource

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.AuthSessionResponseDto
import com.ishara.app.data.remote.dto.UserDto

interface AuthRemoteDataSource {
    suspend fun signInWithGoogle(idToken: String): IshaaraResult<AuthSessionResponseDto>
    suspend fun sendVerificationOtp(email: String): IshaaraResult<Unit>
    suspend fun signInWithEmailOtp(email: String, otp: String): IshaaraResult<AuthSessionResponseDto>
    suspend fun getCurrentSession(token: String): IshaaraResult<AuthSessionResponseDto>
    suspend fun completeOnboarding(role: String, token: String): IshaaraResult<UserDto>
    suspend fun getCurrentUser(token: String): IshaaraResult<UserDto>
    suspend fun signOut(token: String): IshaaraResult<Unit>
}

class AuthRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : AuthRemoteDataSource {

    override suspend fun signInWithGoogle(idToken: String): IshaaraResult<AuthSessionResponseDto> {
        IshaaraLogger.d(TAG, "backend request started")
        val escapedToken = escapeJson(idToken)
        val body = """{"provider":"google","idToken":{"token":"$escapedToken"}}"""
        val request = HttpRequest(
            url = "${networkConfig.authBaseUrl}/sign-in/social",
            method = HttpMethod.POST,
            headers = mapOf("Content-Type" to "application/json"),
            body = body
        )
        return when (val httpResult = httpClient.execute(request)) {
            is IshaaraResult.Success -> {
                val response = httpResult.data
                IshaaraLogger.d(TAG, "backend HTTP status: ${response.statusCode}")
                val sessionDto = parseSessionResponse(response.body)
                if (sessionDto.token.isNotBlank()) {
                    IshaaraLogger.i(TAG, "backend authentication succeeded")
                    IshaaraResult.success(sessionDto)
                } else {
                    IshaaraLogger.w(TAG, "backend authentication failed: no session token in response")
                    IshaaraResult.failure(
                        IshaaraError.Authentication(
                            code = response.statusCode,
                            message = "No session token returned from authentication provider."
                        )
                    )
                }
            }
            is IshaaraResult.Failure -> {
                val error = httpResult.error
                IshaaraLogger.w(TAG, "backend authentication failed")
                IshaaraLogger.w(TAG, "sanitized error message: ${error.message}")
                IshaaraResult.failure(error)
            }
        }
    }

    override suspend fun sendVerificationOtp(email: String): IshaaraResult<Unit> {
        IshaaraLogger.d(TAG, "[OTP] send request started")
        IshaaraLogger.d(TAG, "[OTP] endpoint=/api/auth/email-otp/send-verification-otp")
        IshaaraLogger.d(TAG, "[OTP] email=<REDACTED>")
        val escapedEmail = escapeJson(email.trim().lowercase())
        val body = """{"email":"$escapedEmail","type":"sign-in"}"""
        val request = HttpRequest(
            url = "${networkConfig.authBaseUrl}/email-otp/send-verification-otp",
            method = HttpMethod.POST,
            headers = mapOf("Content-Type" to "application/json"),
            body = body
        )
        return when (val httpResult = httpClient.execute(request)) {
            is IshaaraResult.Success -> {
                IshaaraLogger.d(TAG, "[OTP] responseCode=${httpResult.data.statusCode}")
                IshaaraLogger.d(TAG, "[OTP] responseBody=${httpResult.data.body}")
                IshaaraResult.success(Unit)
            }
            is IshaaraResult.Failure -> {
                IshaaraLogger.w(TAG, "[OTP] send verification failed: ${httpResult.error.message}")
                IshaaraResult.failure(httpResult.error)
            }
        }
    }

    override suspend fun signInWithEmailOtp(email: String, otp: String): IshaaraResult<AuthSessionResponseDto> {
        IshaaraLogger.d(TAG, "[OTP] verify request started")
        IshaaraLogger.d(TAG, "[OTP] endpoint=/api/auth/sign-in/email-otp")
        IshaaraLogger.d(TAG, "[OTP] email=<REDACTED>")
        val escapedEmail = escapeJson(email.trim().lowercase())
        val escapedOtp = escapeJson(otp.trim())
        val body = """{"email":"$escapedEmail","otp":"$escapedOtp"}"""
        val request = HttpRequest(
            url = "${networkConfig.authBaseUrl}/sign-in/email-otp",
            method = HttpMethod.POST,
            headers = mapOf("Content-Type" to "application/json"),
            body = body
        )
        return when (val httpResult = httpClient.execute(request)) {
            is IshaaraResult.Success -> {
                IshaaraLogger.d(TAG, "[OTP] responseCode=${httpResult.data.statusCode}")
                var sessionDto = parseSessionResponse(httpResult.data.body)
                if (sessionDto.token.isBlank()) {
                    val cookieHeader = httpResult.data.headers["set-cookie"]
                    val cookieToken = extractCookieToken(cookieHeader)
                    if (!cookieToken.isNullOrBlank()) {
                        sessionDto = sessionDto.copy(token = cookieToken)
                    }
                }
                if (sessionDto.token.isNotBlank()) {
                    IshaaraLogger.i(TAG, "[OTP] verify request succeeded, session established")
                    IshaaraResult.success(sessionDto)
                } else {
                    IshaaraLogger.w(TAG, "[OTP] verify request response missing session token")
                    IshaaraResult.failure(
                        IshaaraError.Authentication(
                            code = httpResult.data.statusCode,
                            message = "No session token returned from email OTP sign-in."
                        )
                    )
                }
            }
            is IshaaraResult.Failure -> {
                IshaaraLogger.w(TAG, "[OTP] verify request failed: ${httpResult.error.message}")
                IshaaraResult.failure(httpResult.error)
            }
        }
    }

    override suspend fun getCurrentSession(token: String): IshaaraResult<AuthSessionResponseDto> {
        IshaaraLogger.d(TAG, "backend request started: GET /get-session")
        val request = HttpRequest(
            url = "${networkConfig.authBaseUrl}/get-session",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return when (val httpResult = httpClient.execute(request)) {
            is IshaaraResult.Success -> {
                val response = httpResult.data
                IshaaraLogger.d(TAG, "backend HTTP status: ${response.statusCode}")
                val sessionDto = parseSessionResponse(response.body)
                if (sessionDto.token.isNotBlank() || sessionDto.userId.isNotBlank()) {
                    IshaaraLogger.i(TAG, "backend session verified successfully")
                    val finalDto = if (sessionDto.token.isBlank()) sessionDto.copy(token = token) else sessionDto
                    IshaaraResult.success(finalDto)
                } else {
                    IshaaraLogger.w(TAG, "backend session verification failed: empty session response")
                    IshaaraResult.failure(
                        IshaaraError.Authentication(
                            code = response.statusCode,
                            message = "Session is invalid or expired."
                        )
                    )
                }
            }
            is IshaaraResult.Failure -> {
                val error = httpResult.error
                IshaaraLogger.w(TAG, "backend session verification failed")
                IshaaraLogger.w(TAG, "sanitized error message: ${error.message}")
                IshaaraResult.failure(error)
            }
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
            val json = response.body
            val id = extractJsonField(json, "id") ?: extractJsonField(json, "userId") ?: ""
            val name = extractJsonField(json, "name") ?: "User"
            val email = extractJsonField(json, "email")
            val phoneNumber = extractJsonField(json, "phoneNumber")
            UserDto(
                id = id,
                name = name,
                email = email,
                phoneNumber = phoneNumber,
                role = role,
                isOnboarded = true,
                onboardingCompleted = true
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
            val json = response.body
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

            UserDto(
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
    }

    override suspend fun signOut(token: String): IshaaraResult<Unit> {
        val request = HttpRequest(
            url = "${networkConfig.authBaseUrl}/sign-out",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { }
    }

    private fun parseSessionResponse(json: String): AuthSessionResponseDto {
        // Better Auth response shape may contain token/session token and user object
        val token = extractJsonField(json, "token")
            ?: extractJsonField(json, "sessionToken")
            ?: ""
        val userId = extractJsonField(json, "userId")
            ?: extractJsonField(json, "id")
            ?: ""
        val role = extractJsonField(json, "role")?.takeIf { it.isNotBlank() && it != "null" }
        val rawExpiresAt = extractJsonField(json, "expiresAt")
        val expiresAt = parseTimestamp(rawExpiresAt)

        return AuthSessionResponseDto(
            token = token,
            userId = userId,
            role = role,
            expiresAt = expiresAt
        )
    }

    private fun parseTimestamp(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        raw.toLongOrNull()?.let { num ->
            return if (num < 10_000_000_000L) num * 1000L else num
        }
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                java.time.Instant.parse(raw).toEpochMilli()
            } else {
                java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).parse(raw)?.time
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractJsonField(json: String, field: String): String? {
        val quotedPattern = Regex("\"$field\"\\s*:\\s*\"([^\"]+)\"")
        quotedPattern.find(json)?.let { return it.groupValues[1].trim() }

        val unquotedPattern = Regex("\"$field\"\\s*:\\s*([^,\\s}\\]]+)")
        unquotedPattern.find(json)?.let {
            val v = it.groupValues[1].trim()
            if (v != "null") return v
        }
        return null
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun extractCookieToken(cookieHeader: String?): String? {
        if (cookieHeader.isNullOrBlank()) return null
        val match = Regex("""(?:better-auth\.session_token|session_token)=([^;\s]+)""").find(cookieHeader)
        return match?.groupValues?.get(1)?.trim()
    }

    companion object {
        private const val TAG = "AuthRemoteDataSource"
    }
}
