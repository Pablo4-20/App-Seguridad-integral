package com.example.ejemplo

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.text.TextUtils
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import com.example.ejemplo.components.NoInternetScreen
import com.example.ejemplo.utils.NetworkUtils
import com.example.ejemplo.data.Noticia
import com.example.ejemplo.data.RetrofitClient
import com.example.ejemplo.data.SessionManager

@Composable
fun NewsListContent(filtro: String,onBack: () -> Unit, onNoticiaClick: (Noticia) -> Unit) {
    val context = LocalContext.current
    var isConnected by remember { mutableStateOf(NetworkUtils.isNetworkAvailable(context)) }

    if (!isConnected) {
        NoInternetScreen(
            onRetry = { isConnected = NetworkUtils.isNetworkAvailable(context) },
            onBack = { onBack() }
        )
        return
    }

    var lista by remember { mutableStateOf<List<Noticia>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(filtro) {
        isLoading = true
        try {
            val session = SessionManager(context)
            val token = session.fetchAuthToken()
            if (token != null) {
                // 1. Obtener ambos orígenes
                val noticiasRes = RetrofitClient.api.obtenerNoticias("Bearer $token")
                val comunicadosRes = try { RetrofitClient.api.obtenerComunicados() } catch(e:Exception) { emptyList() }

                // 2. Mapear PDFs a formato de Noticia
                val pdfsMapeados = comunicadosRes.map { c ->
                    val catFront = when(c.categoria) {
                        "plan_estudiantil" -> "noticia"
                        "protocolos_seguridad" -> "protocolo"
                        "recomendaciones" -> "recomendacion"
                        "mochila_emergencia" -> "mochila"
                        "alertas_avisos" -> "notificacion"
                        else -> "noticia"
                    }
                    Noticia(
                        id = c.id + 10000, // Evitar colisión de IDs
                        titulo = c.titulo,
                        contenido = "<b>DOCUMENTO PDF</b><br/>Toca para abrir y descargar.",
                        tipo = catFront,
                        imagen_url = null,
                        created_at = c.created_at,
                        isPdf = true,
                        pdfUrl = c.archivo_pdf
                    )
                }

                // 3. Unir, ordenar y filtrar
                val todas = (noticiasRes + pdfsMapeados).sortedByDescending { it.created_at }
                lista = todas.filter { it.tipo.equals(filtro, ignoreCase = true) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else if (lista.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay información disponible.", color = Color.Gray) }
    } else {
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(lista) { noticia ->
                NoticiaHorizontalCard(noticia, onClick = { onNoticiaClick(noticia) })
            }
        }
    }
}

@Composable
fun NoticiaHorizontalCard(noticia: Noticia, onClick: () -> Unit) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clickable {
                // --- MAGIA: SI ES PDF, ABRIR EN NAVEGADOR ---
                if (noticia.isPdf && noticia.pdfUrl != null) {
                    try {
                        // Extraemos la URL base quitando "api/" de tu configuración
                        val fileName = noticia.pdfUrl?.substringAfterLast("/")
                        val finalUrl = "http://sib.swueb.net/api/documento/$fileName"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    onClick()
                }
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE0E0E0)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {

            // --- IMAGEN O ÍCONO PDF ---
            if (noticia.isPdf) {
                Box(
                    modifier = Modifier.size(90.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFFFEBEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = Color(0xFFD32F2F), modifier = Modifier.size(45.dp))
                }
            } else if (noticia.imagen_url != null) {
                AsyncImage(
                    model = noticia.imagen_url,
                    contentDescription = null,
                    modifier = Modifier.size(90.dp).clip(RoundedCornerShape(8.dp)).background(Color.White),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(modifier = Modifier.size(90.dp).clip(RoundedCornerShape(8.dp)).background(Color.White), contentAlignment = Alignment.Center) {
                    Text("UEB", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // --- TEXTOS ---
            Column(modifier = Modifier.fillMaxHeight().weight(1f), verticalArrangement = Arrangement.SpaceBetween) {
                Text(text = noticia.titulo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.Black, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)

                AndroidView(
                    factory = { ctx -> TextView(ctx).apply { textSize = 12f; setTextColor(if(noticia.isPdf) AndroidColor.RED else AndroidColor.DKGRAY); maxLines = 2; ellipsize = TextUtils.TruncateAt.END } },
                    update = { textView -> textView.text = HtmlCompat.fromHtml(noticia.contenido, HtmlCompat.FROM_HTML_MODE_COMPACT) }
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = noticia.tipo.replaceFirstChar { it.uppercase() }, fontSize = 10.sp, color = Color.Gray)
                    Text(text = "Pub: ${noticia.created_at.take(10)}", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}