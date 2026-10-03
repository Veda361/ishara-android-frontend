package com.ishara.app.domain.repository

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.Agency
import com.ishara.app.domain.model.DriverAgencyMembership
import com.ishara.app.domain.model.DriverMembershipState
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface governing DRIVER_CONDUCTOR agency affiliations,
 * discovery of active transport agencies, and membership state management.
 */
interface AgencyMembershipRepository {

    /**
     * Resolves driver's currently active (APPROVED) or latest PENDING membership from backend.
     */
    suspend fun getCurrentMembership(): IshaaraResult<DriverAgencyMembership?>

    /**
     * Observes driver's current authoritative membership.
     */
    fun observeCurrentMembership(): StateFlow<DriverAgencyMembership?>

    /**
     * Observes driver's high-level membership state machine.
     */
    fun observeMembershipState(): StateFlow<DriverMembershipState>

    /**
     * Discovers active transport agencies with optional keyword/city filters.
     */
    suspend fun listAgencies(search: String? = null, city: String? = null): IshaaraResult<List<Agency>>

    /**
     * Retrieves public profile for a specific agency.
     */
    suspend fun getAgency(agencyId: String): IshaaraResult<Agency>

    /**
     * Submits an affiliation request to an active agency.
     */
    suspend fun requestMembership(agencyId: String, notes: String? = null): IshaaraResult<DriverAgencyMembership>

    /**
     * Cancels a driver's pending membership request.
     */
    suspend fun cancelMembership(membershipId: String): IshaaraResult<Unit>

    /**
     * Refreshes the driver's current membership status against backend.
     */
    suspend fun refreshMembership(): IshaaraResult<DriverAgencyMembership?>

    /**
     * Flushes transient and cached agency membership state.
     * Invoked during sign-out and account-switching.
     */
    fun clearMembershipState()
}
