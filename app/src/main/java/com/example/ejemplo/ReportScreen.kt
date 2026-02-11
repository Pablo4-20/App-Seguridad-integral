package com.example.ejemplo

import android.Manifest
import android.location.Geocoder
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border // <--- ESTA ERA LA IMPORTACIÓN QUE FALTABA
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.ejemplo.components.NoInternetScreen
import com.example.ejemplo.utils.NetworkUtils
import com.example.ejemplo.utils.VibrationUtils
import com.example.ejemplo.data.RetrofitClient
import com.example.ejemplo.data.SessionManager
import com.example.ejemplo.data.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // --- Lógica de Conexión ---
    var isConnected by remember { mutableStateOf(NetworkUtils.isNetworkAvailable(context)) }
    if (!isConnected) {
        NoInternetScreen(
            onRetry = { isConnected = NetworkUtils.isNetworkAvailable(context) },
            onBack = { onBack() }
        )
        return
    }

    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(context) }
    val settingsManager = remember { SettingsManager(context) }

    BackHandler { onBack() }

    // --- Colores y Estilos ---
    val PrimaryColor = Color(0xFF1A2B46)
    val AccentColor = Color(0xFFD32F2F)
    val BackgroundColor = Color(0xFFF5F7FA)

    // --- Estados del Formulario ---
    var tipoSeleccionado by remember { mutableStateOf("") }
    var otroTipoTexto by remember { mutableStateOf("") }

    var latitud by remember { mutableStateOf("") }
    var longitud by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }

    // Estados de Cámara
    var fotoUri by remember { mutableStateOf<Uri?>(null) }
    var tempUri by remember { mutableStateOf<Uri?>(null) }
    var fotoFile by remember { mutableStateOf<File?>(null) }

    var isLoading by remember { mutableStateOf(false) }

    // --- DEFINICIÓN DE TIPOS CON ICONOS ---
    data class TipoIncidente(val nombre: String, val icono: ImageVector)

    val tiposList = listOf(
        TipoIncidente("Robo", Icons.Rounded.Security),
        TipoIncidente("Emergencia Médica", Icons.Rounded.MedicalServices),
        TipoIncidente("Acoso", Icons.Rounded.VisibilityOff),
        TipoIncidente("Incendio", Icons.Rounded.Whatshot),
        TipoIncidente("Infraestructura", Icons.Rounded.Build),
        TipoIncidente("Otro", Icons.Rounded.Add)
    )

    // --- Lógica de Cámara ---
    fun crearArchivoImagen(): File {
        val nombreArchivo = "JPEG_${System.currentTimeMillis()}_"
        val directorio = context.cacheDir
        return File.createTempFile(nombreArchivo, ".jpg", directorio).apply {
            fotoFile = this
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            fotoUri = tempUri
            Toast.makeText(context, "Evidencia capturada", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncherCamera = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            try {
                val file = crearArchivoImagen()
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                tempUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Error cámara", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Lógica de GPS ---
    fun obtenerDireccion(lat: Double, lng: Double) {
        scope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                @Suppress("DEPRECATION")
                val direcciones = geocoder.getFromLocation(lat, lng, 1)

                if (!direcciones.isNullOrEmpty()) {
                    val address = direcciones[0]
                    val direccionCompleta = address.getAddressLine(0)
                    val fallback = address.thoroughfare ?: address.featureName ?: "Ubicación detectada"
                    withContext(Dispatchers.Main) {
                        direccion = direccionCompleta ?: fallback
                    }
                } else {
                    withContext(Dispatchers.Main) { direccion = "Ubicación sin dirección (Lat: $lat)" }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { direccion = "Error obteniendo dirección" }
            }
        }
    }

    val permissionLauncherGPS = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            Toast.makeText(context, "Localizando...", Toast.LENGTH_SHORT).show()
            scope.launch {
                val coords = locationService.getUserLocation()
                if (coords != null) {
                    latitud = coords.first
                    longitud = coords.second
                    obtenerDireccion(latitud.toDouble(), longitud.toDouble())
                    Toast.makeText(context, "Ubicación encontrada", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No se pudo obtener ubicación", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- UI ---
    Scaffold(
        containerColor = BackgroundColor,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Nuevo Reporte", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = PrimaryColor,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        // Contenedor principal
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // 1. SELECTOR DE TIPO
                SectionHeader("¿Qué está sucediendo?", Icons.Default.Warning)

                Column {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(tiposList) { item ->
                            val isSelected = tipoSeleccionado == item.nombre

                            FilterChip(
                                selected = isSelected,
                                onClick = { tipoSeleccionado = item.nombre },
                                label = { Text(item.nombre) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = item.icono,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryColor,
                                    selectedLabelColor = Color.White,
                                    selectedLeadingIconColor = Color.White,
                                    containerColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if(isSelected) PrimaryColor else Color.LightGray,
                                    enabled = true,
                                    selected = isSelected
                                )
                            )
                        }
                    }

                    // Campo "Otro" animado
                    AnimatedVisibility(
                        visible = tipoSeleccionado == "Otro",
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        OutlinedTextField(
                            value = otroTipoTexto,
                            onValueChange = { otroTipoTexto = it },
                            placeholder = { Text("Especifique el tipo de incidente") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryColor,
                                unfocusedBorderColor = Color.LightGray,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )
                    }
                }

                // 2. UBICACIÓN (Con OpenStreetMap)
                SectionHeader("Ubicación del incidente", Icons.Rounded.LocationOn)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header de coordenadas
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).background(if(latitud.isNotEmpty()) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(if(latitud.isNotEmpty()) Icons.Default.Check else Icons.Default.PriorityHigh, null, tint = if(latitud.isNotEmpty()) Color(0xFF2E7D32) else Color(0xFFC62828))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if(direccion.isNotEmpty()) direccion else if(latitud.isNotEmpty()) "Coordenadas fijadas" else "Ubicación pendiente",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    fontSize = 14.sp
                                )
                                if (latitud.isNotEmpty()) {
                                    Text("$latitud, $longitud", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            IconButton(onClick = { permissionLauncherGPS.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) {
                                Icon(Icons.Rounded.GpsFixed, "Actualizar", tint = PrimaryColor)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- MAPA VISUAL (OSM) ---
                        val latDouble = latitud.toDoubleOrNull() ?: 0.0
                        val lngDouble = longitud.toDoubleOrNull() ?: 0.0

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp)) // Ahora funcionará
                        ) {
                            OsmMap(
                                latitud = latDouble,
                                longitud = lngDouble,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // 3. EVIDENCIA
                SectionHeader("Evidencia fotográfica", Icons.Rounded.AddAPhoto)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(200.dp).clickable { permissionLauncherCamera.launch(Manifest.permission.CAMERA) },
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (fotoUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(fotoUri),
                                contentDescription = "Evidencia",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).background(Color.Black.copy(alpha=0.6f), RoundedCornerShape(8.dp)).padding(horizontal=12.dp, vertical=6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cambiar", color = Color.White, fontSize = 12.sp)
                                }
                            }
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Rounded.AddAPhoto, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Toque para tomar foto", color = Color.Gray, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // 4. DETALLES
                SectionHeader("Detalles adicionales", Icons.Default.Description)
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    placeholder = { Text("Describa brevemente lo sucedido...", color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }

            // BOTÓN ENVIAR
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(colors = listOf(Color.Transparent, BackgroundColor, BackgroundColor)))
                    .padding(20.dp)
            ) {
                Button(
                    onClick = {
                        val tipoFinal = if (tipoSeleccionado == "Otro") otroTipoTexto.trim() else tipoSeleccionado

                        if (latitud.isEmpty()) {
                            Toast.makeText(context, "Por favor, danos tu ubicación", Toast.LENGTH_SHORT).show()
                        } else if (tipoFinal.isEmpty() || tipoSeleccionado.isEmpty()) {
                            Toast.makeText(context, "Seleccione o especifique el tipo", Toast.LENGTH_SHORT).show()
                        } else {
                            isLoading = true
                            scope.launch {
                                try {
                                    val session = SessionManager(context)
                                    val token = session.fetchAuthToken()
                                    if (token != null) {
                                        val tipoPart = tipoFinal.toRequestBody("text/plain".toMediaTypeOrNull())
                                        val descCompleta = "Dirección: $direccion \n\n $descripcion"
                                        val descPart = descCompleta.toRequestBody("text/plain".toMediaTypeOrNull())
                                        val latPart = latitud.toRequestBody("text/plain".toMediaTypeOrNull())
                                        val lngPart = longitud.toRequestBody("text/plain".toMediaTypeOrNull())

                                        var fotoPart: MultipartBody.Part? = null
                                        fotoFile?.let { file ->
                                            val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                                            fotoPart = MultipartBody.Part.createFormData("foto", file.name, requestFile)
                                        }

                                        RetrofitClient.api.enviarReporte(
                                            "Bearer $token", tipoPart, descPart, latPart, lngPart, fotoPart
                                        )
                                        Toast.makeText(context, "¡Reporte enviado!", Toast.LENGTH_LONG).show()
                                        if (settingsManager.leerVibReporte()) VibrationUtils.vibrar(context, 200)
                                        onBack()
                                    } else {
                                        Toast.makeText(context, "Sesión expirada", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Error: Verifique su conexión", Toast.LENGTH_SHORT).show()
                                } finally { isLoading = false }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).shadow(8.dp, RoundedCornerShape(16.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentColor),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isLoading
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else {
                        Text("ENVIAR REPORTE", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Send, null)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)) {
        Icon(icon, contentDescription = null, tint = Color(0xFF1A2B46), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A2B46))
    }
}