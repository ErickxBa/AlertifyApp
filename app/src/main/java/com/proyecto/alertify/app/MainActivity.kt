package com.proyecto.alertify.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    // --- NUEVAS VARIABLES PARA ACTUALIZACIONES CONTINUAS ---
    private lateinit var locationCallback: LocationCallback
    private var isFirstLocationUpdate = true // Para centrar el mapa solo la primera vez

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupBottomSheet()
        setupClickListeners()

        // --- INICIA EL PROCESO DE ACTUALIZACIÓN DE UBICACIÓN ---
        createLocationCallback()
    }

    override fun onResume() {
        super.onResume()
        // Inicia las actualizaciones cuando la app vuelve a estar visible
        if (isLocationPermissionGranted()) {
            startLocationUpdates()
        }
    }

    override fun onPause() {
        super.onPause()
        // Detiene las actualizaciones para ahorrar batería cuando la app no está visible
        stopLocationUpdates()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation()
    }

    private fun setupBottomSheet() {
        val bottomSheetLayout = findViewById<LinearLayout>(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
    }

    private fun setupClickListeners() {
        val menuButton = findViewById<ImageButton>(R.id.menu_button)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationDrawer = findViewById<NavigationView>(R.id.navigation_drawer)

        //boton para abrir el menu lateral
        menuButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        //Opciones del menu lateral
        navigationDrawer.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.opcionInicio -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.opcionPerfil -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    Toast.makeText(this, "Ventana flotante en desarrollo", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.opcionNotificaciones -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, NotificacionesActivity::class.java))
                    true
                }
                R.id.opcionBeneficios -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, RecompensasActivity::class.java))
                    true
                }
                R.id.opcionZonasPeligrosas,
                R.id.opcionRutaSegura -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    Toast.makeText(this, "Ventana flotante en desarrollo", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.opcionCerrarSesion -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    val prefs = getSharedPreferences("users", MODE_PRIVATE)
                    prefs.edit().remove("current_user").apply()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        val searchBar = findViewById<CardView>(R.id.search_bar)
        searchBar.setOnClickListener {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (!::mMap.isInitialized) return
        if (isLocationPermissionGranted()) {
            mMap.isMyLocationEnabled = true
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }
    }

    /**
     * Define qué hacer cada vez que se recibe una nueva ubicación.
     */
    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    // Cuando llega una nueva ubicación, actualiza la UI
                    updateLocationUI(location)
                }
            }
        }
    }

    /**
     * Inicia la solicitud de actualizaciones de ubicación continuas.
     */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000 // 10 segundos
            fastestInterval = 5000 // 5 segundos
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    /**
     * Detiene las actualizaciones de ubicación.
     */
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * Actualiza tanto el mapa (solo la primera vez) como el TextView del BottomSheet.
     */
    private fun updateLocationUI(location: Location) {
        if (!::mMap.isInitialized) return

        // Centra el mapa solo en la primera actualización
        if (isFirstLocationUpdate) {
            val userLocation = LatLng(location.latitude, location.longitude)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
            isFirstLocationUpdate = false
        }

        // Actualiza el TextView con la dirección (esto se hará en cada actualización)
        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            val currentLocationTextView = findViewById<TextView>(R.id.current_location_text)

            if (addresses != null && addresses.isNotEmpty()) {
                currentLocationTextView.text = addresses[0].getAddressLine(0)
            } else {
                currentLocationTextView.text = "Ubicación actual"
            }
        } catch (e: Exception) {
            findViewById<TextView>(R.id.current_location_text).text = "Ubicación actual"
            e.printStackTrace()
        }
    }

    // --- Funciones de gestión de permisos (sin cambios) ---
    private fun isLocationPermissionGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, "La app necesita permisos de ubicación para mostrar tu posición.", Toast.LENGTH_LONG).show()
        }
        locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            enableMyLocation()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado.", Toast.LENGTH_LONG).show()
        }
    }
}