package com.ishara.app.core.network

/**
 * Centralized network environment configuration.
 * Avoids hardcoding URLs across source files and provides clean environment switching.
 */
enum class Environment(val baseUrl: String, val wsBaseUrl: String) {
    DEVELOPMENT(
        baseUrl = "http://10.0.2.2:5000",
        wsBaseUrl = "ws://10.0.2.2:5000"
    ),
    STAGING(
        baseUrl = "https://staging-ishaara.onrender.com",
        wsBaseUrl = "wss://staging-ishaara.onrender.com"
    ),
    PRODUCTION(
        baseUrl = "https://reposnse-ishaara.onrender.com",
        wsBaseUrl = "wss://reposnse-ishaara.onrender.com"
    );
}

data class NetworkConfig(
    val environment: Environment = Environment.PRODUCTION,
    val apiPrefix: String = "/api/v1",
    val connectTimeoutMillis: Long = 15_000L,
    val readTimeoutMillis: Long = 30_000L
) {
    val fullApiBaseUrl: String
        get() = "${environment.baseUrl}$apiPrefix"

    val fullWebSocketBaseUrl: String
        get() = "${environment.wsBaseUrl}$apiPrefix"

    val authBaseUrl: String
        get() = "${environment.baseUrl}/api/auth"
}
