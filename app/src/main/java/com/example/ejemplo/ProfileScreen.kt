package com.example.ejemplo

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ejemplo.data.SessionManager

@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToProfile: () -> Unit // <--- NUEVO PARAMETRO
) {
    val context = LocalContext.current
    val session = SessionManager(context)
    val userName = session.fetchUserName() ?: "Usuario"
    val userInitial = userName.firstOrNull()?.uppercase() ?: "U"
    val DarkBlue = Color(0xFF1A2B46)

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // CABECERA CLICKEABLE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkBlue)
                .clickable { onNavigateToProfile() } // <--- AQUÍ SE ACTIVA EL CLICK
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .background(Color.White, CircleShape)
                        .border(2.dp, DarkBlue, CircleShape)
                ) {
                    Text(userInitial.toString(), color = DarkBlue, fontSize = 36.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(userName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Ver Perfil Completo >", color = Color(0xFFFFA000), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // MENU DE OPCIONES
        Column(modifier = Modifier.padding(24.dp)) {
            Text("General", color = Color.Gray, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

            ProfileOptionItem("Mis Reportes", Color.Black) { onNavigateToHistory() }
            Divider(color = Color.LightGray.copy(alpha = 0.5f))

            ProfileOptionItem("Historial de Alertas", Color.Black) { onNavigateToAlerts() }
            Divider(color = Color.LightGray.copy(alpha = 0.5f))

            ProfileOptionItem("Notificaciones", Color.Black) { onNavigateToSettings() }
            Divider(color = Color.LightGray.copy(alpha = 0.5f))

            ProfileOptionItem("Ayuda y Soporte", Color.Black) { onNavigateToSupport() }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cerrar Sesión", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun ProfileOptionItem(text: String, textColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = text, color = textColor, fontSize = 16.sp)
        Icon(Icons.Default.ArrowForwardIos, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
    }
}