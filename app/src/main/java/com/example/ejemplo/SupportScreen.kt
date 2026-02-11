package com.example.ejemplo

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val DarkBlue = Color(0xFF1A2B46)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayuda y Soporte", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(16.dp)
        ) {
            // Tarjeta de Información
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, null, tint = DarkBlue, modifier = Modifier.size(50.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Unidad de Gestión de Riesgos", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Universidad Estatal de Bolívar", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Estamos comprometidos con tu seguridad. Utiliza los siguientes canales en caso de dudas sobre los protocolos o fallas en la aplicación.",
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Contactos de Emergencia", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkBlue)
            Spacer(modifier = Modifier.height(8.dp))

            // Botones de Acción
            SupportOptionItem(Icons.Default.Call, "Llamar al ECU 911") {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:911"))
                context.startActivity(intent)
            }

            SupportOptionItem(Icons.Default.Call, "Seguridad UEB (Campus)") {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:032980000")) // Número ficticio de la U
                context.startActivity(intent)
            }

            SupportOptionItem(Icons.Default.Email, "Soporte Técnico App") {
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:soporte@ueb.edu.ec"))
                context.startActivity(intent)
            }

            SupportOptionItem(Icons.Default.Map, "Ubicación Oficina UGR") {
                // Abre Google Maps
                val gmmIntentUri = Uri.parse("geo:-1.6028,-79.0069?q=Universidad Estatal de Bolivar")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                try { context.startActivity(mapIntent) } catch (e: Exception) {}
            }
        }
    }
}

@Composable
fun SupportOptionItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = Color(0xFF1976D2))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}