package com.example.ejemplo

import androidx.activity.compose.BackHandler // <--- IMPORTANTE
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ejemplo.data.SettingsManager
import com.example.ejemplo.utils.VibrationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    val DarkBlue = Color(0xFF1A2B46)

    // --- CORRECCIÓN: MANEJO DEL BOTÓN ATRÁS ---
    // Esto evita que la app se cierre y te devuelve al menú anterior
    BackHandler { onBack() }
    // ------------------------------------------

    // Estados
    var notificaciones by remember { mutableStateOf(settingsManager.leerNotificaciones()) }
    var sonido by remember { mutableStateOf(settingsManager.leerSonido()) }

    var vibNoticia by remember { mutableStateOf(settingsManager.leerVibNoticia()) }
    var vibAlerta by remember { mutableStateOf(settingsManager.leerVibAlerta()) }
    var vibReporte by remember { mutableStateOf(settingsManager.leerVibReporte()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notificaciones y Ajustes", color = Color.White, fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
        ) {

            // SECCIÓN 1: GENERAL
            Text("General", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))

            SettingsSwitchItem("Recibir Comunicados", "Noticias y avisos del admin", notificaciones) {
                notificaciones = it
                settingsManager.guardarNotificaciones(it)
            }
            SettingsSwitchItem("Sonido del Sistema", "Activar sirenas y tonos", sonido) {
                sonido = it
                settingsManager.guardarSonido(it)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECCIÓN 2: VIBRACIÓN (HÁPTICO)
            Text("Respuesta Táctil (Vibración)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))

            SettingsSwitchItem("Al recibir noticia", "Vibrar cuando llega contenido nuevo", vibNoticia) {
                vibNoticia = it
                settingsManager.guardarVibNoticia(it)
                if(it) VibrationUtils.vibrar(context, 100)
            }

            SettingsSwitchItem("Al enviar alerta", "Confirmación táctil en botón de pánico", vibAlerta) {
                vibAlerta = it
                settingsManager.guardarVibAlerta(it)
                if(it) VibrationUtils.vibrar(context, 500)
            }

            SettingsSwitchItem("Al enviar reporte", "Confirmación al subir incidente", vibReporte) {
                vibReporte = it
                settingsManager.guardarVibReporte(it)
                if(it) VibrationUtils.vibrar(context, 100)
            }
        }
    }
}

@Composable
fun SettingsSwitchItem(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1A2B46))
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF1A2B46), checkedTrackColor = Color(0xFFB0BEC5))
            )
        }
    }
}