package com.ishara.app.domain.model

/**
 * Domain model representing an authenticated Ishaara user.
 */
data class User(
    val id: String,
    val name: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val role: UserRole,
    val profileImageUrl: String? = null,
    val isOnboarded: Boolean = false
)


