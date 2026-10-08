package com.ishara.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AuthSession(
    val token: String,
    val userId: String,
    val email: String,
    val role: UserRole,
    val name: String? = null
)
