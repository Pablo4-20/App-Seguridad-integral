package com.example.ejemplo

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ejemplo.components.NoInternetScreen
import com.example.ejemplo.utils.NetworkUtils
import com.example.ejemplo.utils.VibrationUtils
import com.example.ejemplo.data.AlertaRequest
import com.example.ejemplo.data.Noticia
import com.example.ejemplo.data.ReporteItem
import com.example.ejemplo.data.RetrofitClient
import com.example.ejemplo.data.SessionManager
import com.example.ejemplo.data.SettingsManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    initialTab: Int = 0,
    onLogout: () -> Unit,
    onNavigateToReport: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToAlerts: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val DarkBlue = Color(0xFF1A2B46)
    val context = LocalContext.current

    // Gestores
    val settingsManager = remember { SettingsManager(context) }
    val notificationHelper = remember { NotificationHelper(context) }
    val session = remember { SessionManager(context) }

    // Estados de Navegación
    var selectedTab by remember { mutableIntStateOf(initialTab) }
    var activeCategory by remember { mutableStateOf<String?>(null) }
    var noticiaSeleccionada by remember { mutableStateOf<Noticia?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }

    // --- ESTADOS DE MONITOREO ---
    var isMonitoringAlert by remember { mutableStateOf(false) }
    var showHelpArrivedDialog by remember { mutableStateOf(false) }

    // Estado para mostrar el reporte en la UI (Tarjeta)
    var activeReport by remember { mutableStateOf<ReporteItem?>(null) }

    // Estado para detectar CAMBIOS de estado (Notificación)
    var lastReportStatus by remember { mutableStateOf("") }
    var showReportUpdateDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Permisos
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // --- MONITOREO GENERAL (Noticias y Reportes) - Cada 5s ---
    LaunchedEffect(Unit) {
        while(isActive) {
            try {
                if (NetworkUtils.isNetworkAvailable(context)) {
                    val token = session.fetchAuthToken()
                    if (token != null) {
                        // 1. Noticias
                        val noticias = RetrofitClient.api.obtenerNoticias("Bearer $token")
                        if (noticias.isNotEmpty()) {
                            val ultima = noticias.first()
                            val ultimoId = settingsManager.leerUltimoIdNoticia()
                            if (ultimoId == 0 || ultima.id > ultimoId) {
                                settingsManager.guardarUltimoIdNoticia(ultima.id)
                                if (settingsManager.leerNotificaciones()) {
                                    notificationHelper.mostrarNotificacion("Nueva Publicación", ultima.titulo)
                                }
                            }
                        }

                        // 2. Reportes (Incidentes)
                        val reportes = RetrofitClient.api.obtenerMisReportes("Bearer $token")
                        if (reportes.isNotEmpty()) {
                            // Tomamos el último reporte (suponiendo orden por ID o fecha)
                            val ultimoReporte = reportes.maxByOrNull { it.id }

                            if (ultimoReporte != null) {
                                // ACTUALIZAMOS LA UI (Para que se vea la tarjeta)
                                activeReport = ultimoReporte

                                // LÓGICA DE NOTIFICACIÓN DE CAMBIO
                                if (lastReportStatus.isNotEmpty() && lastReportStatus != ultimoReporte.estado) {
                                    if (ultimoReporte.estado == "en_curso") {
                                        showReportUpdateDialog = "Reporte en Proceso" to "Tu reporte está siendo atendido."
                                        VibrationUtils.vibrar(context, 500)
                                    } else if (ultimoReporte.estado == "resuelto") {
                                        showReportUpdateDialog = "Reporte Resuelto" to "Tu incidente ha sido cerrado."
                                        VibrationUtils.vibrar(context, 500)
                                    }
                                }
                                lastReportStatus = ultimoReporte.estado
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(5000) // Revisar cada 5 segundos
        }
    }

    // --- MONITOREO DE ALERTA DE PÁNICO (Rápido) ---
    LaunchedEffect(isMonitoringAlert) {
        if (isMonitoringAlert) {
            while (isActive && isMonitoringAlert) {
                try {
                    val token = session.fetchAuthToken()
                    if (token != null) {
                        val misAlertas = RetrofitClient.api.obtenerMisAlertas("Bearer $token")
                        val ultimaAlerta = misAlertas.firstOrNull()
                        if (ultimaAlerta != null && ultimaAlerta.atendida) {
                            isMonitoringAlert = false
                            showHelpArrivedDialog = true
                            VibrationUtils.vibrar(context, 1000)
                            val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 100)
                            toneG.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 1000)
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
                delay(5000)
            }
        }
    }

    BackHandler(enabled = true) {
        when {
            noticiaSeleccionada != null -> noticiaSeleccionada = null
            activeCategory != null -> activeCategory = null
            selectedTab != 0 -> selectedTab = 0
            else -> showExitDialog = true
        }
    }

    // --- CORRECCIÓN AQUÍ: Usamos los nuevos parámetros ---
    if (noticiaSeleccionada != null) {
        NoticiaDetailScreen(
            noticiaObj = noticiaSeleccionada,
            noticiaId = null, // Como ya tenemos el objeto, no necesitamos el ID
            onBack = { noticiaSeleccionada = null }
        )
        return
    }
    // ----------------------------------------------------

    if (activeCategory == "ruta_evacuacion") {
        EvacuationScreen(onBack = { activeCategory = null })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val titulo = when {
                        activeCategory != null -> when(activeCategory) {
                            "protocolo" -> "Protocolos"
                            "noticia" -> "Plan Estudiantil"
                            "mochila" -> "Mochila de Emergencia"
                            "ruta_evacuacion" -> "Rutas de Evacuación"
                            else -> "Información"
                        }
                        selectedTab == 1 -> "Recomendaciones"
                        selectedTab == 2 -> "Alertas"
                        selectedTab == 3 -> "Configuración"
                        else -> "Inicio"
                    }
                    Text(titulo, color = Color.White, fontWeight = FontWeight.Bold)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Inicio", fontSize = 10.sp) },
                    selected = selectedTab == 0 && activeCategory == null,
                    onClick = { selectedTab = 0; activeCategory = null }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.WorkspacePremium, null) },
                    label = { Text("Recom.", fontSize = 10.sp) },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; activeCategory = null }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Warning, null) },
                    label = { Text("Alertas", fontSize = 10.sp) },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; activeCategory = null }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Config.", fontSize = 10.sp) },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3; activeCategory = null }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize().background(Color(0xFFF5F5F5))) {

            if (activeCategory != null) {
                NewsListContent(
                    filtro = activeCategory!!,
                    onBack = { activeCategory = null },
                    onNoticiaClick = { noticiaSeleccionada = it }
                )
            } else {
                when(selectedTab) {
                    0 -> DashboardContent(
                        onNavigateToReport = onNavigateToReport,
                        onOpenCategory = { cat -> activeCategory = cat },
                        onSwitchTab = { tab -> selectedTab = tab },
                        settingsManager = settingsManager,
                        onAlertSent = { isMonitoringAlert = true },
                        isMonitoring = isMonitoringAlert,
                        activeReport = activeReport, // Pasamos el reporte a la UI
                        onNavigateToHistory = onNavigateToHistory
                    )
                    1 -> NewsListContent(
                        filtro = "recomendacion",
                        onBack = { selectedTab = 0 },
                        onNoticiaClick = { noticiaSeleccionada = it }
                    )
                    2 -> NewsListContent(
                        filtro = "notificacion",
                        onBack = { selectedTab = 0 },
                        onNoticiaClick = { noticiaSeleccionada = it }
                    )
                    3 -> ProfileScreen(
                        onLogout = onLogout,
                        onNavigateToHistory = onNavigateToHistory,
                        onNavigateToAlerts = onNavigateToAlerts,
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToSupport = onNavigateToSupport,
                        onNavigateToProfile = onNavigateToProfile
                    )
                }
            }

            // DIÁLOGOS
            if (showHelpArrivedDialog) {
                AlertDialog(
                    onDismissRequest = { showHelpArrivedDialog = false },
                    icon = { Icon(Icons.Default.CheckCircle, null, tint = Color.Green, modifier = Modifier.size(48.dp)) },
                    title = { Text("¡AYUDA EN CAMINO!", fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20)) },
                    text = { Text("Un administrador ha recibido tu alerta.", textAlign = TextAlign.Center) },
                    confirmButton = { Button(onClick = { showHelpArrivedDialog = false }) { Text("Entendido") } }
                )
            }

            if (showReportUpdateDialog != null) {
                AlertDialog(
                    onDismissRequest = { showReportUpdateDialog = null },
                    icon = { Icon(Icons.Default.Info, null, tint = Color(0xFF1976D2), modifier = Modifier.size(48.dp)) },
                    title = { Text(showReportUpdateDialog!!.first, fontWeight = FontWeight.Bold) },
                    text = { Text(showReportUpdateDialog!!.second, textAlign = TextAlign.Center) },
                    confirmButton = {
                        Button(onClick = {
                            showReportUpdateDialog = null
                            onNavigateToHistory()
                        }) { Text("Ver Detalles") }
                    },
                    dismissButton = { TextButton(onClick = { showReportUpdateDialog = null }) { Text("Cerrar") } }
                )
            }

            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = { Text("¿Salir?") },
                    text = { Text("¿Deseas cerrar la aplicación?") },
                    confirmButton = { Button(onClick = { (context as? Activity)?.finish() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Salir") } },
                    dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Cancelar") } }
                )
            }
        }
    }
}

