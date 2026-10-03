package com.ishara.app.feature.student.discovery

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.domain.model.DiscoveredTrip
import com.ishara.app.domain.model.DiscoveryQuery
import com.ishara.app.domain.model.PassengerTripDetails

/**
 * State lifecycle stages for the Trip Discovery screen.
 */
sealed interface DiscoveryStage {
    object Idle : DiscoveryStage
    object Loading : DiscoveryStage
    object LoadingMore : DiscoveryStage
    object Success : DiscoveryStage
    object Empty : DiscoveryStage
    data class ValidationError(val message: String) : DiscoveryStage
    data class Error(val error: IshaaraError) : DiscoveryStage
}

/**
 * Single source of truth UI state for Student Trip Discovery.
 * Follows Unidirectional Data Flow (UDF).
 */
data class DiscoveryUiState(
    val query: DiscoveryQuery,
    val stage: DiscoveryStage = DiscoveryStage.Loading,
    val trips: List<DiscoveredTrip> = emptyList(),
    val selectedTrip: DiscoveredTrip? = null,
    val selectedTripDetails: PassengerTripDetails? = null,
    val isLoadingDetails: Boolean = false,
    val isDetailsSheetVisible: Boolean = false,
    val hasMore: Boolean = false,
    val nextCursor: String? = null,
    val errorMessage: String? = null,
    val validationError: String? = null
)
