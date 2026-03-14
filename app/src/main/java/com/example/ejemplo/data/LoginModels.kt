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
data class FcmTokenRequest(
    val fcm_token: String
)

// --- RECUPERACIÓN DE CONTRASEÑA (NUEVO) ---
data class ForgotPasswordRequest(val cedula: String)
data class ForgotPasswordResponse(val message: String)

data class ResetPasswordRequest(
    val email: String,
    val token: String,
    val password: String
)
data class ResetPasswordResponse(val message: String)