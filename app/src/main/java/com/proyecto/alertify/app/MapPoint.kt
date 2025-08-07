package com.proyecto.alertify.app

/**
 * Clase de datos para representar puntos en el mapa (zonas seguras/inseguras)
 */
data class MapPoint(
    val latitude: Double,
    val longitude: Double,
    val radius: Double,
    val type: String
)