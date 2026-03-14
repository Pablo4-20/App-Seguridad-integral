package com.example.ejemplo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.*
import com.example.ejemplo.data.FcmTokenRequest
import com.example.ejemplo.data.Noticia
import com.example.ejemplo.data.RetrofitClient
import com.example.ejemplo.data.SessionManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Configuración Inicial
        crearCanalNotificaciones()
        solicitarPermisoNotificaciones()
        val session = SessionManager(this)

        if (session.fetchAuthToken() != null) {
            sincronizarTokenFCM(session)
        }

        // Configuración Worker
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "NoticiasBackgroundWork", ExistingPeriodicWorkPolicy.KEEP, workRequest
        )

        // 2. DETECTAR INTENTS Y DEEP LINKS
        val forceLogoutMessage = intent.getStringExtra("force_logout_message")
        val deepLinkNoticiaId = intent.getStringExtra("extra_noticia_id")

        // Deep Link para Reset Password
        val uri = intent.data
        var deepLinkResetToken: String? = null
        var deepLinkResetEmail: String? = null

        if (uri != null && uri.scheme == "seguridadintegral" && uri.host == "reset-password") {
            deepLinkResetToken = uri.path?.removePrefix("/")
            deepLinkResetEmail = uri.getQueryParameter("email")
            Log.d("MainActivity", "DeepLink Reset: token=$deepLinkResetToken, email=$deepLinkResetEmail")
        }

        setContent {
            var showLogoutDialog by remember { mutableStateOf(forceLogoutMessage != null) }

            var selectedNoticiaObj by remember { mutableStateOf<Noticia?>(null) }
            var selectedNoticiaId by remember { mutableStateOf<String?>(deepLinkNoticiaId) }

            // Estados para el reseteo de contraseña
            var resetToken by remember { mutableStateOf(deepLinkResetToken) }
            var resetEmail by remember { mutableStateOf(deepLinkResetEmail) }

            // Lógica de Pantalla Inicial
            var currentScreen by remember {
                mutableStateOf(
                    if (resetToken != null && resetEmail != null) {
                        "reset_password"
                    } else if (session.fetchAuthToken() != null) {
                        if (deepLinkNoticiaId != null) "noticia_detalle" else "home"
                    } else {
                        "login"
                    }
                )
            }

            var homeStartTab by remember { mutableIntStateOf(0) }

            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = { Text("Cuenta Inactivada") },
                    text = { Text(forceLogoutMessage ?: "Tu cuenta ha sido cerrada por seguridad.") },
                    confirmButton = {
                        Button(onClick = { showLogoutDialog = false }) {
                            Text("Entendido")
                        }
                    }
                )
            }

            when (currentScreen) {
                "login" -> {
                    LoginScreen(
                        onLoginSuccess = { token, name ->
                            session.saveAuthToken(token, name)
                            sincronizarTokenFCM(session)
                            homeStartTab = 0
                            currentScreen = "home"
                        },
                        onNavigateToRegister = { currentScreen = "register" },
                        onNavigateToForgotPassword = { currentScreen = "forgot_password" } // NUEVO
                    )
                }
                "register" -> {
                    RegisterScreen(
                        onRegisterSuccess = { token, name ->
                            session.saveAuthToken(token, name)
                            sincronizarTokenFCM(session)
                            homeStartTab = 0
                            currentScreen = "home"
                        },
                        onBack = { currentScreen = "login" }
                    )
                }
                "forgot_password" -> {
                    ForgotPasswordScreen(
                        onBack = { currentScreen = "login" }
                    )
                }
                "reset_password" -> {
                    if (resetToken != null && resetEmail != null) {
                        ResetPasswordScreen(
                            token = resetToken!!,
                            email = resetEmail!!,
                            onSuccess = {
                                // Limpiamos y volvemos al login
                                resetToken = null
                                resetEmail = null
                                currentScreen = "login"
                            }
                        )
                    } else {
                        currentScreen = "login"
                    }
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
                "noticia_detalle" -> {
                    NoticiaDetailScreen(
                        noticiaObj = selectedNoticiaObj,
                        noticiaId = selectedNoticiaId,
                        onBack = {
                            currentScreen = "home"
                            selectedNoticiaId = null
                            selectedNoticiaObj = null
                            homeStartTab = 0
                        }
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val uri = intent.data
        if (intent.getStringExtra("force_logout_message") != null ||
            intent.getStringExtra("extra_noticia_id") != null ||
            (uri != null && uri.scheme == "seguridadintegral" && uri.host == "reset-password")) {
            recreate()
        }
    }

    private fun sincronizarTokenFCM(session: SessionManager) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result
            val authToken = session.fetchAuthToken()
            if (authToken != null) {
                lifecycleScope.launch {
                    try {
                        RetrofitClient.api.actualizarTokenFcm("Bearer $authToken", FcmTokenRequest(token))
                    } catch (e: Exception) {
                        Log.e("FCM", "Error enviando token: ${e.message}")
                    }
                }
            }
        }
    }

    private fun crearCanalNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("seguridad_ueb_channel", "Noticias Seguridad", NotificationManager.IMPORTANCE_HIGH)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun solicitarPermisoNotificaciones() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }
}