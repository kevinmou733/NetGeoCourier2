package com.example.netgeocourier.data

data class AuthUser(
    val id: String,
    val username: String,
    val displayName: String,
    val studentId: String = "",
    val createdAt: String = "",
    val updatedAt: String = ""
)

data class AuthPayload(
    val tokenType: String,
    val accessToken: String,
    val expiresIn: Long,
    val user: AuthUser
)

data class LoginRequest(
    val username: String,
    val password: String
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val displayName: String = "",
    val studentId: String = ""
)

data class AuthSession(
    val accessToken: String,
    val user: AuthUser? = null
)
