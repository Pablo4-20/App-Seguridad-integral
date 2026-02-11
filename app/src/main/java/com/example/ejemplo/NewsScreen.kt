package com.example.ejemplo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ejemplo.data.Noticia
import com.example.ejemplo.data.RetrofitClient
import com.example.ejemplo.data.SessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsScreen(tipoFiltro: String, onBack: () -> Unit) { // <--- AHORA RECIBE UN FILTRO
    val context = LocalContext.current
    val DarkBlue = Color(0xFF1A2B46)

    // Estados
    var listaCompleta by remember { mutableStateOf<List<Noticia>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Título dinámico según el filtro
    val tituloPantalla = when(tipoFiltro) {
        "protocolo" -> "Protocolos de Seguridad"
        "recomendacion" -> "Recomendaciones"
        "notificacion" -> "Alertas y Notificaciones"
        else -> "Noticias Institucionales"
    }

    // Cargar datos del servidor
    LaunchedEffect(Unit) {
        try {
            val session = SessionManager(context)
            val token = session.fetchAuthToken()
            if (token != null) {
                val respuesta = RetrofitClient.api.obtenerNoticias("Bearer $token")
                listaCompleta = respuesta
            }
            isLoading = false
        } catch (e: Exception) {
            errorMsg = "Error al cargar: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tituloPantalla, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBlue,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().background(Color(0xFFF5F5F5))) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMsg != null) {
                Text(text = errorMsg!!, modifier = Modifier.align(Alignment.Center), color = Color.Red)
            } else {
                // CONTENIDO FILTRADO
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Filtramos la lista para mostrar SOLO lo que el usuario pidió ver
                    val itemsFiltrados = listaCompleta.filter {
                        // Comparamos ignorando mayúsculas
                        it.tipo.equals(tipoFiltro, ignoreCase = true)
                    }

                    if (itemsFiltrados.isEmpty()) {
                        Text(
                            "No hay $tituloPantalla disponibles por el momento.",
                            color = Color.Gray,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        itemsFiltrados.forEach { noticia ->
                            NoticiaItem(noticia)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }
    }
}

// --- ITEM INDIVIDUAL (TARJETA) ---
@Composable
fun NoticiaItem(noticia: Noticia) {
    var itemExpanded by remember { mutableStateOf(false) }

    // Color del borde según tipo
    val colorBorde = when(noticia.tipo) {
        "protocolo" -> Color(0xFFD32F2F)
        "recomendacion" -> Color(0xFF388E3C)
        "notificacion" -> Color(0xFFFFA000)
        else -> Color(0xFF1976D2)
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, colorBorde.copy(alpha = 0.5f)), // Borde sutil
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { itemExpanded = !itemExpanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Título y Fecha
            Text(text = noticia.titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Publicado: ${noticia.created_at.take(10)}", fontSize = 10.sp, color = Color.Gray)

            // Contenido (Expandible)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = noticia.contenido,
                fontSize = 14.sp,
                color = Color.DarkGray,
                maxLines = if (itemExpanded) Int.MAX_VALUE else 3, // Muestra 3 líneas o todo
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            // Imagen (Solo si está expandido y existe)
            if (noticia.imagen_url != null && itemExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                AsyncImage(
                    model = noticia.imagen_url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color.LightGray, RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }

            // Texto "Ver más" o "Ver menos"
            Text(
                text = if(itemExpanded) "Ver menos" else "Ver más...",
                fontSize = 12.sp,
                color = colorBorde,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp).align(Alignment.End)
            )
        }
    }
}