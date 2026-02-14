package com.example.ejemplo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.ejemplo.data.SessionManager
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.Constraints
import java.util.concurrent.TimeUnit
import androidx.work.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración del Worker (Fondo)
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
                        onNavigateToRegister = {
                            currentScreen = "register" // <--- Navegación al registro
                        }
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

                // ... tus otras pantallas ...
                "reporte" -> ReportScreen(onBack = { currentScreen = "home" })
                "historial" -> MyReportsScreen(onBack = { currentScreen = "home" })
                "mis_alertas" -> MyAlertsScreen(onBack = { currentScreen = "home" })
                "soporte" -> SupportScreen(onBack = { currentScreen = "home" })
                "configuracion" -> SettingsScreen(onBack = { currentScreen = "home" })
                "user_profile" -> UserProfileScreen(onBack = { currentScreen = "home" })
            }
        }
    }
}