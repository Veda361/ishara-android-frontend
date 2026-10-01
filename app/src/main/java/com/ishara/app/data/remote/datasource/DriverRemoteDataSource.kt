package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.*
import kotlinx.serialization.json.Json

interface DriverRemoteDataSource {
    suspend fun getDriverProfile(token: String): IshaaraResult<ApiResponse<DriverProfileDto>>
    suspend fun createDriverProfile(token: String, request: CreateDriverProfileRequestDto): IshaaraResult<ApiResponse<DriverProfileDto>>
    suspend fun updateDriverProfile(token: String, request: UpdateDriverProfileRequestDto): IshaaraResult<ApiResponse<DriverProfileDto>>
    suspend fun setOnline(token: String): IshaaraResult<ApiResponse<Map<String, String>>>
    suspend fun setOffline(token: String): IshaaraResult<ApiResponse<Map<String, String>>>
    suspend fun getLatestLocation(token: String): IshaaraResult<ApiResponse<DriverLocationDto>>
    suspend fun updateLocation(token: String, request: UpdateDriverLocationRequestDto): IshaaraResult<ApiResponse<DriverLocationDto>>
    suspend fun submitVerification(token: String, request: DriverVerificationRequestDto): IshaaraResult<ApiResponse<Unit>>
    suspend fun getVerificationStatus(token: String): IshaaraResult<ApiResponse<DriverVerificationStatusDto>>
    suspend fun getReadiness(token: String): IshaaraResult<ApiResponse<DriverReadinessDto>>
    suspend fun getEarnings(token: String, period: String = "today"): IshaaraResult<ApiResponse<DriverEarningsDto>>
}

class DriverRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : DriverRemoteDataSource {

    override suspend fun getDriverProfile(token: String): IshaaraResult<ApiResponse<DriverProfileDto>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { response ->
            json.decodeFromString<ApiResponse<DriverProfileDto>>(response.body)
        }
    }

    override suspend fun createDriverProfile(token: String, request: CreateDriverProfileRequestDto): IshaaraResult<ApiResponse<DriverProfileDto>> {
        val body = json.encodeToString(CreateDriverProfileRequestDto.serializer(), request)
        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = body
        )
        return httpClient.execute(httpRequest).map { response ->
            json.decodeFromString<ApiResponse<DriverProfileDto>>(response.body)
        }
    }

    override suspend fun updateDriverProfile(token: String, request: UpdateDriverProfileRequestDto): IshaaraResult<ApiResponse<DriverProfileDto>> {
        val body = json.encodeToString(UpdateDriverProfileRequestDto.serializer(), request)
        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me",
            method = HttpMethod.PATCH,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = body
        )
        return httpClient.execute(httpRequest).map { response ->
            json.decodeFromString<ApiResponse<DriverProfileDto>>(response.body)
        }
    }

    override suspend fun setOnline(token: String): IshaaraResult<ApiResponse<Map<String, String>>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/status/online",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { response ->
            json.decodeFromString<ApiResponse<Map<String, String>>>(response.body)
        }
    }

    override suspend fun setOffline(token: String): IshaaraResult<ApiResponse<Map<String, String>>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/status/offline",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { response ->
            json.decodeFromString<ApiResponse<Map<String, String>>>(response.body)
        }
    }

    override suspend fun getLatestLocation(token: String): IshaaraResult<ApiResponse<DriverLocationDto>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/location",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { response ->
            json.decodeFromString<ApiResponse<DriverLocationDto>>(response.body)
        }
    }

    override suspend fun updateLocation(token: String, request: UpdateDriverLocationRequestDto): IshaaraResult<ApiResponse<DriverLocationDto>> {
        val body = json.encodeToString(UpdateDriverLocationRequestDto.serializer(), request)
        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/location",
            method = HttpMethod.PATCH,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = body
        )
        return httpClient.execute(httpRequest).map { response ->
            json.decodeFromString<ApiResponse<DriverLocationDto>>(response.body)
        }
    }

    override suspend fun submitVerification(token: String, request: DriverVerificationRequestDto): IshaaraResult<ApiResponse<Unit>> {
        val body = json.encodeToString(DriverVerificationRequestDto.serializer(), request)
        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/verification",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = body
        )
        return httpClient.execute(httpRequest).map { response ->
            json.decodeFromString<ApiResponse<Unit>>(response.body)
        }
    }

    override suspend fun getVerificationStatus(token: String): IshaaraResult<ApiResponse<DriverVerificationStatusDto>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/verification",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { response ->
            json.decodeFromString<ApiResponse<DriverVerificationStatusDto>>(response.body)
        }
    }

    override suspend fun getReadiness(token: String): IshaaraResult<ApiResponse<DriverReadinessDto>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/readiness",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { response ->
            json.decodeFromString<ApiResponse<DriverReadinessDto>>(response.body)
        }
    }

    override suspend fun getEarnings(token: String, period: String): IshaaraResult<ApiResponse<DriverEarningsDto>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/earnings",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token"),
            queryParams = mapOf("period" to period)
        )
        return httpClient.execute(request).map { response ->
            json.decodeFromString<ApiResponse<DriverEarningsDto>>(response.body)
        }
    }
}
