package com.example.ejemplo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ejemplo.components.NoInternetScreen
import com.example.ejemplo.utils.NetworkUtils
import com.example.ejemplo.data.AlertaItem
import com.example.ejemplo.data.RetrofitClient
import com.example.ejemplo.data.SessionManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAlertsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val DarkBlue = Color(0xFF1A2B46)

    // --- VERIFICACIÓN DE INTERNET ---
    var isConnected by remember { mutableStateOf(NetworkUtils.isNetworkAvailable(context)) }
    if (!isConnected) {
        NoInternetScreen(
            onRetry = { isConnected = NetworkUtils.isNetworkAvailable(context) },
            onBack = onBack
        )
        return
    }

    BackHandler { onBack() }

    var listaAlertas by remember { mutableStateOf<List<AlertaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while(true) {
            try {
                val session = SessionManager(context)
                val token = session.fetchAuthToken()
                if (token != null) {
                    // Ordenamos para que la más reciente salga primero
                    val alertas = RetrofitClient.api.obtenerMisAlertas("Bearer $token")
                    listaAlertas = alertas.sortedByDescending { it.created_at }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
            delay(5000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Historial de Alertas", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)

                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF8F9FA)) // Fondo gris muy suave
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = DarkBlue)
            } else if (listaAlertas.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(top = 20.dp, bottom = 20.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Resumen Header
                    item {
                        Text(

                            text = "ÚLTIMOS EVENTOS (${listaAlertas.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                        Text(
                            text= "Registro de seguridad personal",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                    }

                    items(listaAlertas) { item ->
                        AlertTimelineCard(item)
                    }
                }
            }
        }
    }
}

@Composable
fun AlertTimelineCard(item: AlertaItem) {
    // Definir estilo según estado
    val isResolved = item.atendida
    val mainColor = if (isResolved) Color(0xFF4CAF50) else Color(0xFFD32F2F) // Verde o Rojo
    val bgColor = if (isResolved) Color(0xFFF1F8E9) else Color(0xFFFFEBEE)
    val statusText = if (isResolved) "ATENDIDA" else "AYUDA SOLICITADA"
    val icon = if (isResolved) Icons.Rounded.CheckCircle else Icons.Rounded.NotificationsActive

    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {

        // LÍNEA DE TIEMPO (Izquierda)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            // Círculo con Icono
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(mainColor)
                    .shadow(4.dp, CircleShape)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            // Línea Vertical
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(mainColor, mainColor.copy(alpha = 0.1f))
                        )
                    )
                    .padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // TARJETA DE CONTENIDO
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header: Estado y Fecha
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Badge de Estado
                    Surface(
                        color = bgColor,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.dp, mainColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = statusText,
                            color = mainColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }

                    // Fecha limpia
                    Text(
                        text = formatSimpleDate(item.created_at),
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Título
                Text(
                    text = "Alerta de Pánico Activada",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A2B46)
                )

                // Coordenadas o Detalle
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Ubicación registrada en el sistema",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = null,
            tint = Color(0xFFE0E0E0),
            modifier = Modifier.size(100.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Sin Historial",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Text(
            text = "Aún no has emitido ninguna alerta.",
            fontSize = 14.sp,
            color = Color.Gray.copy(alpha = 0.7f)
        )
    }
}

// Función auxiliar para formatear la fecha (Quita segundos y T)
fun formatSimpleDate(dateString: String): String {
    return try {
        // Convierte "2025-12-06T14:30:00.000000Z" a "2025-12-06 14:30"
        dateString.replace("T", " ").take(16)
    } catch (e: Exception) {
        dateString
    }
}