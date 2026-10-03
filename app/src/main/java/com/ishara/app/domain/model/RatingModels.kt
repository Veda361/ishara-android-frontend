package com.ishara.app.domain.model

/**
 * Domain model representing a submitted rating.
 * Privacy-preserving: does not expose reviewer/reviewee internal IDs.
 */
data class Rating(
    val id: String,
    val rideId: String,
    val score: Int,
    val review: String?,
    val createdAt: String
)

/**
 * Domain model representing rating eligibility for a ride.
 */
data class RatingEligibility(
    val eligible: Boolean,
    val alreadyRated: Boolean,
    val reason: String? = null
)

/**
 * Domain model representing driver aggregate rating summary.
 * [averageScore] is null when driver has 0 ratings to avoid 0-star ambiguity.
 */
data class DriverRatingSummary(
    val driverId: String,
    val averageScore: Double?,
    val ratingCount: Int
) {
    val formattedScore: String
        get() = averageScore?.let { String.format("%.1f", it) } ?: "New"

    val displaySummary: String
        get() = if (ratingCount == 0) "No ratings yet" else "$formattedScore ★ ($ratingCount reviews)"
}
