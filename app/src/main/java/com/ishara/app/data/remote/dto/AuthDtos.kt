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

@Serializable
data class AuthSessionResponseDto(
    val user: UserDto,
    val token: String
)

@Serializable
data class SessionResponseDto(
    val session: SessionDto,
    val user: SessionUserDto
)

@Serializable
data class SessionDto(
    val id: String,
    val userId: String,
    val expiresAt: String
)

@Serializable
data class SessionUserDto(
    val id: String,
    val email: String,
    val role: String
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
    val email: String,
    val name: String,
    val role: String,
    val isOnboarded: Boolean,
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
