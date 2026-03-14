package com.example.ejemplo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.ejemplo.data.SessionManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "¡Mensaje recibido!: ${remoteMessage.from}")

        // 1. DETECTAR CIERRE DE SESIÓN FORZADO (Mensaje silencioso de datos)
        val action = remoteMessage.data["action"]
        if (action == "force_logout") {
            Log.d("FCM", "Orden de cierre de sesión recibida desde Laravel.")
            manejarCierreDeCuenta()
            return // Detenemos aquí para no mostrar una notificación visual normal
        }

        // 2. FLUJO NORMAL (Noticias)
        val noticiaId = remoteMessage.data["noticia_id"]
        Log.d("FCM", "ID Noticia recibido: $noticiaId")

        remoteMessage.notification?.let {
            mostrarNotificacion(it.title, it.body, noticiaId)
        }
    }

    private fun manejarCierreDeCuenta() {
        // Limpiamos los datos locales (SharedPreferences)
        val session = SessionManager(applicationContext)
        session.clearSession()

        // Redirigimos a MainActivity limpiando el historial de pantallas abiertas
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("force_logout_message", "Tu cuenta ha sido inactivada por un administrador.")
        }
        startActivity(intent)
    }

    override fun onNewToken(token: String) {
        Log.d("FCM", "Refreshed token: $token")
    }

    private fun mostrarNotificacion(title: String?, body: String?, noticiaId: String?) {
        val channelId = "seguridad_ueb_channel"

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        if (noticiaId != null) {
            intent.putExtra("extra_noticia_id", noticiaId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title ?: "Nueva Notificación")
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Noticias Seguridad",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}