package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.PassengerTripDetails
import com.ishara.app.domain.repository.TripRepository

/**
 * Use case to fetch public sanitized trip details for passenger inspection:
 * GET /api/v1/trips/:tripId
 *
 * Privacy enforcement: Contains no private driver data (license, phone, internal IDs).
 */
class GetPassengerTripDetailsUseCase(
    private val tripRepository: TripRepository
) {
    suspend operator fun invoke(tripId: String): IshaaraResult<PassengerTripDetails> {
        if (tripId.isBlank()) {
            return IshaaraResult.failure(
                IshaaraError.Validation("tripId", "Trip ID cannot be blank.")
            )
        }
        return tripRepository.getPassengerTripDetails(tripId)
    }
}
