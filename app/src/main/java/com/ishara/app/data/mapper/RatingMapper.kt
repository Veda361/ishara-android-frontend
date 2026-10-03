package com.ishara.app.data.mapper

import com.ishara.app.data.remote.dto.DriverRatingSummaryResponseDto
import com.ishara.app.data.remote.dto.RatingEligibilityResponseDto
import com.ishara.app.data.remote.dto.RatingResponseDto
import com.ishara.app.domain.model.DriverRatingSummary
import com.ishara.app.domain.model.Rating
import com.ishara.app.domain.model.RatingEligibility

/**
 * Mapper between Remote DTOs and Domain Models for Ratings.
 */
object RatingMapper {

    fun toDomain(dto: RatingResponseDto): Rating {
        return Rating(
            id = dto.id,
            rideId = dto.rideId,
            score = dto.score,
            review = dto.review,
            createdAt = dto.createdAt
        )
    }

    fun toDomain(dto: RatingEligibilityResponseDto): RatingEligibility {
        return RatingEligibility(
            eligible = dto.eligible,
            alreadyRated = dto.alreadyRated,
            reason = dto.reason
        )
    }

    fun toDomain(dto: DriverRatingSummaryResponseDto): DriverRatingSummary {
        return DriverRatingSummary(
            driverId = dto.driverId,
            averageScore = dto.averageScore,
            ratingCount = dto.ratingCount
        )
    }
}
