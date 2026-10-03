package com.ishara.app.feature.driver.agency

import com.ishara.app.domain.model.Agency
import com.ishara.app.domain.model.DriverAgencyMembership
import com.ishara.app.domain.model.DriverMembershipState

/**
 * UI State for Driver Agency Membership Screen.
 */
data class DriverAgencyMembershipUiState(
    val membershipState: DriverMembershipState = DriverMembershipState.Loading,
    val currentMembership: DriverAgencyMembership? = null,
    val availableAgencies: List<Agency> = emptyList(),
    val searchQuery: String = "",
    val selectedAgency: Agency? = null,
    val applicationNotes: String = "",
    val isLoadingAgencies: Boolean = false,
    val isSubmittingRequest: Boolean = false,
    val isCancellingRequest: Boolean = false,
    val isRefreshing: Boolean = false,
    val userFeedbackMessage: String? = null,
    val errorMessage: String? = null
)
