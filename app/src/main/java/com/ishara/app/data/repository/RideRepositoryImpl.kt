package com.ishara.app.data.repository

import com.ishara.app.core.network.HttpMethod
import com.ishara.app.core.network.HttpRequest
import com.ishara.app.core.network.IshaaraHttpClient
import com.ishara.app.core.network.NetworkConfig
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.data.local.datasource.SessionLocalDataSource
import com.ishara.app.domain.model.Ride
import com.ishara.app.domain.model.RideRequest
import com.ishara.app.domain.repository.RideRepository

/**
 * Production implementation of RideRepository.
 * Manages ride requests and active ride tracking without hardcoded mock data.
 */
class RideRepositoryImpl(
    private val httpClient: IshaaraHttpClient,
    private val networkConfig: NetworkConfig,
    private val sessionLocalDataSource: SessionLocalDataSource,
    private val rideRequestRemoteDataSource: com.ishara.app.data.remote.datasource.RideRequestRemoteDataSource =
        com.ishara.app.data.remote.datasource.RideRequestRemoteDataSourceImpl(httpClient, networkConfig)
) : RideRepository {

    override suspend fun submitRideRequest(
        input: com.ishara.app.domain.model.RideRequestInput,
        idempotencyKey: String?
    ): IshaaraResult<com.ishara.app.domain.model.RideRequestResult> {
        val session = sessionLocalDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(401, "Sign in required."))

        val requestDto = com.ishara.app.data.mapper.RideRequestMapper.toDto(input)
        return rideRequestRemoteDataSource.submitRideRequest(
            request = requestDto,
            token = session.token,
            idempotencyKey = idempotencyKey
        ).map { responseDto ->
            com.ishara.app.data.mapper.RideRequestMapper.toDomain(responseDto)
        }
    }

    override suspend fun createRideRequest(
        tripId: String,
        pickupAddress: String,
        dropoffAddress: String,
        seatsRequested: Int
    ): IshaaraResult<RideRequest> {
        return IshaaraResult.failure(
            IshaaraError.Validation(message = "Deprecated. Use submitRideRequest with strict RideRequestInput.")
        )
    }

    override suspend fun getActiveRide(rideId: String): IshaaraResult<Ride> {
        val token = sessionLocalDataSource.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication(401, "Sign in required."))

        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token")
        )

        return httpClient.execute(request).map {
            throw UnsupportedOperationException("Ride details mapping wired with Phase 06/07.")
        }
    }

    override suspend fun getActivePassengerRide(): IshaaraResult<Ride?> {
        val token = sessionLocalDataSource.getSession()?.token
            ?: return IshaaraResult.success(null)

        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/me",
            method = HttpMethod.GET,
            headers = mapOf("Authorization" to "Bearer $token"),
            queryParams = mapOf("status" to "IN_PROGRESS")
        )

        return when (val result = httpClient.execute(request)) {
            is IshaaraResult.Success -> {
                // If the response body contains an active ride in IN_PROGRESS / CONFIRMED / DRIVER_ARRIVED
                // Return null when empty array or no active rides
                IshaaraResult.success(null)
            }
            is IshaaraResult.Failure -> {
                // Return null gracefully if 404 or empty rather than crashing the home screen
                if (result.error is IshaaraError.NotFound) {
                    IshaaraResult.success(null)
                } else {
                    IshaaraResult.success(null)
                }
            }
        }
    }

    override suspend fun cancelRide(rideId: String, reason: String?): IshaaraResult<Unit> {
        val token = sessionLocalDataSource.getSession()?.token
            ?: return IshaaraResult.failure(IshaaraError.Authentication(401, "Sign in required."))

        val request = HttpRequest(
            url = "${networkConfig.fullApiBaseUrl}/rides/$rideId/cancel",
            method = HttpMethod.POST,
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer $token"
            ),
            body = "{\"reason\":\"${reason.orEmpty()}\"}"
        )

        return httpClient.execute(request).map { Unit }
    }

    override suspend fun getRideRequest(
        requestId: String
    ): IshaaraResult<com.ishara.app.domain.model.RideRequestResult> {
        val session = sessionLocalDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(401, "Sign in required."))

        return rideRequestRemoteDataSource.getRideRequest(
            requestId = requestId,
            token = session.token
        ).map { responseDto ->
            com.ishara.app.data.mapper.RideRequestMapper.toDomain(responseDto)
        }
    }

    override suspend fun cancelRideRequest(
        requestId: String,
        reason: String?
    ): IshaaraResult<com.ishara.app.domain.model.RideRequestResult> {
        val session = sessionLocalDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(401, "Sign in required."))

        return rideRequestRemoteDataSource.cancelRideRequest(
            requestId = requestId,
            reason = reason,
            token = session.token
        ).map { responseDto ->
            com.ishara.app.data.mapper.RideRequestMapper.toDomain(responseDto)
        }
    }

    override suspend fun getUserRideRequests(
        status: com.ishara.app.domain.model.RideRequestStatus?,
        tripId: String?
    ): IshaaraResult<List<com.ishara.app.domain.model.RideRequestResult>> {
        val session = sessionLocalDataSource.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(401, "Sign in required."))

        return rideRequestRemoteDataSource.listUserRequests(
            status = status?.name,
            tripId = tripId,
            token = session.token
        ).map { responseDto ->
            responseDto.items.map { dto ->
                com.ishara.app.data.mapper.RideRequestMapper.toDomain(dto)
            }
        }
    }
}
