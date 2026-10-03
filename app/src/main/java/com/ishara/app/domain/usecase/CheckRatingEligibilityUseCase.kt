package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.RatingEligibility
import com.ishara.app.domain.repository.RatingRepository

/**
 * Use case to verify whether a user is eligible to rate a completed ride.
 */
class CheckRatingEligibilityUseCase(
    private val ratingRepository: RatingRepository
) {
    suspend operator fun invoke(rideId: String): IshaaraResult<RatingEligibility> {
        return ratingRepository.checkRatingEligibility(rideId)
    }
}
