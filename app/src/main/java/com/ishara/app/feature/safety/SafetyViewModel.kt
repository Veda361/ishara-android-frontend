package com.ishara.app.feature.safety

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.EmergencyContactRelationship
import com.ishara.app.domain.usecase.CancelSosUseCase
import com.ishara.app.domain.usecase.CreateEmergencyContactUseCase
import com.ishara.app.domain.usecase.DeleteEmergencyContactUseCase
import com.ishara.app.domain.usecase.GetActiveSosUseCase
import com.ishara.app.domain.usecase.GetEmergencyContactsUseCase
import com.ishara.app.domain.usecase.TriggerSosUseCase
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Production ViewModel managing the Safety & SOS emergency lifecycle.
 * Coordinates reconciliation, intentional confirmation, deduplication, and failure recovery.
 */
class SafetyViewModel(
    val rideId: String,
    private val triggerSosUseCase: TriggerSosUseCase,
    private val getActiveSosUseCase: GetActiveSosUseCase,
    private val cancelSosUseCase: CancelSosUseCase,
    private val getEmergencyContactsUseCase: GetEmergencyContactsUseCase,
    private val createEmergencyContactUseCase: CreateEmergencyContactUseCase? = null,
    private val deleteEmergencyContactUseCase: DeleteEmergencyContactUseCase? = null,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider()
) : ViewModel() {

    private val tag = "SafetyViewModel"

    private val _uiState = MutableStateFlow<SafetyUiState>(SafetyUiState.Loading)
    val uiState: StateFlow<SafetyUiState> = _uiState.asStateFlow()

    init {
        loadInitialState()
    }

    fun loadInitialState() {
        viewModelScope.launch(dispatchers.main) {
            _uiState.value = SafetyUiState.Loading

            // 1. Reconcile active SOS for this ride
            val activeSosResult = getActiveSosUseCase(rideId)
            val activeEvent = when (activeSosResult) {
                is IshaaraResult.Success -> activeSosResult.data
                is IshaaraResult.Failure -> {
                    IshaaraLogger.w(tag, "Failed to check active SOS status: ${activeSosResult.error}")
                    null
                }
            }

            // 2. Load emergency contacts
            val contactsResult = getEmergencyContactsUseCase()
            val contacts = when (contactsResult) {
                is IshaaraResult.Success -> contactsResult.data
                is IshaaraResult.Failure -> {
                    IshaaraLogger.w(tag, "Failed to load emergency contacts: ${contactsResult.error}")
                    emptyList()
                }
            }

            _uiState.value = SafetyUiState.Content(
                rideId = rideId,
                activeEvent = activeEvent,
                emergencyContacts = contacts,
                isLoadingContacts = false
            )
        }
    }

    fun onOpenConfirmationDialog() {
        updateContent { it.copy(showConfirmationDialog = true, errorMessage = null) }
    }

    fun onDismissConfirmationDialog() {
        updateContent { it.copy(showConfirmationDialog = false) }
    }

    fun onConfirmSos() {
        val currentContent = (_uiState.value as? SafetyUiState.Content) ?: return
        if (currentContent.isSubmitting) {
            // Guard against duplicate taps
            return
        }

        viewModelScope.launch(dispatchers.main) {
            updateContent { it.copy(isSubmitting = true, showConfirmationDialog = false, errorMessage = null) }

            when (val result = triggerSosUseCase(rideId = rideId)) {
                is IshaaraResult.Success -> {
                    val event = result.data
                    IshaaraLogger.i(tag, "SOS triggered successfully: eventId=${event.eventId}")
                    updateContent {
                        it.copy(
                            isSubmitting = false,
                            activeEvent = event,
                            bannerMessage = "Safety alert sent. Emergency record created."
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    handleSosTriggerFailure(result.error)
                }
            }
        }
    }

    private suspend fun handleSosTriggerFailure(error: IshaaraError) {
        val isConflict = (error is IshaaraError.Server && (error.code == 409 || error.message.contains("SOS_ALREADY_ACTIVE")))
        if (isConflict) {
            // Reconcile authoritative state from backend
            when (val reconcileResult = getActiveSosUseCase(rideId)) {
                is IshaaraResult.Success -> {
                    updateContent {
                        it.copy(
                            isSubmitting = false,
                            activeEvent = reconcileResult.data,
                            bannerMessage = "A safety alert is already active for this ride."
                        )
                    }
                    return
                }
                is IshaaraResult.Failure -> Unit
            }
        }

        val message = when {
            error is IshaaraError.Network -> "You're offline. We couldn't send the safety alert. Check connection."
            error is IshaaraError.Authentication -> "Authentication session expired. Please sign in again."
            error is IshaaraError.Forbidden -> "You are not authorized to trigger safety actions for this ride."
            error is IshaaraError.Server && error.code == 400 -> "Safety alerts can only be sent for active rides."
            else -> "We couldn't send the safety alert. Please try again."
        }

        updateContent {
            it.copy(
                isSubmitting = false,
                errorMessage = message
            )
        }
    }

    fun onOpenCancelDialog() {
        updateContent { it.copy(showCancelDialog = true, errorMessage = null) }
    }

    fun onDismissCancelDialog() {
        updateContent { it.copy(showCancelDialog = false) }
    }

    fun onCancellationReasonChanged(reason: String) {
        updateContent { it.copy(cancellationReason = reason) }
    }

    fun onConfirmCancel() {
        val currentContent = (_uiState.value as? SafetyUiState.Content) ?: return
        if (currentContent.isCancelling) return

        viewModelScope.launch(dispatchers.main) {
            updateContent { it.copy(isCancelling = true, showCancelDialog = false, errorMessage = null) }

            val reason = currentContent.cancellationReason.takeIf { it.isNotBlank() }
            when (val result = cancelSosUseCase.cancelByRide(rideId, reason)) {
                is IshaaraResult.Success -> {
                    val event = result.data
                    IshaaraLogger.i(tag, "SOS cancelled successfully: eventId=${event.eventId}")
                    updateContent {
                        it.copy(
                            isCancelling = false,
                            activeEvent = event,
                            cancellationReason = "",
                            bannerMessage = "Safety alert has been cancelled."
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    val message = when (val err = result.error) {
                        is IshaaraError.Network -> "You're offline. Unable to cancel safety alert."
                        is IshaaraError.Forbidden -> "Only the person who sent this alert can cancel it."
                        else -> "Unable to cancel safety alert. Please check your connection."
                    }
                    updateContent {
                        it.copy(
                            isCancelling = false,
                            errorMessage = message
                        )
                    }
                }
            }
        }
    }

    fun onDismissBanner() {
        updateContent { it.copy(bannerMessage = null) }
    }

    fun onDismissError() {
        updateContent { it.copy(errorMessage = null) }
    }

    fun onOpenAddContactDialog() {
        updateContent { it.copy(showAddContactDialog = true, contactErrorMessage = null) }
    }

    fun onDismissAddContactDialog() {
        updateContent { it.copy(showAddContactDialog = false, contactErrorMessage = null) }
    }

    fun onAddContact(
        name: String,
        phoneNumber: String,
        relationship: EmergencyContactRelationship
    ) {
        val currentContent = (_uiState.value as? SafetyUiState.Content) ?: return
        if (currentContent.isAddingContact) return

        val trimmedName = name.trim()
        val trimmedPhone = phoneNumber.trim().replace("\\s+".toRegex(), "")

        if (trimmedName.isBlank()) {
            updateContent { it.copy(contactErrorMessage = "Name is required") }
            return
        }

        if (!trimmedPhone.matches("^\\+?[0-9]{7,15}$".toRegex())) {
            updateContent { it.copy(contactErrorMessage = "Phone number must be 7–15 digits (optional leading +)") }
            return
        }

        if (currentContent.emergencyContacts.size >= 5) {
            updateContent { it.copy(contactErrorMessage = "Maximum limit of 5 emergency contacts reached.") }
            return
        }

        val useCase = createEmergencyContactUseCase ?: return

        viewModelScope.launch(dispatchers.main) {
            updateContent { it.copy(isAddingContact = true, contactErrorMessage = null) }

            when (val result = useCase(trimmedName, trimmedPhone, relationship)) {
                is IshaaraResult.Success -> {
                    IshaaraLogger.i(tag, "Emergency contact added: ${result.data.id}")
                    val updatedContacts = listOf(result.data) + currentContent.emergencyContacts
                    updateContent {
                        it.copy(
                            isAddingContact = false,
                            showAddContactDialog = false,
                            emergencyContacts = updatedContacts,
                            bannerMessage = "Emergency contact added successfully."
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    IshaaraLogger.w(tag, "Failed to create emergency contact: ${result.error}")
                    val msg = when (val err = result.error) {
                        is IshaaraError.Server -> {
                            if (err.code == 409 || err.message.contains("EMERGENCY_CONTACT_LIMIT_REACHED")) {
                                "Maximum limit of 5 emergency contacts reached."
                            } else {
                                err.message.ifBlank { "Failed to add emergency contact." }
                            }
                        }
                        is IshaaraError.Network -> "Network unavailable. Please check your connection."
                        else -> "Failed to add emergency contact. Please try again."
                    }
                    updateContent { it.copy(isAddingContact = false, contactErrorMessage = msg) }
                }
            }
        }
    }

    fun onDeleteContact(contactId: String) {
        val currentContent = (_uiState.value as? SafetyUiState.Content) ?: return
        if (currentContent.isDeletingContactId != null) return

        val useCase = deleteEmergencyContactUseCase ?: return

        viewModelScope.launch(dispatchers.main) {
            updateContent { it.copy(isDeletingContactId = contactId) }

            when (val result = useCase(contactId)) {
                is IshaaraResult.Success -> {
                    IshaaraLogger.i(tag, "Emergency contact deleted: $contactId")
                    val updated = currentContent.emergencyContacts.filter { it.id != contactId }
                    updateContent {
                        it.copy(
                            isDeletingContactId = null,
                            emergencyContacts = updated,
                            bannerMessage = "Emergency contact removed."
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    IshaaraLogger.w(tag, "Failed to delete emergency contact: ${result.error}")
                    updateContent {
                        it.copy(
                            isDeletingContactId = null,
                            errorMessage = "Failed to remove emergency contact."
                        )
                    }
                }
            }
        }
    }

    fun onNavigateBack() {
        navigationManager.navigateUp()
    }

    private inline fun updateContent(transform: (SafetyUiState.Content) -> SafetyUiState.Content) {
        _uiState.update { state ->
            if (state is SafetyUiState.Content) transform(state) else state
        }
    }
}
