package com.example.ejemplo.data

// --- LOGIN ---
data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String,
    val access_token: String,
    val user: Usuario
)

// --- REGISTRO ---
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val cedula: String,
    val telefono: String
)

data class RegisterResponse(
    val message: String,
    val access_token: String,
    val user: Usuario
)

// --- NOTIFICACIONES ---
// Arregla el error "Too many arguments for constructor(): FcmTokenRequest"
data class FcmTokenRequest(
    val fcm_token: String
)