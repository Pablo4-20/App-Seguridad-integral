package com.example.ejemplo

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.ejemplo.data.RetrofitClient
import com.example.ejemplo.data.SessionManager
import com.example.ejemplo.data.SettingsManager

class NotificationWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        // Contexto de la aplicación
        val context = applicationContext

        // Inicializamos los gestores
        val settings = SettingsManager(context)
        val session = SessionManager(context)
        val helper = NotificationHelper(context)

        try {
            // 1. Obtener Token (Si no hay sesión, paramos)
            val token = session.fetchAuthToken() ?: return Result.success()

            // 2. Consultar API
            val noticias = RetrofitClient.api.obtenerNoticias("Bearer $token")

            if (noticias.isNotEmpty()) {
                val noticiaMasReciente = noticias.first() // La primera es la más nueva (ID más alto)

                // CORRECCIÓN 1: Usar el nombre correcto de la función
                val ultimoIdVisto = settings.leerUltimoIdNoticia()

                // 3. Comparar: ¿Es nueva?
                if (noticiaMasReciente.id > ultimoIdVisto) {

                    // CORRECCIÓN 2: Usar el nombre correcto 'mostrarNotificacion'
                    // ¡ES NUEVA! -> Lanzar alerta
                    helper.mostrarNotificacion(
                        "Nueva Publicación",
                        noticiaMasReciente.titulo
                    )

                    // CORRECCIÓN 3: Actualizar memoria con el nombre correcto
                    settings.guardarUltimoIdNoticia(noticiaMasReciente.id)
                }
            }
            return Result.success()

        } catch (e: Exception) {
            // Si falla por internet, Android reintentará más tarde
            return Result.retry()
        }
    }
}