package com.example.ejemplo.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    // Login
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    //Registro
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): LoginResponse

    // Perfil
    @GET("perfil")
    suspend fun obtenerPerfil(@Header("Authorization") token: String): Usuario

    @Multipart
    @POST("perfil/foto")
    suspend fun subirFotoPerfil(@Header("Authorization") token: String, @Part foto: MultipartBody.Part): ReporteResponse

    // Alertas de Pánico
    @POST("alertas")
    suspend fun enviarAlerta(@Header("Authorization") token: String, @Body request: AlertaRequest): AlertaResponse

    @GET("mis-alertas")
    suspend fun obtenerMisAlertas(@Header("Authorization") token: String): List<AlertaItem>

    // --- ESTA ES LA FUNCIÓN QUE ARREGLA EL ERROR EN REPORTSCREEN ---
    @Multipart
    @POST("incidentes")
    suspend fun enviarReporte(
        @Header("Authorization") token: String,
        @Part("tipo") tipo: RequestBody,
        @Part("descripcion") descripcion: RequestBody,
        @Part("latitud") latitud: RequestBody,
        @Part("longitud") longitud: RequestBody,
        @Part foto: MultipartBody.Part?
    ): ReporteResponse
    // ---------------------------------------------------------------

    @GET("incidentes")
    suspend fun obtenerMisReportes(@Header("Authorization") token: String): List<ReporteItem>

    @GET("noticias")
    suspend fun obtenerNoticias(@Header("Authorization") token: String): List<Noticia>

    @GET("mapa/puntos")
    suspend fun obtenerPuntosMapa(): List<PuntoMapa>
}

object RetrofitClient {
    // Usamos la constante centralizada
    private const val BASE_URL = Config.API_URL

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}