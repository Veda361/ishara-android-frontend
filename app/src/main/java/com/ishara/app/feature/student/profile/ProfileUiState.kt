package com.ishara.app.feature.student.profile

import com.ishara.app.domain.model.UserProfile

/**
 * Coherent UI state for the Student Profile screen.
 * Follows Unidirectional Data Flow (UDF) and avoids contradictory flags.
 */
data class ProfileUiState(
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val isSaving: Boolean = false,
    val userProfile: UserProfile? = null,
    val editName: String = "",
    val editPhone: String = "",
    val nameError: String? = null,
    val phoneError: String? = null,
    val userFacingError: String? = null,
    val successMessage: String? = null
) {
    val canSave: Boolean
        get() = isEditing && !isSaving && editName.isNotBlank() && nameError == null && phoneError == null
}
