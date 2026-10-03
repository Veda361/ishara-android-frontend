package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.Agency
import com.ishara.app.domain.model.DriverAgencyMembership
import com.ishara.app.domain.model.DriverMembershipState
import com.ishara.app.domain.repository.AgencyMembershipRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Resolves current driver agency membership from authoritative backend.
 */
class GetCurrentAgencyMembershipUseCase(
    private val repository: AgencyMembershipRepository
) {
    suspend operator fun invoke(): IshaaraResult<DriverAgencyMembership?> {
        return repository.getCurrentMembership()
    }
}

/**
 * Observes the driver's membership state machine.
 */
class ObserveAgencyMembershipStateUseCase(
    private val repository: AgencyMembershipRepository
) {
    operator fun invoke(): StateFlow<DriverMembershipState> {
        return repository.observeMembershipState()
    }
}

/**
 * Observes the driver's current membership entity.
 */
class ObserveCurrentAgencyMembershipUseCase(
    private val repository: AgencyMembershipRepository
) {
    operator fun invoke(): StateFlow<DriverAgencyMembership?> {
        return repository.observeCurrentMembership()
    }
}

/**
 * Discovers active transport agencies available for driver affiliation.
 */
class ListAgenciesUseCase(
    private val repository: AgencyMembershipRepository
) {
    suspend operator fun invoke(search: String? = null, city: String? = null): IshaaraResult<List<Agency>> {
        return repository.listAgencies(search, city)
    }
}

/**
 * Retrieves public profile details for a specific agency.
 */
class GetAgencyDetailsUseCase(
    private val repository: AgencyMembershipRepository
) {
    suspend operator fun invoke(agencyId: String): IshaaraResult<Agency> {
        return repository.getAgency(agencyId)
    }
}

/**
 * Submits an agency membership request.
 */
class RequestAgencyMembershipUseCase(
    private val repository: AgencyMembershipRepository
) {
    suspend operator fun invoke(agencyId: String, notes: String? = null): IshaaraResult<DriverAgencyMembership> {
        return repository.requestMembership(agencyId, notes)
    }
}

/**
 * Cancels a driver's pending agency membership request.
 */
class CancelAgencyMembershipUseCase(
    private val repository: AgencyMembershipRepository
) {
    suspend operator fun invoke(membershipId: String): IshaaraResult<Unit> {
        return repository.cancelMembership(membershipId)
    }
}

/**
 * Refreshes driver's agency membership state from the backend.
 */
class RefreshAgencyMembershipUseCase(
    private val repository: AgencyMembershipRepository
) {
    suspend operator fun invoke(): IshaaraResult<DriverAgencyMembership?> {
        return repository.refreshMembership()
    }
}

/**
 * Clears cached and transient agency membership state on logout or account switch.
 */
class ClearAgencyMembershipStateUseCase(
    private val repository: AgencyMembershipRepository
) {
    operator fun invoke() {
        repository.clearMembershipState()
    }
}
