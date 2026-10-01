package com.ishara.app.data.mapper

import com.ishara.app.data.remote.dto.AuthSessionResponseDto
import com.ishara.app.data.remote.dto.SessionResponseDto
import com.ishara.app.data.remote.dto.UserDto
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.User
import com.ishara.app.domain.model.UserRole

/**
 * Maps between Auth Remote DTOs and Domain Models.
 */
object AuthMapper {

    fun toDomain(dto: UserDto): User {
        return User(
            id = dto.id,
            name = dto.name,
            email = dto.email,
            phoneNumber = dto.phoneNumber,
            role = UserRole.fromString(dto.role),
            profileImageUrl = dto.image,
            isOnboarded = dto.isOnboarded
        )
    }

    fun toAuthSession(dto: AuthSessionResponseDto): AuthSession {
        return AuthSession(
            token = dto.token,
            userId = dto.user.id,
            email = dto.user.email,
            role = UserRole.fromString(dto.user.role),
            name = dto.user.name
        )
    }

    fun toAuthSession(dto: SessionResponseDto, token: String): AuthSession {
        return AuthSession(
            token = token,
            userId = dto.user.id,
            email = dto.user.email,
            role = UserRole.fromString(dto.user.role),
            name = null
        )
    }
}
