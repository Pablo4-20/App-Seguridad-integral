package com.example.ejemplo

import android.graphics.Color as AndroidColor
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape // <--- ESTE ERA EL IMPORT FALTANTE
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ejemplo.components.NoInternetScreen
import com.example.ejemplo.utils.NetworkUtils
import com.example.ejemplo.data.PuntoMapa
import com.example.ejemplo.data.RetrofitClient
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvacuationScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // --- VERIFICACIÓN DE INTERNET ---
    var isConnected by remember { mutableStateOf(NetworkUtils.isNetworkAvailable(context)) }
    if (!isConnected) {
        NoInternetScreen(
            onRetry = { isConnected = NetworkUtils.isNetworkAvailable(context) },
            onBack = { onBack() } // Usamos la navegación para volver al menú
        )
        return
    }
    // -------------------------------
    val DarkBlue = Color(0xFF1A2B46)

    // 1. Configurar OSM (Obligatorio)
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", 0))
    }

    // 2. Estado de Puntos
    var puntosReales by remember { mutableStateOf<List<PuntoMapa>>(emptyList()) }

    // Filtros de Capas
    var showZonas by remember { mutableStateOf(true) }
    var showPuntos by remember { mutableStateOf(true) }

    // 3. Cargar desde API
    LaunchedEffect(Unit) {
        try {
            val resultado = RetrofitClient.api.obtenerPuntosMapa()
            puntosReales = resultado
            if (resultado.isNotEmpty()) {
                // Mensaje discreto de éxito
                // Toast.makeText(context, "Mapa actualizado", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Error cargando puntos", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rutas de Evacuación", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBlue)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {

            // --- MAPA ---
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        // Usamos la capa Humanitaria para mejor visibilidad
                        val humanitarianSource = XYTileSource(
                            "Humanitarian", 0, 20, 256, ".png",
                            arrayOf("https://a.tile.openstreetmap.fr/hot/", "https://b.tile.openstreetmap.fr/hot/")
                        )
                        setTileSource(humanitarianSource)
                        setMultiTouchControls(true)

                        minZoomLevel = 5.0
                        maxZoomLevel = 22.0

                        // CENTRO DE LA UEB
                        val uebCenter = GeoPoint(-1.5709124271569128, -79.00723995356827)
                        controller.setZoom(17.0)
                        controller.setCenter(uebCenter)
                    }
                },
                update = { mapView ->
                    mapView.overlays.clear()
                    var primerPunto: GeoPoint? = null

                    puntosReales.forEach { punto ->
                        try {
                            // Conversión segura de coordenadas (Coma a Punto)
                            val latStr = punto.latitud.toString().replace(",", ".").trim()
                            val lngStr = punto.longitud.toString().replace(",", ".").trim()

                            val lat = latStr.toDoubleOrNull() ?: 0.0
                            val lng = lngStr.toDoubleOrNull() ?: 0.0

                            if (lat != 0.0) {
                                val geoPoint = GeoPoint(lat, lng)
                                if (primerPunto == null) primerPunto = geoPoint

                                val mostrar = (punto.tipo == "peligro" && showZonas) || (punto.tipo == "seguro" && showPuntos)

                                if (mostrar) {
                                    if (punto.tipo == "peligro") {
                                        // ZONA ROJA (Círculo)
                                        val circulo = Polygon().apply {
                                            points = Polygon.pointsAsCircle(geoPoint, 30.0) // 30m radio
                                            fillPaint.color = AndroidColor.parseColor("#55FF0000") // Rojo transparente
                                            outlinePaint.color = AndroidColor.RED
                                            outlinePaint.strokeWidth = 3f
                                            title = punto.titulo
                                        }
                                        mapView.overlays.add(circulo)
                                    } else {
                                        // PUNTO SEGURO (Marcador)
                                        val marcador = Marker(mapView).apply {
                                            position = geoPoint
                                            title = punto.titulo
                                            snippet = "Punto de Encuentro"
                                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                        }
                                        mapView.overlays.add(marcador)
                                    }
                                }
                            }
                        } catch (e: Exception) { }
                    }

                    // Centrar si hay puntos nuevos
                    if (primerPunto != null) {
                        // Opcional: mapView.controller.animateTo(primerPunto)
                    }

                    mapView.invalidate()
                },
                modifier = Modifier.fillMaxSize()
            )

            // --- CONTROLES DE CAPAS ---
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = showZonas,
                    onClick = { showZonas = !showZonas },
                    label = { Text("Zonas Peligro") },
                    leadingIcon = {
                        if (showZonas) Icon(Icons.Default.Check, null)
                        else Icon(Icons.Default.Warning, null)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFD32F2F),
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                        containerColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = showPuntos,
                    onClick = { showPuntos = !showPuntos },
                    label = { Text("Puntos Seguros") },
                    leadingIcon = {
                        if (showPuntos) Icon(Icons.Default.Check, null)
                        else Icon(Icons.Default.Flag, null)
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF1976D2),
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                        containerColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            // --- LEYENDA INFERIOR ---
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Simbología:", style = MaterialTheme.typography.labelLarge, color = Color.Black)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Leyenda Seguro
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = Color(0xFF1976D2), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Punto Seguro", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                        // Leyenda Peligro (Círculo con borde)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(Color(0x55FF0000), CircleShape)
                                    .border(1.dp, Color.Red, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Zona Peligro", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}