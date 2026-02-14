package com.example.ejemplo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.ejemplo.data.SessionManager
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. OBLIGATORIO: Crear el canal de notificaciones (Android 8+)
        crearCanalNotificaciones()

        // 2. OBLIGATORIO: Pedir permiso explícito (Android 13+)
        // Si no se ejecuta esto, no aparecerá la opción de permitir notificaciones.
        solicitarPermisoNotificaciones()

        // Configuración del Worker (Segundo plano)
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

        val session = SessionManager(this)

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

    // --- FUNCIONES CRÍTICAS QUE FALTABAN ---

    private fun crearCanalNotificaciones() {
        // En Android 8.0+ las notificaciones DEBEN tener un canal asignado
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "seguridad_ueb_channel"
            val name = "Noticias Seguridad"
            val descriptionText = "Canal para noticias y alertas de seguridad"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun solicitarPermisoNotificaciones() {
        // En Android 13+ (Tiramisu) se debe pedir permiso en tiempo de ejecución
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