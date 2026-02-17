package com.example.ejemplo.data

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call // <--- FALTABA ESTO
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path // <--- FALTABA ESTO

interface ApiService {
    // --- AUTENTICACIÓN ---
    @POST("login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("register")
    suspend fun register(@Body request: RegisterRequest): RegisterResponse

    // --- PERFIL Y TOKEN ---
    @GET("perfil")
    suspend fun obtenerPerfil(@Header("Authorization") token: String): Usuario

    @POST("perfil/fcm-token")
    suspend fun actualizarTokenFcm(@Header("Authorization") token: String, @Body request: FcmTokenRequest): Any

    @Multipart
    @POST("perfil/foto")
    suspend fun subirFotoPerfil(@Header("Authorization") token: String, @Part foto: MultipartBody.Part): ReporteResponse

    // --- ALERTAS ---
    @POST("alertas")
    suspend fun enviarAlerta(@Header("Authorization") token: String, @Body request: AlertaRequest): AlertaResponse

    @GET("mis-alertas")
    suspend fun obtenerMisAlertas(@Header("Authorization") token: String): List<AlertaItem>

    // --- INCIDENTES ---
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

    @GET("incidentes")
    suspend fun obtenerMisReportes(@Header("Authorization") token: String): List<ReporteItem>

    // --- NOTICIAS ---
    @GET("noticias")
    suspend fun obtenerNoticias(@Header("Authorization") token: String): List<Noticia>

    // Método para obtener UNA sola noticia por ID (Usado por notificaciones)
    // NOTA: Si tu BASE_URL ya incluye "/api/", esto llamará a ".../api/noticias/{id}"
    @GET("noticias/{id}")
    fun getNoticia(@Path("id") id: Int): Call<Noticia>

    // --- OTROS ---
    @GET("mapa/puntos")
    suspend fun obtenerPuntosMapa(): List<PuntoMapa>
}

object RetrofitClient {
    // Asegúrate de que Config.API_URL termine en "/" (ej: "http://192.168.1.10:8000/api/")
    private const val BASE_URL = Config.API_URL

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}