@Composable
fun DashboardContent(
    onNavigateToReport: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onSwitchTab: (Int) -> Unit,
    settingsManager: SettingsManager,
    onAlertSent: () -> Unit,
    isMonitoring: Boolean,
    activeReport: ReporteItem?,
    onNavigateToHistory: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }
    val toneGenerator = remember { ToneGenerator(AudioManager.STREAM_ALARM, 100) }
    var isSendingAlert by remember { mutableStateOf(false) }

    val bannerImages = remember { listOf(R.drawable.ugr1, R.drawable.ugr2, R.drawable.ugr3) }
    val pagerState = rememberPagerState(pageCount = { bannerImages.size })

    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            if (bannerImages.isNotEmpty()) {
                val nextPage = (pagerState.currentPage + 1) % bannerImages.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    fun realizarEnvio() {
        if (!NetworkUtils.isNetworkAvailable(context)) {
            Toast.makeText(context, "⚠️ Sin conexión", Toast.LENGTH_SHORT).show()
            return
        }
        isSendingAlert = true
        if (settingsManager.leerSonido()) toneGenerator.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK)
        if (settingsManager.leerVibAlerta()) VibrationUtils.vibrar(context, 1000)

        scope.launch {
            try {
                val coords = locationService.getUserLocation()
                val lat = coords?.first ?: "-1.6028"
                val lng = coords?.second ?: "-79.0069"
                val session = SessionManager(context)
                val token = session.fetchAuthToken()

                if (token != null) {
                    RetrofitClient.api.enviarAlerta("Bearer $token", AlertaRequest(lat, lng))
                    Toast.makeText(context, "🚨 ¡AYUDA ENVIADA!", Toast.LENGTH_LONG).show()
                    onAlertSent()
                }
            } catch (e: Exception) { e.printStackTrace() }
            finally {
                if (settingsManager.leerSonido()) toneGenerator.stopTone()
                isSendingAlert = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { if (it.values.all { g -> g }) realizarEnvio() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // CARRUSEL
        Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                Image(painter = painterResource(id = bannerImages[page]), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
            Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp), horizontalArrangement = Arrangement.Center) {
                repeat(bannerImages.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                    Box(modifier = Modifier.padding(4.dp).clip(CircleShape).background(color).size(8.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- TARJETA DE ESTADO DE ALERTA ---
        if (isMonitoring) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Esperando confirmación de ayuda...", color = Color(0xFF2E7D32), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // --- TARJETA DE ESTADO DEL REPORTE (NUEVO) ---
        if (activeReport != null && activeReport.estado != "resuelto") {
            val (cardColor, textColor, label) = when (activeReport.estado) {
                "en_curso" -> Triple(Color(0xFFE3F2FD), Color(0xFF1976D2), "EN CURSO")
                else -> Triple(Color(0xFFFFF3E0), Color(0xFFF57C00), "PENDIENTE")
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = cardColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { onNavigateToHistory() }, // Clic para ver detalles
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "ESTADO DE TU REPORTE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor.copy(alpha = 0.7f))
                        Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Text(text = activeReport.tipo, fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = textColor)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        // ---------------------------------------------

        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardCard(
                title = if(isSendingAlert) "ENVIANDO..." else "EMERGENCIA",
                icon = Icons.Default.NotificationsActive,
                iconColor = if(isSendingAlert) Color.Gray else Color.Red,
                modifier = Modifier.weight(1f)
            ) {
                if(!isSendingAlert && !isMonitoring) {
                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                } else if (isMonitoring) {
                    Toast.makeText(context, "Alerta ya activa", Toast.LENGTH_SHORT).show()
                }
            }

            DashboardCard("Reportar", Icons.Default.ReportProblem, Color(0xFFFFA000), Modifier.weight(1f)) { onNavigateToReport() }
        }

        SectionHeader("ZONAS DE AMENAZAS Y RUTAS")
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardCard("Protocolos", Icons.Default.Assignment, Color(0xFF1A2B46), Modifier.weight(1f)) { onOpenCategory("protocolo") }
            DashboardCard("Rutas de\nEvacuación", Icons.Default.Map, Color(0xFF00897B), Modifier.weight(1f)) { onOpenCategory("ruta_evacuacion") }
        }

        SectionHeader("PLAN ESTUDIANTIL")
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardCard("Plan\nEstudiantil", Icons.Default.School, Color.Black, Modifier.weight(1f)) { onOpenCategory("noticia") }
            DashboardCard("Mochila de\nEmergencia", Icons.Default.Backpack, Color(0xFFFFD700), Modifier.weight(1f)) { onOpenCategory("mochila") }
        }

        SectionHeader("NAVEGACIÓN RÁPIDA")
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardCard("Recomendaciones", Icons.Default.WorkspacePremium, Color(0xFF1976D2), Modifier.weight(1f)) { onSwitchTab(1) }
            DashboardCard("Alertas", Icons.Default.Warning, Color.Red, Modifier.weight(1f)) { onSwitchTab(2) }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth())
}

@Composable
fun DashboardCard(title: String, icon: ImageVector, iconColor: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier = modifier.height(120.dp).clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}