package com.example.ejemplo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ejemplo.components.NoInternetScreen
import com.example.ejemplo.utils.NetworkUtils
import com.example.ejemplo.data.ReporteItem
import com.example.ejemplo.data.RetrofitClient
import com.example.ejemplo.data.SessionManager
import kotlinx.coroutines.delay
import com.example.ejemplo.data.Config

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsScreen(onBack: () -> Unit) {
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

    var listaReportes by remember { mutableStateOf<List<ReporteItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Estado para el modal de detalle
    var reporteSeleccionado by remember { mutableStateOf<ReporteItem?>(null) }

    LaunchedEffect(Unit) {
        while(true) {
            try {
                val session = SessionManager(context)
                val token = session.fetchAuthToken()
                if (token != null) {
                    val reportes = RetrofitClient.api.obtenerMisReportes("Bearer $token")
                    listaReportes = reportes.sortedByDescending { it.created_at }
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
                        Text("Mis Reportes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
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
                .background(Color(0xFFF8F9FA))
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = DarkBlue)
            } else if (listaReportes.isEmpty()) {
                EmptyReportsState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Historial de incidentes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        Text(
                            text = "TOCA PARA VER DETALLES (${listaReportes.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }
                    items(listaReportes) { reporte ->
                        ReporteStylishCard(
                            reporte = reporte,
                            onClick = { reporteSeleccionado = reporte }
                        )
                    }
                }
            }

            // --- MODAL DE DETALLE ---
            if (reporteSeleccionado != null) {
                ReportDetailDialog(
                    reporte = reporteSeleccionado!!,
                    onDismiss = { reporteSeleccionado = null }
                )
            }
        }
    }
}

@Composable
fun ReporteStylishCard(reporte: ReporteItem, onClick: () -> Unit) {
    val (colorEstado, textoEstado, bgEstado) = when(reporte.estado) {
        "resuelto" -> Triple(Color(0xFF4CAF50), "RESUELTO", Color(0xFFE8F5E9))
        "en_curso" -> Triple(Color(0xFF2196F3), "EN PROCESO", Color(0xFFE3F2FD))
        else -> Triple(Color(0xFFFFA726), "PENDIENTE", Color(0xFFFFF3E0))
    }
    val iconoTipo = getIconForType(reporte.tipo)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(modifier = Modifier.width(6.dp).fillMaxHeight().background(colorEstado))

            Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFF5F7FA))
                    ) {
                        Icon(imageVector = iconoTipo, contentDescription = null, tint = Color(0xFF1A2B46), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = reporte.tipo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A2B46))
                        Text(text = formatReportDate(reporte.created_at), fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = reporte.descripcion, fontSize = 14.sp, color = Color.DarkGray, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = bgEstado, shape = RoundedCornerShape(50), border = androidx.compose.foundation.BorderStroke(1.dp, colorEstado.copy(alpha = 0.2f))) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colorEstado))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = textoEstado, color = colorEstado, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportDetailDialog(reporte: ReporteItem, onDismiss: () -> Unit) {
    val (colorEstado, textoEstado, bgEstado) = when(reporte.estado) {
        "resuelto" -> Triple(Color(0xFF4CAF50), "RESUELTO", Color(0xFFE8F5E9))
        "en_curso" -> Triple(Color(0xFF2196F3), "EN PROCESO", Color(0xFFE3F2FD))
        else -> Triple(Color(0xFFFFA726), "PENDIENTE", Color(0xFFFFF3E0))
    }

    // --- CORRECCIÓN DE URL PARA LA IMAGEN ---
    val imagenUrlCorregida = construirUrlImagen(reporte.imagen_url)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // --- HEADER CON IMAGEN ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp) // Hice la imagen un poco más alta
                        .background(Color.LightGray)
                ) {
                    if (imagenUrlCorregida != null) {
                        AsyncImage(
                            model = imagenUrlCorregida, // Usamos la URL corregida
                            contentDescription = "Evidencia",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0xFFECEFF1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.ImageNotSupported, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Text("Sin Foto", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color.White)
                    }
                }

                // --- CONTENIDO ---
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Surface(color = bgEstado, shape = RoundedCornerShape(8.dp), modifier = Modifier.align(Alignment.Start)) {
                        Text(text = textoEstado, color = colorEstado, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = reporte.tipo, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A2B46))

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CalendarToday, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = reporte.created_at.replace("T", " ").take(16), fontSize = 14.sp, color = Color.Gray)
                    }

                    Divider(modifier = Modifier.padding(vertical = 24.dp), color = Color.LightGray.copy(alpha = 0.5f))

                    Text(text = "Descripción", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                    Text(text = reporte.descripcion, fontSize = 16.sp, color = Color(0xFF37474F), lineHeight = 24.sp)

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(Color(0xFFF5F7FA), RoundedCornerShape(12.dp)).padding(12.dp)) {
                        Icon(Icons.Default.LocationOn, null, tint = Color(0xFF1A2B46))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Ubicación registrada", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A2B46))
                            Text("Lat: ${reporte.latitud?.take(7) ?: "N/A"}, Lng: ${reporte.longitud?.take(7) ?: "N/A"}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyReportsState() {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(imageVector = Icons.Rounded.Assignment, contentDescription = null, tint = Color(0xFFE0E0E0), modifier = Modifier.size(100.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Sin Reportes", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        Text(text = "Tu historial de incidentes aparecerá aquí.", fontSize = 14.sp, color = Color.Gray.copy(alpha = 0.7f))
    }
}

fun getIconForType(tipo: String): ImageVector {
    return when {
        tipo.contains("Robo", true) -> Icons.Rounded.Security
        tipo.contains("Médica", true) -> Icons.Rounded.LocalHospital
        tipo.contains("Incendio", true) -> Icons.Rounded.LocalFireDepartment
        tipo.contains("Acoso", true) -> Icons.Rounded.RecordVoiceOver
        tipo.contains("Infraestructura", true) -> Icons.Rounded.Construction
        else -> Icons.Rounded.ReportProblem
    }
}

fun formatReportDate(dateString: String): String {
    return try { dateString.replace("T", " ").take(16) } catch (e: Exception) { dateString }
}

// --- FUNCIÓN CLAVE PARA ARREGLAR LA URL ---
fun construirUrlImagen(path: String?): String? {
    if (path.isNullOrEmpty()) return null

    // Si ya viene completa (ej: desde Firebase o URL externa), no la tocamos
    if (path.startsWith("http")) return path

    // Usamos la URL centralizada de Config
    return Config.IMAGES_URL + path
}