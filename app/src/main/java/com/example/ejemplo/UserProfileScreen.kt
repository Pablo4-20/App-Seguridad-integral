package com.example.ejemplo

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ejemplo.data.RetrofitClient
import com.example.ejemplo.data.SessionManager
import com.example.ejemplo.data.Usuario
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val session = SessionManager(context)
    val DarkBlue = Color(0xFF1A2B46)

    BackHandler { onBack() }
    // Estado del usuario (desde API)
    var usuario by remember { mutableStateOf<Usuario?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Datos de sesión (Respaldo inmediato)
    val sessionName = session.fetchUserName() ?: "Usuario"

    // Calculamos la inicial para mostrar
    val nombreMostrar = usuario?.nombre ?: sessionName
    val inicial = nombreMostrar.firstOrNull()?.uppercase() ?: "U"

    // Cargar datos frescos al entrar
    LaunchedEffect(Unit) {
        try {
            val token = session.fetchAuthToken()
            if (token != null) {
                usuario = RetrofitClient.api.obtenerPerfil("Bearer $token")
            }
        } catch (e: Exception) {
            // Si falla la red, usamos los datos de sesión silenciosamente
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null, tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- CABECERA AZUL ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(DarkBlue),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {

                    // AVATAR CON INICIAL (ESTILO GMAIL)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(100.dp)
                            .background(Color.White, CircleShape) // Fondo Blanco
                            .border(4.dp, Color(0xFF90CAF9), CircleShape) // Borde Azul Claro
                    ) {
                        Text(
                            text = inicial.toString(),
                            color = DarkBlue,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // NOMBRE
                    Text(
                        text = nombreMostrar,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Comunidad Universitaria",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- TARJETAS DE DATOS ---
            if (isLoading) {
                CircularProgressIndicator(color = DarkBlue, modifier = Modifier.padding(20.dp))
            } else {
                ProfileDetailCard("ID de Usuario", usuario?.id?.toString() ?: "N/A", Icons.Default.Badge)
                ProfileDetailCard("Correo Institucional", usuario?.email ?: sessionName, Icons.Default.Email, isVerified = usuario?.email_verified_at != null)
                ProfileDetailCard("Rol", usuario?.rol?.uppercase() ?: "ESTUDIANTE", Icons.Default.Security)
            }
        }
    }
}

@Composable
fun ProfileDetailCard(label: String, value: String, icon: ImageVector, isVerified: Boolean = false) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color(0xFF1A2B46), modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, fontSize = 12.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                    if (isVerified) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.CheckCircle, "Verificado", tint = Color(0xFF388E3C), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}