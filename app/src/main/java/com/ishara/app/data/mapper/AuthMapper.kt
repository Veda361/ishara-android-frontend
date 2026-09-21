package com.ishara.app.data.mapper

import com.ishara.app.data.remote.dto.AuthSessionResponseDto
import com.ishara.app.data.remote.dto.UserDto
import com.ishara.app.domain.model.AuthSession
import com.ishara.app.domain.model.User
import com.ishara.app.domain.model.UserRole

object AuthMapper {
    fun toDomain(dto: AuthSessionResponseDto): AuthSession {
        return AuthSession(
            token = dto.token,
            userId = dto.userId,
            role = UserRole.fromString(dto.role),
            expiresAtMillis = dto.expiresAt
        )
    }

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
}
