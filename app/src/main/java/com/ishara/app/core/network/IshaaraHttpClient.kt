package com.ishara.app.core.network

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult

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
    val isSuccessful: Boolean get() = statusCode in 200..299
}

/**
 * Architectural abstraction over network transports (HttpURLConnection, OkHttp, Retrofit, Ktor).
 * Decouples the application data layer from the specific third-party HTTP engine.
 */
interface IshaaraHttpClient {
    suspend fun execute(request: HttpRequest): IshaaraResult<HttpResponse>
}

/**
 * Normalizes raw HTTP errors and transport exceptions into domain-safe IshaaraError.
 */
object ErrorNormalizer {
    fun normalize(statusCode: Int, body: String?, throwable: Throwable? = null): IshaaraError {
        return when (statusCode) {
            401 -> IshaaraError.Authentication(
                code = 401,
                message = extractMessage(body) ?: "Unauthorized. Please sign in again.",
                cause = throwable
            )
            403 -> IshaaraError.Authentication(
                code = 403,
                message = extractMessage(body) ?: "Access denied for your role.",
                cause = throwable
            )
            404 -> IshaaraError.NotFound(
                message = extractMessage(body) ?: "Resource not found.",
                cause = throwable
            )
            408, 504 -> IshaaraError.Timeout(
                message = extractMessage(body) ?: "Request timed out.",
                cause = throwable
            )
            422 -> IshaaraError.Validation(
                message = extractMessage(body) ?: "Invalid parameters submitted.",
                cause = throwable
            )
            429 -> IshaaraError.RateLimited(
                message = extractMessage(body) ?: "Rate limit exceeded. Please wait.",
                cause = throwable
            )
            in 500..599 -> IshaaraError.Server(
                code = statusCode,
                message = extractMessage(body) ?: "Server error ($statusCode).",
                cause = throwable
            )
            else -> IshaaraError.Unknown(
                message = extractMessage(body) ?: "HTTP request failed with status $statusCode",
                cause = throwable
            )
        }
    }

    private fun extractMessage(body: String?): String? {
        if (body.isNullOrBlank()) return null
        val messageMatch = Regex("\"message\"\\s*:\\s*\"([^\"]+)\"").find(body)
        return messageMatch?.groupValues?.get(1)
    }
}
