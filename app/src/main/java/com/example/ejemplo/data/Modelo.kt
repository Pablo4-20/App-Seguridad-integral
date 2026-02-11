package com.example.ejemplo.data

import com.google.gson.annotations.SerializedName

data class Usuario(
    val id: Int,
    val nombre: String,
    val email: String,
    val rol: String? = "estudiante", // Valor por defecto si viene nulo
    val foto_perfil: String? = null,
    val email_verified_at: String? = null
)

// Lo que enviamos al presionar el botón rojo
data class AlertaRequest(
    val latitud: String,
    val longitud: String
)

// Lo que nos responde el servidor
data class AlertaResponse(
    val message: String,
    val alerta_id: Int,
    val status: String
)

// HU #8: Datos para enviar un reporte
data class ReporteRequest(
    val tipo: String,
    val descripcion: String,
    val latitud: String,
    val longitud: String
)

data class ReporteResponse(
    val message: String,
    val id: Int
)

// HU #10: Estructura de una noticia/protocolo
data class Noticia(
    val id: Int,
    val titulo: String,
    val contenido: String,
    val tipo: String, // recomendacion, protocolo, noticia
    val imagen_url: String?,
    val created_at: String
)

data class ReporteItem(
    val id: Int,
    val tipo: String,
    val descripcion: String,
    val estado: String, // pendiente, en_curso, resuelto
    val created_at: String,
    val latitud: String?,
    val longitud: String?,
    @SerializedName("foto_path")
    val imagen_url: String?
)

data class AlertaItem(
    val id: Int,
    val latitud: String,
    val longitud: String,
    val atendida: Boolean, // CLAVE: Esto nos dice si la ayuda va en camino
    val created_at: String
)

data class PuntoMapa(
    val id: Int,
    val titulo: String,
    val latitud: String,
    val longitud: String,
    val tipo: String // seguro, peligro
)