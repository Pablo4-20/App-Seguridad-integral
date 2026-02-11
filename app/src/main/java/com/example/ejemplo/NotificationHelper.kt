package com.example.ejemplo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.ejemplo.data.SettingsManager

class NotificationHelper(val context: Context) {
    // Usamos un ID nuevo para asegurar que se apliquen los cambios de sonido/vibración
    private val channelId = "canal_seguridad_ueb_final_v4"
    private val settingsManager = SettingsManager(context)

    init {
        crearCanal()
    }

    private fun crearCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Noticias y Alertas UEB"
            val descriptionText = "Canal para comunicados de seguridad"
            val importance = NotificationManager.IMPORTANCE_HIGH

            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                setSound(soundUri, AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // --- AQUÍ ESTABA LA CONFUSIÓN: AHORA SE LLAMA "mostrarNotificacion" ---
    fun mostrarNotificacion(titulo: String, mensaje: String) {

        // 1. Verificar configuración del usuario
        if (!settingsManager.leerNotificaciones()) return

        // 2. Verificar permisos (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        // 3. Leer preferencias de Sonido/Vibración
        val usarSonido = settingsManager.leerSonido()
        val usarVibracion = settingsManager.leerVibNoticia() // Usamos la vibración específica de noticias

        val soundUri = if (usarSonido) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) else null
        val vibrationPattern = if (usarVibracion) longArrayOf(0, 500, 200, 500) else longArrayOf(0)

        try {
            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(titulo)
                .setContentText(mensaje)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setVibrate(vibrationPattern)
                // Si ambos están apagados, silenciamos la notificación
                .setSilent(!usarSonido && !usarVibracion)

            with(NotificationManagerCompat.from(context)) {
                notify(System.currentTimeMillis().toInt(), builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}