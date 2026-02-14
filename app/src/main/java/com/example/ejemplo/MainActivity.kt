package com.example.ejemplo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.example.ejemplo.data.FcmTokenRequest
import com.example.ejemplo.data.RetrofitClient
import com.example.ejemplo.data.SessionManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Canales y Permisos
        crearCanalNotificaciones()
        solicitarPermisoNotificaciones()

        val session = SessionManager(this)

        // 2. RECUPERADO: Si ya está logueado, actualizar el token inmediatamente
        if (session.fetchAuthToken() != null) {
            sincronizarTokenFCM(session)
        }

        // Configuración del Worker (Tu código de fondo)
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NoticiasBackgroundWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        setContent {
            var currentScreen by remember {
                mutableStateOf(if (session.fetchAuthToken() != null) "home" else "login")
            }
            var homeStartTab by remember { mutableIntStateOf(0) }

            when (currentScreen) {
                "login" -> {
                    LoginScreen(
                        onLoginSuccess = { token, name ->
                            session.saveAuthToken(token, name)
                            // 3. RECUPERADO: Enviar token al entrar
                            sincronizarTokenFCM(session)

                            homeStartTab = 0
                            currentScreen = "home"
                        },
                        onNavigateToRegister = { currentScreen = "register" }
                    )
                }
                "register" -> {
                    RegisterScreen(
                        onRegisterSuccess = { token, name ->
                            session.saveAuthToken(token, name)
                            // 3. RECUPERADO: Enviar token al registrarse
                            sincronizarTokenFCM(session)

                            homeStartTab = 0
                            currentScreen = "home"
                        },
                        onBack = { currentScreen = "login" }
                    )
                }
                "home" -> {
                    HomeScreen(
                        initialTab = homeStartTab,
                        onLogout = {
                            session.clearSession()
                            currentScreen = "login"
                        },
                        onNavigateToReport = { homeStartTab = 0; currentScreen = "reporte" },
                        onNavigateToHistory = { homeStartTab = 3; currentScreen = "historial" },
                        onNavigateToAlerts = { homeStartTab = 3; currentScreen = "mis_alertas" },
                        onNavigateToSettings = { homeStartTab = 3; currentScreen = "configuracion" },
                        onNavigateToSupport = { homeStartTab = 3; currentScreen = "soporte" },
                        onNavigateToProfile = { homeStartTab = 3; currentScreen = "user_profile" }
                    )
                }
                "reporte" -> ReportScreen(onBack = { currentScreen = "home" })
                "historial" -> MyReportsScreen(onBack = { currentScreen = "home" })
                "mis_alertas" -> MyAlertsScreen(onBack = { currentScreen = "home" })
                "soporte" -> SupportScreen(onBack = { currentScreen = "home" })
                "configuracion" -> SettingsScreen(onBack = { currentScreen = "home" })
                "user_profile" -> UserProfileScreen(onBack = { currentScreen = "home" })
            }
        }
    }

    // --- ESTA ES LA FUNCIÓN QUE FALTABA ---
    private fun sincronizarTokenFCM(session: SessionManager) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Error obteniendo token FCM", task.exception)
                return@addOnCompleteListener
            }

            // 1. Obtener el token nuevo del celular
            val token = task.result
            Log.d("FCM", "Token generado: $token")

            // 2. Enviarlo a Laravel para actualizar el "viejo"
            val authToken = session.fetchAuthToken()
            if (authToken != null) {
                lifecycleScope.launch {
                    try {
                        RetrofitClient.api.actualizarTokenFcm("Bearer $authToken", FcmTokenRequest(token))
                        Log.d("FCM", "Token actualizado en el servidor correctamente.")
                    } catch (e: Exception) {
                        Log.e("FCM", "Fallo al enviar token al servidor: ${e.message}")
                    }
                }
            }
        }
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "seguridad_ueb_channel"
            val name = "Noticias Seguridad"
            val descriptionText = "Canal para noticias y alertas de seguridad"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }
}