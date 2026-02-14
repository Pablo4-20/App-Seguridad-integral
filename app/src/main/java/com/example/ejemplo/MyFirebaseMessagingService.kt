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
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // ESTO SE EJECUTA CUANDO LA APP ESTÁ ABIERTA O EN PRIMER PLANO
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "¡Mensaje recibido!: ${remoteMessage.from}")

        // Extraer título y cuerpo de la notificación
        remoteMessage.notification?.let {
            Log.d("FCM", "Datos: ${it.title} - ${it.body}")
            mostrarNotificacion(it.title, it.body)
        }
    }

    // Se ejecuta si Firebase decide cambiar tu token de seguridad
    override fun onNewToken(token: String) {
        Log.d("FCM", "Refreshed token: $token")
        // Aquí podrías enviar el token al backend si quisieras
    }

    private fun mostrarNotificacion(title: String?, body: String?) {
        val channelId = "seguridad_ueb_channel" // Debe coincidir con el AndroidManifest.xml

        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Construir la alerta visual
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Icono por defecto de Android
            .setContentTitle(title ?: "Nueva Notificación")
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear el canal si es Android 8 o superior (Obligatorio)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Noticias Seguridad",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}