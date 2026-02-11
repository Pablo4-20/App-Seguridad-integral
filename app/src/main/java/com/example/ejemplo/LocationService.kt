package com.example.ejemplo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

class LocationService(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // Verificar si el GPS está prendido
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    suspend fun getUserLocation(): Pair<String, String>? {
        // 1. Si el GPS está apagado, avisar y abrir configuración
        if (!isLocationEnabled()) {
            Toast.makeText(context, "Por favor enciende tu GPS", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return null
        }

        return try {
            // 2. Intentar obtener ubicación precisa (Prioridad Alta)
            val location: Location? = fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).await()

            if (location != null) {
                Pair(location.latitude.toString(), location.longitude.toString())
            } else {
                // 3. Si falla la precisa, intentar la última conocida (Respaldo)
                val lastLocation = fusedLocationClient.lastLocation.await()
                if (lastLocation != null) {
                    Pair(lastLocation.latitude.toString(), lastLocation.longitude.toString())
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}