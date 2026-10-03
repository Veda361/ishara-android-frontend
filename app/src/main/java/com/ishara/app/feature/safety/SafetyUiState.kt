package com.ishara.app.feature.safety

import com.ishara.app.domain.model.EmergencyContact
import com.ishara.app.domain.model.EmergencyEvent

/**
 * Immutable UI state model for the Safety & SOS flow.
 * Ensures calm, accessible, and honest state representation.
 */
sealed interface SafetyUiState {

    /** Initial state loading active status reconciliation */
    object Loading : SafetyUiState

    /** Normal interactive state ready for SOS triggering or reviewing contacts */
    data class Content(
        val rideId: String,
        val activeEvent: EmergencyEvent? = null,
        val emergencyContacts: List<EmergencyContact> = emptyList(),
        val isLoadingContacts: Boolean = false,
        val isSubmitting: Boolean = false,
        val showConfirmationDialog: Boolean = false,
        val showCancelDialog: Boolean = false,
        val cancellationReason: String = "",
        val isCancelling: Boolean = false,
        val showAddContactDialog: Boolean = false,
        val isAddingContact: Boolean = false,
        val isDeletingContactId: String? = null,
        val contactErrorMessage: String? = null,
        val bannerMessage: String? = null,
        val errorMessage: String? = null
    ) : SafetyUiState {
        val hasActiveAlert: Boolean
            get() = activeEvent != null && !activeEvent.status.isTerminal
    }

    /** Unrecoverable or technical error requiring user action */
    data class Error(
        val message: String,
        val canRetry: Boolean = true,
        val isNetworkError: Boolean = false
    ) : SafetyUiState
}
