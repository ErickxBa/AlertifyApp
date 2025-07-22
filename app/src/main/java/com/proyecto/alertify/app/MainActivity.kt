package com.proyecto.alertify.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient // Cliente para obtener la ubicación

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa el cliente de ubicación
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation() // Llama a la función para gestionar la ubicación
    }

    /**
     * Comprueba si los permisos de ubicación están concedidos.
     * Si lo están, activa la capa de ubicación en el mapa.
     * Si no, solicita los permisos.
     */
    @RequiresPermission(allOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    private fun enableMyLocation() {
        if (!::mMap.isInitialized) return // Si el mapa no está listo, no hagas nada

        if (isLocationPermissionGranted()) {
            // Si tenemos permiso, activamos la capa de "Mi Ubicación"
            mMap.isMyLocationEnabled = true
            centerMapOnUserLocation()
        } else {
            // Si no tenemos permiso, lo solicitamos
            requestLocationPermission()
        }
    }

    /**
     * Mueve la cámara del mapa a la última ubicación conocida del usuario.
     */
    private fun centerMapOnUserLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLocation = LatLng(location.latitude, location.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                } else {
                    // Si no se pudo obtener la ubicación, muestra un mensaje
                    Toast.makeText(this, "No se pudo obtener la ubicación actual.", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            // Esto es necesario por si acaso, aunque ya hemos verificado los permisos
            e.printStackTrace()
        }
    }

    /**
     * Verifica si el permiso de ubicación precisa ha sido concedido.
     */
    private fun isLocationPermissionGranted() =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    /**
     * Lanza el diálogo del sistema para solicitar permisos de ubicación.
     */
    private fun requestLocationPermission() {
        // Si el usuario ya ha rechazado el permiso antes, podemos mostrarle una explicación
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "La app necesita permisos de ubicación para mostrar tu posición en el mapa.", Toast.LENGTH_LONG).show()
        }
        // Lanza el diálogo para pedir el permiso
        locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /**
     * Gestiona la respuesta del usuario al diálogo de solicitud de permisos.
     */
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    )
    @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION]) { isGranted: Boolean ->
        if (isGranted) {
            // Si el usuario concede el permiso, activamos la capa de ubicación
            enableMyLocation()
        } else {
            // Si el usuario rechaza el permiso, le informamos
            Toast.makeText(this, "Permiso de ubicación denegado. No se puede mostrar la ubicación actual.", Toast.LENGTH_LONG).show()
        }
    }
}