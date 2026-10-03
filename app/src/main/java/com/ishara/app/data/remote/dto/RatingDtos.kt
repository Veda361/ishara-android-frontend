package com.ishara.app.data.remote.dto

/**
 * Request payload for POST /api/v1/rides/:rideId/ratings.
 * Zod schema is strictly validated server-side.
 * Client MUST NOT supply reviewerUserId or revieweeUserId.
 */
data class SubmitRatingRequestDto(
    val score: Int,
    val review: String? = null
)

/**
 * Public response DTO for a submitted rating.
 */
data class RatingResponseDto(
    val id: String,
    val rideId: String,
    val score: Int,
    val review: String? = null,
    val createdAt: String
)

/**
 * Response DTO for rating eligibility check.
 */
data class RatingEligibilityResponseDto(
    val eligible: Boolean,
    val alreadyRated: Boolean,
    val reason: String? = null
)

/**
 * Response DTO for driver aggregate rating summary.
 */
data class DriverRatingSummaryResponseDto(
    val driverId: String,
    val averageScore: Double? = null,
    val ratingCount: Int
)
