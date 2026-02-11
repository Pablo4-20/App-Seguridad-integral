package com.example.ejemplo

import android.graphics.Color as AndroidColor
import android.text.method.LinkMovementMethod
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
import androidx.compose.runtime.Composable
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticiaDetailScreen(noticia: Noticia, onBack: () -> Unit) {
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
            // 1. IMAGEN GRANDE (Estilo Póster)
            if (noticia.imagen_url != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = noticia.imagen_url,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
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
                    Text(
                        text = "Publicado el: ${noticia.created_at}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. CONTENIDO HTML (INTERPRETADO)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp)) // Gris suave
                    .padding(24.dp)
            ) {
                // Usamos AndroidView para renderizar HTML nativo
                AndroidView(
                    factory = { context ->
                        TextView(context).apply {
                            textSize = 15f // Tamaño de letra
                            setTextColor(AndroidColor.parseColor("#424242")) // Color Gris Oscuro
                            // Habilitar links si el HTML tiene <a>
                            movementMethod = LinkMovementMethod.getInstance()
                        }
                    },
                    update = { textView ->
                        // Convertir HTML a Spanned para que Android lo entienda
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