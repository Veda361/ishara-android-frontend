package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.RideRequestResult
import com.ishara.app.domain.model.RideRequestStatus
import com.ishara.app.domain.repository.RideRepository

/**
 * Use case to retrieve the list of ride requests created by the authenticated passenger.
 * Supports filtering by status and tripId.
 */
class GetUserRideRequestsUseCase(
    private val rideRepository: RideRepository
) {
    suspend fun execute(
        status: RideRequestStatus? = null,
        tripId: String? = null
    ): IshaaraResult<List<RideRequestResult>> {
        return rideRepository.getUserRideRequests(status, tripId)
    }
}
