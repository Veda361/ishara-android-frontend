package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.*
import kotlinx.serialization.json.Json

interface TripRemoteDataSource {
    suspend fun listActiveTrips(
        originLat: Double?,
        originLng: Double?,
        destLat: Double?,
        destLng: Double?,
        page: Int?,
        limit: Int?
    ): IshaaraResult<ApiResponse<List<TripDto>>>

    suspend fun createTrip(token: String, request: CreateTripRequestDto): IshaaraResult<ApiResponse<TripDto>>
    
    suspend fun getTripById(tripId: String, token: String): IshaaraResult<ApiResponse<TripDto>>
    
    suspend fun startTrip(tripId: String, token: String): IshaaraResult<ApiResponse<Unit>>
    
    suspend fun completeTrip(tripId: String, token: String): IshaaraResult<ApiResponse<Unit>>
    
    suspend fun cancelTrip(tripId: String, token: String, reason: String): IshaaraResult<ApiResponse<Unit>>
    
    suspend fun discoverTrips(request: DiscoverTripsRequestDto, token: String): IshaaraResult<ApiResponse<DiscoveryResponseDto>>
}

class TripRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : TripRemoteDataSource {

    override suspend fun listActiveTrips(
        originLat: Double?,
        originLng: Double?,
        destLat: Double?,
        destLng: Double?,
        page: Int?,
        limit: Int?
    ): IshaaraResult<ApiResponse<List<TripDto>>> {
        val queryParams = mutableMapOf<String, String>()
        originLat?.let { queryParams["originLat"] = it.toString() }
        originLng?.let { queryParams["originLng"] = it.toString() }
        destLat?.let { queryParams["destLat"] = it.toString() }
        destLng?.let { queryParams["destLng"] = it.toString() }
        page?.let { queryParams["page"] = it.toString() }
        limit?.let { queryParams["limit"] = it.toString() }

        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/active",
            method = HttpMethod.GET,
            queryParams = queryParams
        )
        return httpClient.execute(request).map { response ->
            json.decodeFromString<ApiResponse<List<TripDto>>>(response.body)
        }
    }

    override suspend fun createTrip(token: String, request: CreateTripRequestDto): IshaaraResult<ApiResponse<TripDto>> {
        val body = json.encodeToString(CreateTripRequestDto.serializer(), request)
        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = body
        )
        return httpClient.execute(httpRequest).map { response ->
            json.decodeFromString<ApiResponse<TripDto>>(response.body)
        }
    }

    override suspend fun getTripById(tripId: String, token: String): IshaaraResult<ApiResponse<TripDto>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/$tripId",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { response ->
            json.decodeFromString<ApiResponse<TripDto>>(response.body)
        }
    }

    override suspend fun startTrip(tripId: String, token: String): IshaaraResult<ApiResponse<Unit>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/$tripId/start",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { response ->
            json.decodeFromString<ApiResponse<Unit>>(response.body)
        }
    }

    override suspend fun completeTrip(tripId: String, token: String): IshaaraResult<ApiResponse<Unit>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/$tripId/complete",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { response ->
            json.decodeFromString<ApiResponse<Unit>>(response.body)
        }
    }

    override suspend fun cancelTrip(tripId: String, token: String, reason: String): IshaaraResult<ApiResponse<Unit>> {
        val body = """{"reason": "$reason"}"""
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/$tripId/cancel",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = body
        )
        return httpClient.execute(request).map { response ->
            json.decodeFromString<ApiResponse<Unit>>(response.body)
        }
    }

    override suspend fun discoverTrips(
        request: DiscoverTripsRequestDto,
        token: String
    ): IshaaraResult<ApiResponse<DiscoveryResponseDto>> {
        val body = json.encodeToString(DiscoverTripsRequestDto.serializer(), request)
        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/discovery/trips",
            method = HttpMethod.POST,
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to "application/json"
            ),
            body = body
        )
        return httpClient.execute(httpRequest).map { response ->
            json.decodeFromString<ApiResponse<DiscoveryResponseDto>>(response.body)
        }
    }
}
