package com.example.ejemplo.data

object Config {
    // --- CAMBIA ESTA LÍNEA CUANDO CAMBIES DE SERVIDOR ---

    // MODO DESARROLLO (Emulador):
    //private const val BASE_IP = "http://10.0.2.2:8000"

    // MODO PRUEBA (Celular Físico - Cambia por la IP de tu PC):
     private const val BASE_IP = "http://192.168.0.101:8000"

    // MODO PRODUCCIÓN (Servidor Real):
    // private const val BASE_IP = "https://api.seguridad-ueb.com"

    // ----------------------------------------------------

    // URLs automáticas
    const val API_URL = "$BASE_IP/api/"
    const val IMAGES_URL = "$BASE_IP/storage/"
}