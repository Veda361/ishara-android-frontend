package com.ishara.app.core.network

import com.ishara.app.BuildConfig

class NetworkConfig {
    val connectTimeoutMillis: Long = 30000
    val readTimeoutMillis: Long = 30000

    val baseUrl: String = BuildConfig.BASE_URL.removeSuffix("/")
    
    // Better Auth endpoints (Section 2 of API Ref)
    val authBaseUrl: String = "$baseUrl/api/auth"
    
    // Ishaara API endpoints (Section 3+ of API Ref)
    val fullApiBaseUrl: String = "$baseUrl/api/v1"
}
