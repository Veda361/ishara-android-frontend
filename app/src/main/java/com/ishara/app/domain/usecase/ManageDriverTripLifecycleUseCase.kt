package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverActiveTrip
import com.ishara.app.domain.repository.DriverRepository

/**
 * Use case to control the lifecycle of the driver's current operational trip
 * (Start, Complete, Cancel).
 */
class ManageDriverTripLifecycleUseCase(
    private val driverRepository: DriverRepository
) {
    private val objectIdRegex = Regex("^[0-9a-fA-F]{24}$")

    suspend fun startTrip(tripId: String): IshaaraResult<DriverActiveTrip> {
        if (!tripId.matches(objectIdRegex)) {
            return IshaaraResult.failure(
                IshaaraError.Validation("tripId", "Invalid trip identifier format.")
            )
        }
        return driverRepository.startTrip(tripId)
    }

    suspend fun completeTrip(tripId: String): IshaaraResult<DriverActiveTrip> {
        if (!tripId.matches(objectIdRegex)) {
            return IshaaraResult.failure(
                IshaaraError.Validation("tripId", "Invalid trip identifier format.")
            )
        }
        return driverRepository.completeTrip(tripId)
    }

    suspend fun cancelTrip(tripId: String): IshaaraResult<DriverActiveTrip> {
        if (!tripId.matches(objectIdRegex)) {
            return IshaaraResult.failure(
                IshaaraError.Validation("tripId", "Invalid trip identifier format.")
            )
        }
        return driverRepository.cancelTrip(tripId)
    }
}
