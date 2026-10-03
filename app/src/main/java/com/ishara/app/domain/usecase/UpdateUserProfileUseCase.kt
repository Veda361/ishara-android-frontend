package com.ishara.app.domain.usecase

import com.ishara.app.core.result.IshaaraError
import com.ishara.app.core.result.IshaaraResult
import com.ishara.app.domain.model.UserProfile
import com.ishara.app.domain.repository.UserRepository

/**
 * Domain use case to validate and update mutable user profile attributes (name, phoneNumber, image).
 * Strictly maps to PATCH /api/v1/users/me.
 */
class UpdateUserProfileUseCase(
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        name: String? = null,
        phoneNumber: String? = null,
        image: String? = null
    ): IshaaraResult<UserProfile> {
        val trimmedName = name?.trim()
        val trimmedPhone = phoneNumber?.trim()

        if (trimmedName != null && trimmedName.isEmpty()) {
            return IshaaraResult.failure(
                IshaaraError.Validation("name", "Name cannot be empty.")
            )
        }

        if (trimmedName != null && trimmedName.length > 100) {
            return IshaaraResult.failure(
                IshaaraError.Validation("name", "Name cannot exceed 100 characters.")
            )
        }

        if (trimmedPhone != null && trimmedPhone.isNotEmpty()) {
            val phoneRegex = Regex("^\\+?[0-9]{10,15}$")
            if (!phoneRegex.matches(trimmedPhone)) {
                return IshaaraResult.failure(
                    IshaaraError.Validation("phoneNumber", "Please enter a valid phone number (10-15 digits).")
                )
            }
        }

        return userRepository.updateUserProfile(
            name = trimmedName,
            phoneNumber = trimmedPhone,
            image = image?.trim()
        )
    }
}
