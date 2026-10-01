package com.ishara.app.data.remote.dto

data class AuthUserDto(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val isOnboarded: Boolean = false
)