package com.ishara.app.feature.student.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ishara.app.core.common.DefaultDispatcherProvider
import com.ishara.app.core.common.DispatcherProvider
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.UserProfile
import com.ishara.app.domain.repository.UserRepository
import com.ishara.app.domain.usecase.GetCurrentUserProfileUseCase
import com.ishara.app.domain.usecase.UpdateUserProfileUseCase
import com.ishara.app.navigation.NavigationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel managing the Student / Passenger Profile screen.
 * Handles authoritative profile loading, validation, and editing via PATCH /api/v1/users/me.
 */
class ProfileViewModel(
    initialProfile: UserProfile? = null,
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val userRepository: UserRepository,
    private val navigationManager: NavigationManager,
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider(),
    externalScope: CoroutineScope? = null
) : ViewModel() {

    private val scope: CoroutineScope = externalScope ?: viewModelScope

    private val _uiState = MutableStateFlow(
        ProfileUiState(
            userProfile = initialProfile,
            editName = initialProfile?.name.orEmpty(),
            editPhone = initialProfile?.phoneNumber.orEmpty()
        )
    )
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeUserProfile()
        refreshProfile()
    }

    private fun observeUserProfile() {
        scope.launch(dispatchers.io) {
            userRepository.observeUserProfile().collect { profile ->
                if (profile != null) {
                    _uiState.update { current ->
                        current.copy(
                            userProfile = profile,
                            editName = if (!current.isEditing) profile.name else current.editName,
                            editPhone = if (!current.isEditing) profile.phoneNumber.orEmpty() else current.editPhone
                        )
                    }
                }
            }
        }
    }

    fun refreshProfile() {
        _uiState.update { it.copy(isLoading = true, userFacingError = null) }
        scope.launch(dispatchers.io) {
            when (val result = getCurrentUserProfileUseCase()) {
                is IshaaraResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userProfile = result.data,
                            userFacingError = null
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            userFacingError = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun startEditing() {
        val currentProfile = _uiState.value.userProfile
        _uiState.update {
            it.copy(
                isEditing = true,
                editName = currentProfile?.name.orEmpty(),
                editPhone = currentProfile?.phoneNumber.orEmpty(),
                nameError = null,
                phoneError = null,
                userFacingError = null,
                successMessage = null
            )
        }
    }

    fun cancelEditing() {
        val currentProfile = _uiState.value.userProfile
        _uiState.update {
            it.copy(
                isEditing = false,
                editName = currentProfile?.name.orEmpty(),
                editPhone = currentProfile?.phoneNumber.orEmpty(),
                nameError = null,
                phoneError = null
            )
        }
    }

    fun onNameChanged(name: String) {
        val error = when {
            name.isBlank() -> "Name cannot be empty"
            name.length > 100 -> "Name cannot exceed 100 characters"
            else -> null
        }
        _uiState.update { it.copy(editName = name, nameError = error) }
    }

    fun onPhoneChanged(phone: String) {
        val trimmed = phone.trim()
        val error = if (trimmed.isNotEmpty() && !Regex("^\\+?[0-9]{10,15}$").matches(trimmed)) {
            "Please enter a valid phone number (10-15 digits)"
        } else {
            null
        }
        _uiState.update { it.copy(editPhone = phone, phoneError = error) }
    }

    fun saveProfile() {
        val currentState = _uiState.value
        if (!currentState.canSave) return

        _uiState.update { it.copy(isSaving = true, userFacingError = null, successMessage = null) }

        scope.launch(dispatchers.io) {
            val result = updateUserProfileUseCase(
                name = currentState.editName,
                phoneNumber = currentState.editPhone.ifBlank { null }
            )

            when (result) {
                is IshaaraResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            isEditing = false,
                            userProfile = result.data,
                            successMessage = "Profile updated successfully.",
                            userFacingError = null
                        )
                    }
                }
                is IshaaraResult.Failure -> {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            userFacingError = result.error.message
                        )
                    }
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(userFacingError = null, successMessage = null) }
    }

    fun onNavigateBack() {
        navigationManager.navigateUp()
    }
}
