package com.ishara.app.data.mapper

import com.ishara.app.data.remote.dto.UserDto
import com.ishara.app.domain.model.UserProfile
import com.ishara.app.domain.model.UserRole

/**
 * Maps User DTO representations to strongly-typed UserProfile domain models.
 */
object UserMapper {

    fun toDomain(dto: UserDto): UserProfile {
        val mappedRole = UserRole.fromBackendString(dto.role)
        return UserProfile(
            id = dto.id,
            name = dto.name,
            email = dto.email,
            phoneNumber = dto.phoneNumber,
            role = mappedRole,
            profileImageUrl = dto.image,
            isOnboarded = dto.isOnboarded || (mappedRole != null)
        )
    }
}
