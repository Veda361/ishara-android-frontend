package com.ishara.app.feature.driver.agency

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.Agency
import com.ishara.app.domain.model.DriverMembershipState
import com.ishara.app.domain.usecase.CancelAgencyMembershipUseCase
import com.ishara.app.domain.usecase.GetCurrentAgencyMembershipUseCase
import com.ishara.app.domain.usecase.ListAgenciesUseCase
import com.ishara.app.domain.usecase.ObserveAgencyMembershipStateUseCase
import com.ishara.app.domain.usecase.ObserveCurrentAgencyMembershipUseCase
import com.ishara.app.domain.usecase.RefreshAgencyMembershipUseCase
import com.ishara.app.domain.usecase.RequestAgencyMembershipUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider

/**
 * ViewModel managing driver agency memberships, agency discovery,
 * application requests, and cancellation flows.
 */
class DriverAgencyMembershipViewModel(
    private val getCurrentAgencyMembershipUseCase: GetCurrentAgencyMembershipUseCase,
    private val observeAgencyMembershipStateUseCase: ObserveAgencyMembershipStateUseCase,
    private val observeCurrentAgencyMembershipUseCase: ObserveCurrentAgencyMembershipUseCase,
    private val listAgenciesUseCase: ListAgenciesUseCase,
    private val requestAgencyMembershipUseCase: RequestAgencyMembershipUseCase,
    private val cancelAgencyMembershipUseCase: CancelAgencyMembershipUseCase,
    private val refreshAgencyMembershipUseCase: RefreshAgencyMembershipUseCase,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DriverAgencyMembershipUiState())
    val uiState: StateFlow<DriverAgencyMembershipUiState> = _uiState.asStateFlow()

    init {
        observeMembershipState()
        observeCurrentMembership()
        refresh()
        loadAgencies()
    }

    private fun observeMembershipState() {
        viewModelScope.launch(dispatchers.main) {
            observeAgencyMembershipStateUseCase().collect { state ->
                _uiState.update { current ->
                    current.copy(membershipState = state)
                }
            }
        }
    }

    private fun observeCurrentMembership() {
        viewModelScope.launch(dispatchers.main) {
            observeCurrentAgencyMembershipUseCase().collect { membership ->
                _uiState.update { current ->
                    current.copy(currentMembership = membership)
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        loadAgencies(search = query)
    }

    fun onSelectAgency(agency: Agency?) {
        _uiState.update { it.copy(selectedAgency = agency, errorMessage = null) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(applicationNotes = notes) }
    }

    fun loadAgencies(search: String? = null, city: String? = null) {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(isLoadingAgencies = true) }
            val result = listAgenciesUseCase(search = search, city = city)
            when (result) {
                is IshaaraResult.Success -> {
                    _uiState.update {
                        it.copy(
                            availableAgencies = result.data,
                            isLoadingAgencies = false
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    IshaaraLogger.w(TAG, "Failed to load agencies: ${result.error.message}")
                    _uiState.update { it.copy(isLoadingAgencies = false) }
                }
            }
        }
    }

    /**
     * Submits an affiliation request to the selected agency with idempotency guard.
     */
    fun submitMembershipRequest() {
        val currentState = _uiState.value
        if (currentState.isSubmittingRequest) return

        val agency = currentState.selectedAgency
        if (agency == null) {
            _uiState.update { it.copy(errorMessage = "Please select an agency to request affiliation.") }
            return
        }

        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(isSubmittingRequest = true, errorMessage = null, userFeedbackMessage = null) }

            val result = requestAgencyMembershipUseCase(
                agencyId = agency.id,
                notes = currentState.applicationNotes.trim().ifEmpty { null }
            )

            when (result) {
                is IshaaraResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingRequest = false,
                            selectedAgency = null,
                            applicationNotes = "",
                            userFeedbackMessage = "Application submitted. Pending agency approval."
                        )
                    }
                    IshaaraLogger.i(TAG, "Membership request submitted successfully for agency ${agency.id}")
                }
                is IshaaraResult.Failure -> {
                    val friendlyMessage = when (result.error) {
                        is IshaaraError.Conflict -> "You already have an active or pending membership."
                        is IshaaraError.Forbidden -> "Access denied. Role 'DRIVER_CONDUCTOR' required."
                        is IshaaraError.Validation -> result.error.message
                        is IshaaraError.NotFound -> "Selected agency not found."
                        is IshaaraError.Network -> "Network connection failed. Please check your internet."
                        else -> result.error.message
                    }
                    _uiState.update {
                        it.copy(
                            isSubmittingRequest = false,
                            errorMessage = friendlyMessage
                        )
                    }
                }
            }
        }
    }

    /**
     * Cancels a pending membership request.
     */
    fun cancelPendingMembership() {
        val currentState = _uiState.value
        if (currentState.isCancellingRequest) return

        val membership = currentState.currentMembership
        if (membership == null) {
            _uiState.update { it.copy(errorMessage = "No active membership request to cancel.") }
            return
        }

        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(isCancellingRequest = true, errorMessage = null, userFeedbackMessage = null) }

            val result = cancelAgencyMembershipUseCase(membership.id)
            when (result) {
                is IshaaraResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCancellingRequest = false,
                            userFeedbackMessage = "Membership request cancelled."
                        )
                    }
                    loadAgencies()
                }
                is IshaaraResult.Failure -> {
                    val friendlyMessage = when (result.error) {
                        is IshaaraError.NotFound -> "Pending membership request not found."
                        is IshaaraError.Network -> "Network connection failed."
                        else -> result.error.message
                    }
                    _uiState.update {
                        it.copy(
                            isCancellingRequest = false,
                            errorMessage = friendlyMessage
                        )
                    }
                }
            }
        }
    }

    /**
     * Refreshes the driver's current membership status against backend.
     */
    fun refresh() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
            val result = refreshAgencyMembershipUseCase()
            if (result is IshaaraResult.Failure) {
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        errorMessage = if (it.currentMembership == null) result.error.message else null
                    )
                }
            } else {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(errorMessage = null, userFeedbackMessage = null) }
    }

    companion object {
        private const val TAG = "DriverAgencyVM"
    }
}
