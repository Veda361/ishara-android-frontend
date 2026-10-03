package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.RideFare
import com.ishara.app.domain.repository.FareRepository

/**
 * Use case retrieving authoritative fare calculation or immutable billing snapshot for a ride.
 * Before ride completion, returns estimated fare.
 * Upon completion, returns the immutable billing snapshot.
 */
class GetRideFareUseCase(
    private val fareRepository: FareRepository
) {
    suspend operator fun invoke(rideId: String): IshaaraResult<RideFare> {
        return fareRepository.getRideFare(rideId)
    }
}
