package com.ishara.app.feature.driver.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.common.IshaaraLogger
import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.DriverEmergencyContact
import com.ishara.app.domain.usecase.CreateDriverProfileUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Production ViewModel managing Driver Onboarding and Profile Submission.
 * Strictly decoupled from passenger workflows and operates on top of verified backend contracts.
 */
class DriverOnboardingViewModel(
    private val createDriverProfileUseCase: CreateDriverProfileUseCase,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = externalScope ?: viewModelScope
    private val _uiState = MutableStateFlow(DriverOnboardingUiState())
    val uiState: StateFlow<DriverOnboardingUiState> = _uiState.asStateFlow()

    fun onLicenseNumberChanged(license: String) {
        val uppercase = license.uppercase().filter { it.isLetterOrDigit() || it == '-' }
        val error = when {
            uppercase.isBlank() -> "License number is required"
            uppercase.length < 3 -> "License number must be at least 3 characters"
            uppercase.length > 30 -> "License number cannot exceed 30 characters"
            else -> null
        }
        _uiState.update {
            it.copy(
                licenseNumber = uppercase,
                licenseNumberError = error,
                errorMessage = null
            )
        }
    }

    fun onYearsOfExperienceChanged(yearsStr: String) {
        val digits = yearsStr.filter { it.isDigit() }
        val yearsInt = digits.toIntOrNull()
        val error = when {
            digits.isNotBlank() && (yearsInt == null || yearsInt !in 0..60) ->
                "Years of experience must be between 0 and 60"
            else -> null
        }
        _uiState.update {
            it.copy(
                yearsOfExperience = digits,
                yearsOfExperienceError = error,
                errorMessage = null
            )
        }
    }

    fun onEmergencyContactNameChanged(name: String) {
        _uiState.update {
            it.copy(
                emergencyContactName = name,
                errorMessage = null
            )
        }
    }

    fun onEmergencyContactPhoneChanged(phone: String) {
        val trimmed = phone.trim()
        val error = when {
            trimmed.isNotBlank() && !Regex("^\\+?[1-9]\\d{7,14}\$").matches(trimmed) ->
                "Enter valid phone number (8-15 digits, optional +)"
            else -> null
        }
        _uiState.update {
            it.copy(
                emergencyContactPhone = trimmed,
                emergencyPhoneError = error,
                errorMessage = null
            )
        }
    }

    fun onEmergencyContactRelationshipChanged(relationship: String) {
        _uiState.update {
            it.copy(
                emergencyContactRelationship = relationship,
                errorMessage = null
            )
        }
    }

    fun selectOperatingType(choice: DriverOperatingTypeChoice) {
        IshaaraLogger.i("ISHAARA_ONBOARDING", "selection=${choice.name}")
        _uiState.update {
            it.copy(
                selectedOperatingType = choice,
                operatingTypeError = null,
                errorMessage = null
            )
        }
    }

    fun onOperatingTypeChanged(type: String?) {
        val choice = when (type?.uppercase()) {
            "AGENCY" -> DriverOperatingTypeChoice.AGENCY
            "INDIVIDUAL" -> DriverOperatingTypeChoice.INDIVIDUAL
            else -> DriverOperatingTypeChoice.NONE
        }
        selectOperatingType(choice)
    }

    fun submitOnboarding(onSuccess: (() -> Unit)? = null) {
        val state = _uiState.value
        if (!state.isOperatingTypeSelected) {
            _uiState.update {
                it.copy(
                    operatingTypeError = "Please choose an operating type (Individual or Agency/Fleet).",
                    errorMessage = "Please choose an operating type (Individual or Agency/Fleet)."
                )
            }
            return
        }
        if (!state.isFormValid) {
            onLicenseNumberChanged(state.licenseNumber)
            return
        }

        val years = state.yearsOfExperience.toIntOrNull()
        val emergencyContact = if (state.emergencyContactName.isNotBlank() && state.emergencyContactPhone.isNotBlank()) {
            DriverEmergencyContact(
                name = state.emergencyContactName.trim(),
                phoneNumber = state.emergencyContactPhone.trim(),
                relationship = state.emergencyContactRelationship.trim().ifEmpty { null }
            )
        } else null

        val chosenType = state.operatingTypeString
        IshaaraLogger.i("ISHAARA_ONBOARDING", "Submitting driver profile with operatingType=$chosenType")

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        scope.launch(dispatchers.io) {
            val result = createDriverProfileUseCase(
                licenseNumber = state.licenseNumber.trim(),
                yearsOfExperience = years,
                emergencyContact = emergencyContact,
                operatingType = chosenType
            )

            when (result) {
                is IshaaraResult.Success -> {
                    IshaaraLogger.i(TAG, "Driver onboarding successful: id=${result.data.id}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSuccess = true,
                            createdProfile = result.data,
                            errorMessage = null
                        )
                    }
                    onSuccess?.invoke()
                }
                is IshaaraResult.Failure -> {
                    val userMessage = when (val err = result.error) {
                        is IshaaraError.Conflict -> "Driver profile already exists. Updating local session..."
                        is IshaaraError.Validation -> err.message ?: "Please review your inputs."
                        is IshaaraError.Authentication -> "Authentication session expired. Please sign in again."
                        is IshaaraError.Forbidden -> "Access denied: Driver permissions required."
                        is IshaaraError.Network -> "Network unavailable. Please check your internet connection."
                        is IshaaraError.Server -> "Server error. Please try again shortly."
                        else -> err.message ?: "An unexpected error occurred during driver registration."
                    }
                    IshaaraLogger.w(TAG, "Driver onboarding failed: $userMessage")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = userMessage
                        )
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    companion object {
        private const val TAG = "DriverOnboardingViewModel"
    }
}
