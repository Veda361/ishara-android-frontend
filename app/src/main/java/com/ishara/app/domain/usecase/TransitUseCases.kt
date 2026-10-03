package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.TransitRoute
import com.ishara.app.domain.repository.TransitRepository
import com.ishara.app.domain.repository.TransitResource
import kotlinx.coroutines.flow.Flow

/**
 * UseCase to retrieve and observe transit routes with stale-while-revalidate offline support.
 */
class GetTransitRoutesUseCase(
    private val transitRepository: TransitRepository
) {
    operator fun invoke(forceRefresh: Boolean = false): Flow<TransitResource<List<TransitRoute>>> {
        return transitRepository.getRoutes(forceRefresh = forceRefresh)
    }
}

/**
 * UseCase to retrieve details for a specific transit route by ID.
 */
class GetRouteDetailsUseCase(
    private val transitRepository: TransitRepository
) {
    operator fun invoke(routeId: String, forceRefresh: Boolean = false): Flow<TransitResource<TransitRoute>> {
        return transitRepository.getRouteById(routeId = routeId, forceRefresh = forceRefresh)
    }
}

/**
 * UseCase to explicitly trigger a cache synchronization refresh with the backend.
 */
class RefreshTransitRoutesUseCase(
    private val transitRepository: TransitRepository
) {
    suspend operator fun invoke(): IshaaraResult<List<TransitRoute>> {
        return transitRepository.refreshRoutes()
    }
}
