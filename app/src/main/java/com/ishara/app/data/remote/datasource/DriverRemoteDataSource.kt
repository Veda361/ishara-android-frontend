package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.DriverLocationUpdateDto
import com.ishara.app.data.remote.dto.DriverProfileDto

interface DriverRemoteDataSource {
    suspend fun getDriverProfile(token: String): IshaaraResult<DriverProfileDto>
    suspend fun setOnline(token: String): IshaaraResult<Unit>
    suspend fun setOffline(token: String): IshaaraResult<Unit>
    suspend fun updateLocation(dto: DriverLocationUpdateDto, token: String): IshaaraResult<Unit>
}

class DriverRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : DriverRemoteDataSource {

    override suspend fun getDriverProfile(token: String): IshaaraResult<DriverProfileDto> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/profile",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map {
            DriverProfileDto(
                id = "mock_driver_id",
                userId = "mock_user_id",
                licenseNumber = "MH****1234",
                yearsOfExperience = 5,
                isOnline = true,
                ratingAverage = 4.8,
                totalRatingsCount = 120
            )
        }
    }

    override suspend fun setOnline(token: String): IshaaraResult<Unit> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/status/online",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { }
    }

    override suspend fun setOffline(token: String): IshaaraResult<Unit> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/status/offline",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { }
    }

    override suspend fun updateLocation(
        dto: DriverLocationUpdateDto,
        token: String
    ): IshaaraResult<Unit> {
        val body = """
            {
                "coordinates": [${dto.coordinates[0]}, ${dto.coordinates[1]}],
                "heading": ${dto.heading ?: 0.0},
                "speed": ${dto.speed ?: 0.0},
                "accuracy": ${dto.accuracy ?: 5.0}
            }
        """.trimIndent()

        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/drivers/me/location",
            method = HttpMethod.PATCH,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer $token"
            ),
            body = body
        )
        return httpClient.execute(request).map { }
    }
}
