package com.example.ejemplo

import android.view.MotionEvent // <--- Importante para detectar los toques
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun OsmMap(
    latitud: Double,
    longitud: Double,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Inicializamos el MapView
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(19.0)

            // --- SOLUCIÓN MÁGICA ---
            // Esto detecta cuando pones el dedo en el mapa
            setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // Al presionar: Bloquea el scroll de la página (padre)
                        v.parent.requestDisallowInterceptTouchEvent(true)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // Al soltar: Libera el scroll para que la página funcione de nuevo
                        v.parent.requestDisallowInterceptTouchEvent(false)
                    }
                }
                false // Devolvemos 'false' para que el mapa sí procese el movimiento (zoom/pan)
            }
            // -----------------------
        }
    }

    // Ciclo de vida del mapa
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier,
        update = { map ->
            if (latitud != 0.0 && longitud != 0.0) {
                val punto = GeoPoint(latitud, longitud)

                map.controller.animateTo(punto)
                map.controller.setZoom(19.0) // Zoom cercano

                map.overlays.clear()
                val marker = Marker(map)
                marker.position = punto
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                marker.title = "Ubicación"
                map.overlays.add(marker)
            } else {
                // Vista por defecto si no hay GPS
                if (map.zoomLevelDouble < 10) {
                    map.controller.setZoom(6.0)
                    map.controller.setCenter(GeoPoint(-1.8312, -78.1834))
                }
            }
            map.invalidate()
        }
    )
}