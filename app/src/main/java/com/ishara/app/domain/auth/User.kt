package com.ishara.app.domain.auth

data class User(
    val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String? = null
)