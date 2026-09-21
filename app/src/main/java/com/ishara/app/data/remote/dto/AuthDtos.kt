package com.ishara.app.data.remote.dto

data class GoogleSignInRequestDto(
    val provider: String = "google",
    val idToken: String
)

data class OnboardingRequestDto(
    val role: String
)

data class AuthSessionResponseDto(
    val token: String,
    val userId: String,
    val role: String,
    val expiresAt: Long? = null
)

data class UserDto(
    val id: String,
    val name: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val role: String,
    val image: String? = null,
    val isOnboarded: Boolean = false
)
