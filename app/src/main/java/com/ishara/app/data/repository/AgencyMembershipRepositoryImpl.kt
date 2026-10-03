package com.ishara.app.data.repository

import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.core.storage.SessionStore
import com.ishara.app.data.mapper.AgencyMapper
import com.ishara.app.data.remote.datasource.AgencyRemoteDataSource
import com.ishara.app.domain.model.Agency
import com.ishara.app.domain.model.AgencyMembershipStatus
import com.ishara.app.domain.model.DriverAgencyMembership
import com.ishara.app.domain.model.DriverMembershipState
import com.ishara.app.domain.repository.AgencyMembershipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Production implementation of AgencyMembershipRepository.
 * Manages driver agency affiliations with centralized state flows,
 * conflict reconciliation (409), and clean account-switching resets.
 */
class AgencyMembershipRepositoryImpl(
    private val remoteDataSource: AgencyRemoteDataSource,
    private val sessionStore: SessionStore
) : AgencyMembershipRepository {

    private val mutex = Mutex()

    private val _currentMembershipFlow = MutableStateFlow<DriverAgencyMembership?>(null)
    override fun observeCurrentMembership(): StateFlow<DriverAgencyMembership?> =
        _currentMembershipFlow.asStateFlow()

    private val _membershipStateFlow = MutableStateFlow<DriverMembershipState>(DriverMembershipState.Loading)
    override fun observeMembershipState(): StateFlow<DriverMembershipState> =
        _membershipStateFlow.asStateFlow()

    override suspend fun getCurrentMembership(): IshaaraResult<DriverAgencyMembership?> = mutex.withLock {
        val session = sessionStore.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "Authentication required."))

        val result = remoteDataSource.getCurrentDriverMembership(session.token)
        return when (result) {
            is IshaaraResult.Success -> {
                val dto = result.data
                if (dto == null) {
                    _currentMembershipFlow.value = null
                    _membershipStateFlow.value = DriverMembershipState.NoMembership
                    IshaaraResult.success(null)
                } else {
                    val domainMembership = AgencyMapper.toDomain(dto)
                    _currentMembershipFlow.value = domainMembership
                    _membershipStateFlow.value = when (domainMembership.status) {
                        AgencyMembershipStatus.APPROVED -> DriverMembershipState.Approved(domainMembership)
                        AgencyMembershipStatus.PENDING -> DriverMembershipState.Pending(domainMembership)
                        AgencyMembershipStatus.REJECTED -> DriverMembershipState.Rejected(domainMembership)
                    }
                    IshaaraResult.success(domainMembership)
                }
            }
            is IshaaraResult.Failure -> {
                IshaaraLogger.w(TAG, "Failed to resolve driver agency membership: ${result.error.message}")
                _membershipStateFlow.value = DriverMembershipState.Error(result.error)
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun listAgencies(search: String?, city: String?): IshaaraResult<List<Agency>> {
        val result = remoteDataSource.listAgencies(search = search, city = city)
        return when (result) {
            is IshaaraResult.Success -> {
                val domainAgencies = result.data.map { AgencyMapper.toDomain(it) }
                IshaaraResult.success(domainAgencies)
            }
            is IshaaraResult.Failure -> {
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun getAgency(agencyId: String): IshaaraResult<Agency> {
        val result = remoteDataSource.getAgencyById(agencyId)
        return when (result) {
            is IshaaraResult.Success -> {
                IshaaraResult.success(AgencyMapper.toDomain(result.data))
            }
            is IshaaraResult.Failure -> {
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun requestMembership(
        agencyId: String,
        notes: String?
    ): IshaaraResult<DriverAgencyMembership> = mutex.withLock {
        val session = sessionStore.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "Authentication required."))

        val result = remoteDataSource.requestMembership(agencyId, notes, session.token)
        return when (result) {
            is IshaaraResult.Success -> {
                val domainMembership = AgencyMapper.toDomain(result.data)
                _currentMembershipFlow.value = domainMembership
                _membershipStateFlow.value = DriverMembershipState.Pending(domainMembership)
                IshaaraLogger.i(TAG, "Driver submitted membership request for agency $agencyId")
                IshaaraResult.success(domainMembership)
            }
            is IshaaraResult.Failure -> {
                if (result.error is IshaaraError.Conflict) {
                    IshaaraLogger.w(TAG, "Membership request conflict (409): reconciling with backend state.")
                    // Reconcile by querying authoritative backend membership
                    val reconcileResult = remoteDataSource.getCurrentDriverMembership(session.token)
                    if (reconcileResult is IshaaraResult.Success && reconcileResult.data != null) {
                        val reSynced = AgencyMapper.toDomain(reconcileResult.data)
                        _currentMembershipFlow.value = reSynced
                        _membershipStateFlow.value = when (reSynced.status) {
                            AgencyMembershipStatus.APPROVED -> DriverMembershipState.Approved(reSynced)
                            AgencyMembershipStatus.PENDING -> DriverMembershipState.Pending(reSynced)
                            AgencyMembershipStatus.REJECTED -> DriverMembershipState.Rejected(reSynced)
                        }
                    }
                }
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun cancelMembership(membershipId: String): IshaaraResult<Unit> = mutex.withLock {
        val session = sessionStore.getSession()
            ?: return IshaaraResult.failure(IshaaraError.Authentication(message = "Authentication required."))

        val result = remoteDataSource.cancelMembership(membershipId, session.token)
        return when (result) {
            is IshaaraResult.Success -> {
                _currentMembershipFlow.value = null
                _membershipStateFlow.value = DriverMembershipState.NoMembership
                IshaaraLogger.i(TAG, "Driver cancelled pending membership request $membershipId")
                IshaaraResult.success(Unit)
            }
            is IshaaraResult.Failure -> {
                IshaaraLogger.w(TAG, "Failed to cancel membership request: ${result.error.message}")
                IshaaraResult.failure(result.error)
            }
        }
    }

    override suspend fun refreshMembership(): IshaaraResult<DriverAgencyMembership?> {
        return getCurrentMembership()
    }

    override fun clearMembershipState() {
        _currentMembershipFlow.value = null
        _membershipStateFlow.value = DriverMembershipState.Loading
        IshaaraLogger.i(TAG, "Flushed all cached driver agency membership state.")
    }

    companion object {
        private const val TAG = "AgencyMembershipRepo"
    }
}
