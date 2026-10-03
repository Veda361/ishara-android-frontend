package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.SearchResultLocation
import com.ishara.app.domain.repository.LocationRepository

/**
 * Use case to search for locations matching a text query.
 * Validates search constraints:
 * - Query must be at least 2 characters (trimmed)
 * - Maximum query length 100 characters
 * - Result limit constrained between 1 and 10
 */
class SearchLocationsUseCase(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(
        query: String,
        latitude: Double? = null,
        longitude: Double? = null,
        radius: Double? = null,
        limit: Int = 5
    ): IshaaraResult<List<SearchResultLocation>> {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            return IshaaraResult.failure(
                IshaaraError.Validation(message = "Search query must be at least 2 characters.")
            )
        }
        if (trimmed.length > 100) {
            return IshaaraResult.failure(
                IshaaraError.Validation(message = "Search query cannot exceed 100 characters.")
            )
        }

        val clampedLimit = limit.coerceIn(1, 10)

        return locationRepository.searchLocations(
            query = trimmed,
            latitude = latitude,
            longitude = longitude,
            radius = radius,
            limit = clampedLimit
        )
    }
}
