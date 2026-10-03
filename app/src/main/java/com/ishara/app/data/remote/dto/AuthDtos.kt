package com.ishara.app.data.remote.dto

data class GoogleIdTokenPayloadDto(
    val token: String,
    val accessToken: String? = null
)

data class GoogleSignInRequestDto(
    val provider: String = "google",
    val idToken: GoogleIdTokenPayloadDto
)

data class OnboardingRequestDto(
    val role: String
)

data class SendVerificationOtpRequestDto(
    val email: String,
    val type: String = "sign-in"
)

data class SendVerificationOtpResponseDto(
    val success: Boolean,
    val message: String? = null
)

data class SignInEmailOtpRequestDto(
    val email: String,
    val otp: String
)

data class AuthSessionResponseDto(
    val token: String,
    val userId: String,
    val role: String? = null,
    val expiresAt: Long? = null
)

data class UserDto(
    val id: String,
    val name: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val role: String? = null,
    val image: String? = null,
    val isOnboarded: Boolean = false,
    val onboardingCompleted: Boolean = isOnboarded
)
