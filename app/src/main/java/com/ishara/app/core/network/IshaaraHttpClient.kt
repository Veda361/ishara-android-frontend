package com.ishara.app.core.network

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

enum class HttpMethod {
    GET, POST, PUT, PATCH, DELETE
}

data class HttpRequest(
    val url: String,
    val method: HttpMethod,
    val headers: Map<String, String> = emptyMap(),
    val queryParams: Map<String, String> = emptyMap(),
    val body: String? = null
)

data class HttpResponse(
    val statusCode: Int,
    val headers: Map<String, String>,
    val body: String
) {
    constructor(statusCode: Int, body: String) : this(statusCode, emptyMap(), body)
    val isSuccessful: Boolean get() = statusCode in 200..299
}

/**
 * Architectural abstraction over network transports.
 * Decouples the application data layer from the specific third-party HTTP engine.
 */
interface IshaaraHttpClient {
    suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse>
}

/**
 * Production-ready HTTP client implementation using java.net.HttpURLConnection.
 * Features:
 * - Automatic timeout handling (connect/read)
 * - Safe request/response streaming
 * - Sensitive token redaction in logs (Authorization: [REDACTED])
 * - 401 interception delegating to SessionInvalidationCoordinator
 * - Centralized network and error normalization
 */
class DefaultIshaaraHttpClient(
    private val config: NetworkConfig,
    private val sessionCoordinator: SessionInvalidationCoordinator? = null
) : IshaaraHttpClient {

    override suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val fullUrlString = buildUrl(request.url, request.queryParams)
            val url = URL(fullUrlString)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = config.connectTimeoutMillis.toInt()
                readTimeout = config.readTimeoutMillis.toInt()
                instanceFollowRedirects = false
                useCaches = false

                val hasBody = request.body != null && (request.method in listOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH))
                if (hasBody) {
                    doOutput = true
                }

                // PATCH method support via method override header if needed
                if (request.method == HttpMethod.PATCH) {
                    requestMethod = "POST"
                    setRequestProperty("X-HTTP-Method-Override", "PATCH")
                } else {
                    requestMethod = request.method.name
                }

                // Default headers
                if (!request.headers.keys.any { it.equals("Accept", ignoreCase = true) }) {
                    setRequestProperty("Accept", "application/json")
                }
                if (!request.headers.keys.any { it.equals("User-Agent", ignoreCase = true) }) {
                    setRequestProperty("User-Agent", "Ishaara-Android/1.0")
                }

                // Inject headers
                request.headers.forEach { (key, value) ->
                    setRequestProperty(key, value)
                }

                logSanitizedRequest(request.method.name, fullUrlString, request.headers)

                // Write body for methods with payload
                if (hasBody) {
                    val bodyBytes = request.body!!.toByteArray(Charsets.UTF_8)
                    setFixedLengthStreamingMode(bodyBytes.size)
                    outputStream.use { os ->
                        os.write(bodyBytes)
                        os.flush()
                    }
                }
            }

            val statusCode = connection.responseCode
            val responseHeaders = connection.headerFields
                .filterKeys { it != null }
                .map { (k, v) -> k!!.lowercase() to v.joinToString(", ") }
                .toMap()

            val inputStream = if (statusCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }

            val responseBody = inputStream?.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            }.orEmpty()

            val errorCode = ErrorNormalizer.extractCode(responseBody)
            val serverMessage = ErrorNormalizer.extractMessage(responseBody)

            IshaaraLogger.d(
                TAG,
                "[SafeDiagnostic] HTTP $statusCode <- $fullUrlString | status = $statusCode | endpoint = ${url.path} | backend error code = ${errorCode ?: "NONE"}"
            )

            if (statusCode == 401) {
                val isAuthEndpoint = fullUrlString.contains("/api/auth/sign-in") ||
                                     fullUrlString.contains("/api/auth/email-otp")

                if (!isAuthEndpoint) {
                    val isSessionExpired = serverMessage?.contains("expired", ignoreCase = true) == true ||
                                          errorCode?.contains("EXPIRED", ignoreCase = true) == true ||
                                          errorCode == "SESSION_EXPIRED" ||
                                          errorCode == "TOKEN_EXPIRED"

                    if (isSessionExpired) {
                        val invalidationReason = serverMessage ?: "Your session has expired. Please sign in again."
                        IshaaraLogger.w(TAG, "Backend confirmed session expiration: $invalidationReason")
                        sessionCoordinator?.notifyUnauthorized(invalidationReason)
                    } else {
                        // PHASE 6: DO NOT ASSUME 401 = EXPIRED
                        IshaaraLogger.w(TAG, "HTTP 401 received without expiration indication (code=$errorCode, message=$serverMessage); preserving session.")
                    }
                }
            }

            if (statusCode in 200..299) {
                IshaaraResult.success(
                    HttpResponse(
                        statusCode = statusCode,
                        headers = responseHeaders,
                        body = responseBody
                    )
                )
            } else {
                IshaaraResult.failure(ErrorNormalizer.normalize(statusCode, responseBody))
            }
        } catch (e: UnknownHostException) {
            IshaaraLogger.w(TAG, "Network unavailable (UnknownHostException): ${e.message}")
            IshaaraResult.failure(IshaaraError.Network("You're offline. Check your internet connection.", e))
        } catch (e: SocketTimeoutException) {
            IshaaraLogger.w(TAG, "Request timed out: ${e.message}")
            IshaaraResult.failure(IshaaraError.Timeout("Request timed out. Please try again.", e))
        } catch (e: Exception) {
            IshaaraLogger.e(TAG, "Transport error: ${e.message}", e)
            IshaaraResult.failure(IshaaraError.Network("Network communication failed: ${e.localizedMessage ?: "Unknown"}", e))
        } finally {
            connection?.disconnect()
        }
    }

    private fun buildUrl(baseUrl: String, queryParams: Map<String, String>): String {
        if (queryParams.isEmpty()) return baseUrl
        val separator = if (baseUrl.contains("?")) "&" else "?"
        val queryString = queryParams.entries.joinToString("&") { (k, v) -> "$k=$v" }
        return "$baseUrl$separator$queryString"
    }

    private fun logSanitizedRequest(method: String, url: String, headers: Map<String, String>) {
        val authHeader = headers.entries.firstOrNull { it.key.equals("Authorization", ignoreCase = true) }?.value
        val hasAuth = authHeader != null
        val tokenLen = authHeader?.removePrefix("Bearer ")?.trim()?.length ?: 0
        IshaaraLogger.d(
            TAG,
            "[SafeDiagnostic] HTTP $method -> $url | Authorization present = $hasAuth | token length = $tokenLen | token source = EncryptedSessionStore"
        )
    }

    companion object {
        private const val TAG = "IshaaraHttpClient"
    }
}

