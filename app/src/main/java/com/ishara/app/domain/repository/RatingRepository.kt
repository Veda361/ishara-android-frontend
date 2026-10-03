package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverRatingSummary
import com.ishara.app.domain.model.Rating
import com.ishara.app.domain.model.RatingEligibility

/**
 * Domain repository contract for Post-Ride Ratings, Reviews, and Driver Rating Summaries.
 * Keeps strict separation from Safety / SOS repositories.
 */
interface RatingRepository {

    /**
     * Checks if the authenticated user is eligible to rate the specified completed ride.
     */
    suspend fun checkRatingEligibility(rideId: String): IshaaraResult<RatingEligibility>

    /**
     * Submits a rating (score 1-5, optional review) for a completed ride.
     * Supports optional idempotency key for network retry safety.
     */
    suspend fun submitRating(
        rideId: String,
        score: Int,
        review: String? = null,
        idempotencyKey: String? = null
    ): IshaaraResult<Rating>

    /**
     * Retrieves ratings associated with a specific ride.
     * Caller must be a verified ride participant.
     */
    suspend fun getRatingsByRide(rideId: String): IshaaraResult<List<Rating>>

    /**
     * Retrieves the aggregate rating summary for the authenticated driver.
     */
    suspend fun getMyRatingSummary(): IshaaraResult<DriverRatingSummary>
}
