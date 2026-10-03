package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.Rating
import com.ishara.app.domain.repository.RatingRepository

/**
 * Use case to retrieve ratings for a specific ride.
 */
class GetRideRatingsUseCase(
    private val ratingRepository: RatingRepository
) {
    suspend operator fun invoke(rideId: String): IshaaraResult<List<Rating>> {
        return ratingRepository.getRatingsByRide(rideId)
    }
}
