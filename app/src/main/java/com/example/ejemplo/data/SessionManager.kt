package com.example.ejemplo.data

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private var prefs: SharedPreferences = context.getSharedPreferences("app_ugr_prefs", Context.MODE_PRIVATE)

    companion object {
        const val USER_TOKEN = "user_token"
        const val USER_NAME = "user_name"
    }

    // Guardar sesión al hacer login
    fun saveAuthToken(token: String, name: String) {
        val editor = prefs.edit()
        editor.putString(USER_TOKEN, token)
        editor.putString(USER_NAME, name)
        editor.apply()
    }

    // Obtener token (para las peticiones API)
    fun fetchAuthToken(): String? {
        return prefs.getString(USER_TOKEN, null)
    }

    // Obtener nombre (para el saludo)
    fun fetchUserName(): String? {
        return prefs.getString(USER_NAME, "Usuario")
    }

    // Cerrar sesión
    fun clearSession() {
        val editor = prefs.edit()
        editor.clear() // Borra todo
        editor.apply()
    }
}