package com.ishara.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GoogleSignInRequestDto(
    val provider: String = "google",
    val idToken: String
)

@Serializable
data class EmailOtpSendRequestDto(
    val email: String,
    val type: String = "sign-in"
)

@Serializable
data class EmailOtpSignInRequestDto(
    val email: String,
    val otp: String
)

/**
 * Standard Better Auth response for successful sign-in.
 */
@Serializable
data class AuthSessionResponseDto(
    val user: UserDto,
    val session: SessionDto
)

@Serializable
data class SessionResponseDto(
    val session: SessionDto,
    val user: UserDto
)

@Serializable
data class SessionDto(
    val id: String,
    val userId: String,
    val expiresAt: String,
    val token: String? = null // Sometimes token is separate, sometimes session.id is used
)

@Serializable
data class OnboardingRequestDto(
    val role: String,
    val name: String,
    val phoneNumber: String
)

@Serializable
data class UpdateUserRequestDto(
    val name: String? = null,
    val phoneNumber: String? = null,
    val image: String? = null
)

@Serializable
data class UserDto(
    val id: String,
    val email: String? = null,
    val name: String? = null,
    val role: String? = null,
    val isOnboarded: Boolean = false,
    val phoneNumber: String? = null,
    val image: String? = null,
    val createdAt: String? = null
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null
)
