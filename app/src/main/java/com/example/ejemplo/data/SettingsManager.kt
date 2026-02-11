package com.example.ejemplo.data

import android.content.Context

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("app_ajustes_v2", Context.MODE_PRIVATE)

    // Preferencias Generales
    fun guardarNotificaciones(activo: Boolean) = prefs.edit().putBoolean("master_notif", activo).apply()
    fun leerNotificaciones(): Boolean = prefs.getBoolean("master_notif", true)

    fun guardarSonido(activo: Boolean) = prefs.edit().putBoolean("master_sound", activo).apply()
    fun leerSonido(): Boolean = prefs.getBoolean("master_sound", true)

    // Vibraciones
    fun guardarVibNoticia(activo: Boolean) = prefs.edit().putBoolean("vib_news", activo).apply()
    fun leerVibNoticia(): Boolean = prefs.getBoolean("vib_news", true)

    fun guardarVibAlerta(activo: Boolean) = prefs.edit().putBoolean("vib_panic", activo).apply()
    fun leerVibAlerta(): Boolean = prefs.getBoolean("vib_panic", true)

    fun guardarVibReporte(activo: Boolean) = prefs.edit().putBoolean("vib_report", activo).apply()
    fun leerVibReporte(): Boolean = prefs.getBoolean("vib_report", false)

    // --- ID DE NOTICIAS (Nombres Corregidos) ---
    fun guardarUltimoIdNoticia(id: Int) = prefs.edit().putInt("last_news_id", id).apply()
    fun leerUltimoIdNoticia(): Int = prefs.getInt("last_news_id", 0)
}