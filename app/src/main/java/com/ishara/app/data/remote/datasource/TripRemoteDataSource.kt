package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.DiscoverTripsRequestDto
import com.ishara.app.data.remote.dto.TripDto

interface TripRemoteDataSource {
    suspend fun discoverTrips(request: DiscoverTripsRequestDto, token: String?): IshaaraResult<List<TripDto>>
    suspend fun getTripById(tripId: String, token: String?): IshaaraResult<TripDto>
    suspend fun startTrip(tripId: String, token: String): IshaaraResult<TripDto>
    suspend fun completeTrip(tripId: String, token: String): IshaaraResult<TripDto>
}

class TripRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig
) : TripRemoteDataSource {

    override suspend fun discoverTrips(
        request: DiscoverTripsRequestDto,
        token: String?
    ): IshaaraResult<List<TripDto>> {
        val headers = mutableMapOf("Content-Type" to "application/json")
        if (token != null) headers["Authorization"] = "Bearer $token"

        val body = """
            {
                "pickup": {
                    "address": "${request.pickup.address}",
                    "coordinates": [${request.pickup.coordinates[0]}, ${request.pickup.coordinates[1]}]
                },
                "destination": {
                    "address": "${request.destination.address}",
                    "coordinates": [${request.destination.coordinates[0]}, ${request.destination.coordinates[1]}]
                },
                "passengerCount": ${request.passengerCount}
            }
        """.trimIndent()

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/discovery/trips",
            method = HttpMethod.POST,
            headers = headers,
            body = body
        )

        return httpClient.execute(httpRequest).map { response ->
            // In a full implementation, JSON deserialization maps list of TripDto
            emptyList()
        }
    }

    override suspend fun getTripById(tripId: String, token: String?): IshaaraResult<TripDto> {
        val headers = mutableMapOf<String, String>()
        if (token != null) headers["Authorization"] = "Bearer $token"

        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/$tripId",
            method = HttpMethod.GET,
            headers = headers
        )
        return httpClient.execute(httpRequest).map { response ->
            throw UnsupportedOperationException("Full JSON deserialization to be wired with JSON engine")
        }
    }

    override suspend fun startTrip(tripId: String, token: String): IshaaraResult<TripDto> {
        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/$tripId/start",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(httpRequest).map { response ->
            throw UnsupportedOperationException("Full JSON deserialization to be wired with JSON engine")
        }
    }

    override suspend fun completeTrip(tripId: String, token: String): IshaaraResult<TripDto> {
        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/trips/$tripId/complete",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(httpRequest).map { response ->
            throw UnsupportedOperationException("Full JSON deserialization to be wired with JSON engine")
        }
    }
}
