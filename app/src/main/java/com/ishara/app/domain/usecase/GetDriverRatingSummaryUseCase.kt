package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverRatingSummary
import com.ishara.app.domain.repository.RatingRepository

/**
 * Use case to retrieve the aggregate rating summary for the authenticated driver.
 */
class GetDriverRatingSummaryUseCase(
    private val ratingRepository: RatingRepository
) {
    suspend operator fun invoke(): IshaaraResult<DriverRatingSummary> {
        return ratingRepository.getMyRatingSummary()
    }
}
