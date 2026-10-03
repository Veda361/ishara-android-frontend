package com.ishara.app.core.result

/**
 * Domain-safe error representation for the Ishaara mobility platform.
 * Decouples domain and presentation layers from framework or network protocol details.
 */
sealed class IshaaraError(
    open val message: String,
    open val cause: Throwable? = null
) {
    open val code: Int? get() = null

    /** Network connectivity issues (offline, DNS resolution failure, socket closed) */
    data class Network(
        override val message: String = "Network connection failed. Please check your internet connection.",
        override val cause: Throwable? = null
    ) : IshaaraError(message, cause)

    /** Authentication errors (401 Unauthorized, token expired, invalid session) */
    data class Authentication(
        override val code: Int = 401,
        override val message: String = "Authentication failed. Please sign in again.",
        val errorCode: String? = null,
        override val cause: Throwable? = null
    ) : IshaaraError(message, cause)

    /** Forbidden access errors (403 Forbidden) */
    data class Forbidden(
        override val message: String = "You do not have permission to perform this action.",
        val errorCode: String? = null,
        override val cause: Throwable? = null
    ) : IshaaraError(message, cause) {
        override val code: Int = 403
    }

    /** Conflict errors (409 Conflict, duplicate request) */
    data class Conflict(
        override val message: String = "A conflicting resource or duplicate request already exists.",
        val errorCode: String? = null,
        override val cause: Throwable? = null
    ) : IshaaraError(message, cause) {
        override val code: Int = 409
    }

    /** Client validation errors (invalid coordinates, missing required parameters) */
    data class Validation(
        val field: String? = null,
        override val message: String,
        val errorCode: String? = null,
        override val cause: Throwable? = null
    ) : IshaaraError(message, cause) {
        override val code: Int = 400
    }

    /** Requested entity not found (404 Not Found, trip expired or removed) */
    data class NotFound(
        override val message: String = "Requested resource was not found.",
        override val cause: Throwable? = null
    ) : IshaaraError(message, cause) {
        override val code: Int = 404
    }

    /** API rate limiting (429 Too Many Requests) */
    data class RateLimited(
        val retryAfterSeconds: Int? = null,
        override val message: String = "Too many requests. Please try again later.",
        override val cause: Throwable? = null
    ) : IshaaraError(message, cause) {
        override val code: Int = 429
    }

    /** Request timeout (socket read timeout, 408 / 504) */
    data class Timeout(
        override val message: String = "Request timed out. Please try again.",
        override val cause: Throwable? = null
    ) : IshaaraError(message, cause) {
        override val code: Int = 408
    }

    /** Remote server errors (500, 502, 503) */
    data class Server(
        override val code: Int = 500,
        override val message: String = "Server encountered an error. Please try again shortly.",
        override val cause: Throwable? = null
    ) : IshaaraError(message, cause)

    /** Device or runtime permission denied */
    data class PermissionDenied(
        override val message: String = "Required permission was denied.",
        override val cause: Throwable? = null
    ) : IshaaraError(message, cause)

    /** Hardware location services disabled */
    data class LocationDisabled(
        override val message: String = "Location services are disabled.",
        override val cause: Throwable? = null
    ) : IshaaraError(message, cause)

    /** Unexpected or unhandled exceptions */
    data class Unknown(
        override val message: String = "An unexpected error occurred.",
        override val cause: Throwable? = null
    ) : IshaaraError(message, cause)
}
