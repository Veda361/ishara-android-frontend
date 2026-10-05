package com.ishara.app.data.remote.datasource

import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.remote.dto.*
import kotlinx.serialization.json.Json

interface RideRemoteDataSource {
    suspend fun createRideRequest(token: String, request: CreateRideRequestDto): IshaaraResult<ApiResponse<RideRequestDto>>
    suspend fun listMyRideRequests(token: String): IshaaraResult<ApiResponse<List<RideRequestDto>>>
    suspend fun getRideRequest(token: String, requestId: String): IshaaraResult<ApiResponse<RideRequestDto>>
    suspend fun cancelRideRequest(token: String, requestId: String, reason: String): IshaaraResult<ApiResponse<Unit>>

    suspend fun acceptRideRequest(token: String, requestId: String): IshaaraResult<ApiResponse<RideDto>>
    suspend fun rejectRideRequest(token: String, requestId: String, reason: String): IshaaraResult<ApiResponse<Unit>>

    suspend fun getRide(token: String, rideId: String): IshaaraResult<ApiResponse<RideDto>>
    suspend fun getDriverLocation(token: String, rideId: String): IshaaraResult<ApiResponse<DriverLocationDto>>
    suspend fun getRideTracking(token: String, rideId: String): IshaaraResult<ApiResponse<RideTrackingDto>>

    suspend fun driverArrived(token: String, rideId: String): IshaaraResult<ApiResponse<Unit>>
    suspend fun driverPickedUp(token: String, rideId: String): IshaaraResult<ApiResponse<Unit>>
    suspend fun startRide(token: String, rideId: String): IshaaraResult<ApiResponse<Unit>>
    suspend fun completeRide(token: String, rideId: String): IshaaraResult<ApiResponse<Unit>>
    suspend fun cancelRide(token: String, rideId: String, reason: String): IshaaraResult<ApiResponse<Unit>>

    suspend fun getFareBreakdown(token: String, rideId: String): IshaaraResult<ApiResponse<FareBreakdownDto>>

    suspend fun createPaymentOrder(token: String, rideId: String, idempotencyKey: String? = null): IshaaraResult<ApiResponse<PaymentRecordDto>>
    suspend fun verifyPayment(token: String, paymentId: String, request: PaymentVerificationRequestDto): IshaaraResult<ApiResponse<PaymentRecordDto>>

    suspend fun submitRating(token: String, rideId: String, score: Int, review: String?): IshaaraResult<ApiResponse<Unit>>
}

class RideRemoteDataSourceImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : RideRemoteDataSource {

    override suspend fun createRideRequest(token: String, request: CreateRideRequestDto): IshaaraResult<ApiResponse<RideRequestDto>> {
        val body = json.encodeToString(CreateRideRequestDto.serializer(), request)
        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/ride-requests",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token", "Content-Type" to "application/json"),
            body = body
        )
        return httpClient.execute(httpRequest).map { json.decodeFromString(it.body) }
    }

    override suspend fun listMyRideRequests(token: String): IshaaraResult<ApiResponse<List<RideRequestDto>>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/ride-requests/me",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { json.decodeFromString(it.body) }
    }

    override suspend fun getRideRequest(token: String, requestId: String): IshaaraResult<ApiResponse<RideRequestDto>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/ride-requests/$requestId",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { json.decodeFromString(it.body) }
    }

    override suspend fun cancelRideRequest(token: String, requestId: String, reason: String): IshaaraResult<ApiResponse<Unit>> {
        val body = """{"reason": "$reason"}"""
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/ride-requests/$requestId/cancel",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token", "Content-Type" to "application/json"),
            body = body
        )
        return httpClient.execute(request).map { json.decodeFromString(it.body) }
    }

    override suspend fun acceptRideRequest(token: String, requestId: String): IshaaraResult<ApiResponse<RideDto>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/ride-requests/$requestId/accept",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { json.decodeFromString(it.body) }
    }

    override suspend fun rejectRideRequest(token: String, requestId: String, reason: String): IshaaraResult<ApiResponse<Unit>> {
        val body = """{"reason": "$reason"}"""
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/ride-requests/$requestId/reject",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token", "Content-Type" to "application/json"),
            body = body
        )
        return httpClient.execute(request).map { json.decodeFromString(it.body) }
    }

    override suspend fun getRide(token: String, rideId: String): IshaaraResult<ApiResponse<RideDto>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { json.decodeFromString(it.body) }
    }

    override suspend fun getDriverLocation(token: String, rideId: String): IshaaraResult<ApiResponse<DriverLocationDto>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/driver-location",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { json.decodeFromString(it.body) }
    }

    override suspend fun getRideTracking(token: String, rideId: String): IshaaraResult<ApiResponse<RideTrackingDto>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/tracking",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { json.decodeFromString(it.body) }
    }

    override suspend fun driverArrived(token: String, rideId: String): IshaaraResult<ApiResponse<Unit>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/arrive",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { json.decodeFromString(it.body) }
    }

    override suspend fun driverPickedUp(token: String, rideId: String): IshaaraResult<ApiResponse<Unit>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/pickup",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { json.decodeFromString(it.body) }
    }

    override suspend fun startRide(token: String, rideId: String): IshaaraResult<ApiResponse<Unit>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/start",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { json.decodeFromString(it.body) }
    }

    override suspend fun completeRide(token: String, rideId: String): IshaaraResult<ApiResponse<Unit>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/complete",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { json.decodeFromString(it.body) }
    }

    override suspend fun cancelRide(token: String, rideId: String, reason: String): IshaaraResult<ApiResponse<Unit>> {
        val body = """{"reason": "$reason"}"""
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/cancel",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token", "Content-Type" to "application/json"),
            body = body
        )
        return httpClient.execute(request).map { json.decodeFromString(it.body) }
    }

    override suspend fun getFareBreakdown(token: String, rideId: String): IshaaraResult<ApiResponse<FareBreakdownDto>> {
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/fare",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )
        return httpClient.execute(request).map { json.decodeFromString(it.body) }
    }

    override suspend fun createPaymentOrder(token: String, rideId: String, idempotencyKey: String?): IshaaraResult<ApiResponse<PaymentRecordDto>> {
        val headers = mutableMapOf("Authorization" to "Bearer $token", "Content-Type" to "application/json")
        idempotencyKey?.let { headers["Idempotency-Key"] = it }
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/payment",
            method = HttpMethod.POST,
            headers = headers
        )
        return httpClient.execute(request).map { json.decodeFromString(it.body) }
    }

    override suspend fun verifyPayment(token: String, paymentId: String, request: PaymentVerificationRequestDto): IshaaraResult<ApiResponse<PaymentRecordDto>> {
        val body = json.encodeToString(PaymentVerificationRequestDto.serializer(), request)
        val httpRequest = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/payments/$paymentId/verify",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token", "Content-Type" to "application/json"),
            body = body
        )
        return httpClient.execute(httpRequest).map { json.decodeFromString(it.body) }
    }

    override suspend fun submitRating(token: String, rideId: String, score: Int, review: String?): IshaaraResult<ApiResponse<Unit>> {
        val body = """{"score": $score, "review": "${review ?: ""}"}"""
        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/ratings",
            method = HttpMethod.POST,
            headers = mapOf("Authorization" to "Bearer $token", "Content-Type" to "application/json"),
            body = body
        )
        return httpClient.execute(request).map { json.decodeFromString(it.body) }
    }
}
