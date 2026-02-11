package com.example.ejemplo.data

// Lo que enviamos al servidor
data class LoginRequest(
    val email: String,
    val password: String
)
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val password_confirmation: String, // Laravel exige esto si usas la regla 'confirmed'
    val cedula: String? = null,
    val telefono: String? = null
)

// Lo que el servidor nos responde (según lo que programamos en Laravel)
data class LoginResponse(
    val message: String,
    val access_token: String,
    val user: Usuario // Reutilizamos tu clase Usuario existente
)