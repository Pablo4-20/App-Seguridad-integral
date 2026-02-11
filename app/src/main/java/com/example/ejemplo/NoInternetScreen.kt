package com.example.ejemplo.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack // <--- IMPORTANTE
import androidx.compose.material.icons.rounded.SignalWifiOff
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ejemplo.R

@Composable
fun NoInternetScreen(
    onRetry: () -> Unit, onBack: () -> Unit
) {
    val UebBlue = Color(0xFF1A2B46)
    val ErrorRed = Color(0xFFD32F2F)
    val TextColor = Color(0xFF333333)

    // Usamos Box para poder poner el botón de atrás en la esquina superior
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        // --- 1. BOTÓN DE VOLVER (ARRIBA A LA IZQUIERDA) ---
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart) // Pegado arriba izquierda
                .size(48.dp)
                .offset(x = (-12).dp, y = (-12).dp) // Pequeño ajuste para margen visual
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Volver",
                tint = TextColor,
                modifier = Modifier.size(28.dp)
            )
        }

        // --- 2. CONTENIDO CENTRAL (IGUAL QUE ANTES) ---
        Column(
            modifier = Modifier.align(Alignment.Center), // Centrado en la pantalla
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "¡Sin Conexión!",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = ErrorRed
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No podemos conectarnos al servidor.\nVerifica tu acceso a internet.",
                fontSize = 16.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Intentamos cargar tu imagen, si no, usamos icono

                Image(
                    painter = painterResource(id = R.drawable.sin_internet),
                    contentDescription = "Sin conexión",
                    modifier = Modifier.size(220.dp)
                )




            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Intenta realizar lo siguiente:",
                fontSize = 14.sp,
                color = TextColor,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            StepItem("Revisa tu conexión WiFi o Datos", UebBlue)
            StepItem("Comprueba el 'Modo Avión'", UebBlue)

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = UebBlue),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text("REINTENTAR", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun StepItem(text: String, iconColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Spacer(modifier = Modifier.width(10.dp))
        Icon(
            imageVector = Icons.Rounded.Verified,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text, color = Color.Gray, fontSize = 15.sp)
    }
}