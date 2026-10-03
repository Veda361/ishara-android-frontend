package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.Rating
import com.ishara.app.domain.repository.RatingRepository

/**
 * Use case to submit a post-ride rating and optional review for a completed ride.
 */
class SubmitRatingUseCase(
    private val ratingRepository: RatingRepository
) {
    suspend operator fun invoke(
        rideId: String,
        score: Int,
        review: String? = null,
        idempotencyKey: String? = null
    ): IshaaraResult<Rating> {
        return ratingRepository.submitRating(
            rideId = rideId,
            score = score,
            review = review,
            idempotencyKey = idempotencyKey
        )
    }
}
