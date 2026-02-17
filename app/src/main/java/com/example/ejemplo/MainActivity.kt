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

        // 2. DETECTAR SI VENIMOS DE UNA NOTIFICACIÓN
        // Buscamos el extra que pusimos en MyFirebaseMessagingService
        val deepLinkNoticiaId = intent.getStringExtra("extra_noticia_id")
        Log.d("MainActivity", "DeepLink recibido: $deepLinkNoticiaId")

        setContent {
            // Estado para controlar qué noticia se seleccionó (Objeto completo o ID)
            var selectedNoticiaObj by remember { mutableStateOf<Noticia?>(null) }
            var selectedNoticiaId by remember { mutableStateOf<String?>(deepLinkNoticiaId) }

            // Lógica de Pantalla Inicial
            var currentScreen by remember {
                mutableStateOf(
                    if (session.fetchAuthToken() != null) {
                        if (deepLinkNoticiaId != null) "noticia_detalle" else "home"
                    } else {
                        "login"
                    }
                )
            }

            var homeStartTab by remember { mutableIntStateOf(0) }

            when (currentScreen) {
                "login" -> {
                    LoginScreen(
                        onLoginSuccess = { token, name ->
                            session.saveAuthToken(token, name)
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
                        onNavigateToProfile = { homeStartTab = 3; currentScreen = "user_profile" },

                        // IMPORTANTE: Modifica tu HomeScreen para pasar el objeto Noticia cuando se hace clic en la lista
                        /* Debes asegurarte de que tu HomeScreen o NewsScreen tenga un callback:
                           onNoticiaClick = { noticia ->
                               selectedNoticiaObj = noticia
                               selectedNoticiaId = null // Limpiamos ID
                               currentScreen = "noticia_detalle"
                           }
                        */
                    )
                }
                "noticia_detalle" -> {
                    NoticiaDetailScreen(
                        noticiaObj = selectedNoticiaObj,
                        noticiaId = selectedNoticiaId,
                        onBack = {
                            currentScreen = "home"
                            // Al volver, limpiamos la selección para que no se quede pegada
                            selectedNoticiaId = null
                            selectedNoticiaObj = null
                            homeStartTab = 0 // Volver a la pestaña de noticias
                        }
                    )
                }
                // ... Resto de pantallas igual ...
                "reporte" -> ReportScreen(onBack = { currentScreen = "home" })
                "historial" -> MyReportsScreen(onBack = { currentScreen = "home" })
                "mis_alertas" -> MyAlertsScreen(onBack = { currentScreen = "home" })
                "soporte" -> SupportScreen(onBack = { currentScreen = "home" })
                "configuracion" -> SettingsScreen(onBack = { currentScreen = "home" })
                "user_profile" -> UserProfileScreen(onBack = { currentScreen = "home" })
            }
        }
    }

    // --- ESTO ES IMPORTANTE PARA CUANDO LA APP YA ESTÁ ABIERTA EN SEGUNDO PLANO ---
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Actualizamos el intent actual
        val newId = intent.getStringExtra("extra_noticia_id")
        if (newId != null) {
            // Aquí podrías forzar una recomposición o reinicio de la actividad si fuera necesario
            // Pero normalmente con onCreate basta si la actividad se recrea.
            // Si la actividad no se destruye, necesitarás observar el intent dentro del Composable
            // o reiniciar la Activity:
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