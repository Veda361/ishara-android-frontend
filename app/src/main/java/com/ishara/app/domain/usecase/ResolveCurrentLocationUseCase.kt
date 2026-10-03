package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.CurrentLocationDisplay
import com.ishara.app.domain.repository.LocationRepository

/**
 * Resolves current physical device position into a human-readable display model.
 * Prevents raw latitude/longitude exposure in user-facing UI.
 */
class ResolveCurrentLocationUseCase(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(): IshaaraResult<CurrentLocationDisplay> {
        return when (val locationResult = locationRepository.getCurrentLocation()) {
            is IshaaraResult.Success -> {
                locationRepository.resolveHumanReadableLocation(locationResult.data)
            }
            is IshaaraResult.Failure -> {
                IshaaraResult.failure(locationResult.error)
            }
        }
    }
}
