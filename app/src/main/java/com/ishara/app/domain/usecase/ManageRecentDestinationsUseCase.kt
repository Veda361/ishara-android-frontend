package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.StudentDestination
import com.ishara.app.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Manages local, privacy-conscious storage of recent destination searches.
 */
class ManageRecentDestinationsUseCase(
    private val locationRepository: LocationRepository
) {
    fun getRecentDestinations(): Flow<List<StudentDestination>> {
        return locationRepository.getRecentDestinations()
    }

    suspend fun saveDestination(destination: StudentDestination): IshaaraResult<Unit> {
        return locationRepository.saveRecentDestination(destination)
    }

    suspend fun clearRecent(): IshaaraResult<Unit> {
        return locationRepository.clearRecentDestinations()
    }
}
