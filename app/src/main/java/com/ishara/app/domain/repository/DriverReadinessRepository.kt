package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverOperationalReadiness
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface governing DRIVER_CONDUCTOR operational readiness evaluation,
 * caching policies, and requirement status tracking.
 */
interface DriverReadinessRepository {

    /**
     * Retrieves authoritative operational readiness from backend.
     * When [forceRefresh] is true or cached state is absent, queries the backend API.
     */
    suspend fun getDriverReadiness(forceRefresh: Boolean = false): IshaaraResult<DriverOperationalReadiness>

    /**
     * Observes the cached driver operational readiness state reactively.
     */
    fun observeDriverReadiness(): StateFlow<DriverOperationalReadiness?>

    /**
     * Explicitly refreshes the authoritative readiness state against backend.
     */
    suspend fun refreshDriverReadiness(): IshaaraResult<DriverOperationalReadiness>

    /**
     * Clears cached readiness state.
     * Must be called during logout or account switching to prevent state leakage.
     */
    fun clearReadinessState()
}