/**
 * Normalizes raw HTTP errors and transport exceptions into domain-safe IshaaraError.
 */
object ErrorNormalizer {
    fun normalize(statusCode: Int, body: String?, throwable: Throwable? = null): IshaaraError {
        val errorCode = extractCode(body)
        val message = extractMessage(body)

        return when (statusCode) {
            400 -> IshaaraError.Validation(
                errorCode = errorCode,
                message = message ?: "Bad request submitted.",
                cause = throwable
            )
            401 -> IshaaraError.Authentication(
                code = 401,
                errorCode = errorCode,
                message = message ?: "Authentication required. Please sign in.",
                cause = throwable
            )
            403 -> IshaaraError.Forbidden(
                errorCode = errorCode,
                message = message ?: "Access denied for your role.",
                cause = throwable
            )
            404 -> IshaaraError.NotFound(
                message = message ?: "Resource not found.",
                cause = throwable
            )
            408, 504 -> IshaaraError.Timeout(
                message = message ?: "Request timed out.",
                cause = throwable
            )
            409 -> IshaaraError.Conflict(
                errorCode = errorCode,
                message = message ?: "A conflict occurred with the current resource state.",
                cause = throwable
            )
            422 -> IshaaraError.Validation(
                errorCode = errorCode,
                message = message ?: "Invalid parameters submitted.",
                cause = throwable
            )
            429 -> IshaaraError.RateLimited(
                message = message ?: "Rate limit exceeded. Please wait.",
                cause = throwable
            )
            in 500..599 -> IshaaraError.Server(
                code = statusCode,
                message = message ?: "Server error ($statusCode). Please try again shortly.",
                cause = throwable
            )
            else -> IshaaraError.Unknown(
                message = message ?: "HTTP request failed with status $statusCode",
                cause = throwable
            )
        }
    }

    fun extractMessage(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val messageMatch = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body)
        return messageMatch?.groupValues?.get(1)
    }

    fun extractCode(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val codeMatch = Regex("\"code\"\\s*:\\s*\"([^\"]+)\"").find(body)
        return codeMatch?.groupValues?.get(1)
    }
}
