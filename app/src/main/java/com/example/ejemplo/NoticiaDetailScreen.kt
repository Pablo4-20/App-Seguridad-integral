package com.example.ejemplo

import android.graphics.Color as AndroidColor
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.TextView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import com.example.ejemplo.data.Noticia
import com.example.ejemplo.data.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// --- COMPONENTE CONTROLADOR (Decide qué mostrar) ---
@Composable
fun NoticiaDetailScreen(
    noticiaObj: Noticia?,   // Opción A: Objeto ya listo
    noticiaId: String?,     // Opción B: ID para descargar
    onBack: () -> Unit
) {
    // 1. Si ya tenemos el objeto, mostramos el contenido directamente
    if (noticiaObj != null) {
        NoticiaContent(noticia = noticiaObj, onBack = onBack)
    }
    // 2. Si solo tenemos el ID, descargamos los datos primero
    else if (noticiaId != null) {
        var noticiaDescargada by remember { mutableStateOf<Noticia?>(null) }
        var cargando by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(noticiaId) {
            try {
                val idInt = noticiaId.toInt()
                RetrofitClient.api.getNoticia(idInt).enqueue(object : Callback<Noticia> {
                    override fun onResponse(call: Call<Noticia>, response: Response<Noticia>) {
                        if (response.isSuccessful && response.body() != null) {
                            noticiaDescargada = response.body()
                        } else {
                            error = "No se encontró la noticia"
                        }
                        cargando = false
                    }

                    override fun onFailure(call: Call<Noticia>, t: Throwable) {
                        error = "Error de conexión: ${t.message}"
                        cargando = false
                    }
                })
            } catch (e: Exception) {
                error = "ID inválido"
                cargando = false
            }
        }

        if (cargando) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = error!!, color = Color.Red)
                Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                    Text("Volver")
                }
            }
        } else {
            // ¡Éxito! Mostramos el contenido
            NoticiaContent(noticia = noticiaDescargada!!, onBack = onBack)
        }
    }
}

// --- UI DEL CONTENIDO (Tu diseño original intacto) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticiaContent(noticia: Noticia, onBack: () -> Unit) {
    BackHandler { onBack() }
    val DarkBlue = Color(0xFF1A2B46)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
        ) {
            // 1. IMAGEN GRANDE
            if (noticia.imagen_url != null) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = noticia.imagen_url,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            // 2. TÍTULO
            Text(
                text = noticia.titulo,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 3. FECHAS
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Publicado el: ${noticia.created_at}", fontSize = 12.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. CONTENIDO HTML
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                AndroidView(
                    factory = { context ->
                        TextView(context).apply {
                            textSize = 15f
                            setTextColor(AndroidColor.parseColor("#424242"))
                            movementMethod = LinkMovementMethod.getInstance()
                        }
                    },
                    update = { textView ->
                        textView.text = HtmlCompat.fromHtml(
                            noticia.contenido,
                            HtmlCompat.FROM_HTML_MODE_COMPACT
                        )
                    }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}