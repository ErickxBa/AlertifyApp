package com.proyecto.alertify.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

import com.google.android.gms.maps.model.CircleOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var bottomSheetLayout: LinearLayout

    private lateinit var auth: FirebaseAuth
    private lateinit var locationCallback: LocationCallback
    private var isFirstLocationUpdate = true // Para centrar el mapa solo la primera vez

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa Firebase Auth
        auth = Firebase.auth

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        bottomSheetLayout = findViewById(R.id.bottom_sheet_include)
        setupBottomSheet()   // Inicializa y oculta el BottomSheet
        setupClickListeners()

        // Muestra la información del usuario en el menú lateral
        updateNavHeader()

        // Inicia el proceso de actualización de ubicación
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
        addMapBubbles()
    }
    /**
     * Configura el BottomSheet para que se pueda expandir y contraer.
     * Además, lo oculta inicialmente.
     */
    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetLayout.visibility = View.GONE
    }

    /**
     * Configura todos los listeners de clics para los botones y el menú.
     */
    private fun setupClickListeners() {
        val menuButton = findViewById<ImageButton>(R.id.menu_button)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationDrawer = findViewById<NavigationView>(R.id.navigation_drawer)

        // Botón para abrir el menú lateral
        menuButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START)
            } else {
                drawerLayout.openDrawer(GravityCompat.START)
            }
        }

        // Opciones del menú lateral
        navigationDrawer.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START) // Cierra el menú al seleccionar una opción
            when (menuItem.itemId) {
                R.id.opcionInicio -> true
                R.id.opcionPerfil -> {
                    Toast.makeText(this, "Ventana flotante en desarrollo", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.opcionNotificaciones -> {
                    startActivity(Intent(this, NotificacionesActivity::class.java))
                    true
                }
                R.id.opcionBeneficios -> {
                    startActivity(Intent(this, RecompensasActivity::class.java))
                    true
                }
                R.id.opcionZonasPeligrosas -> {
                    Toast.makeText(this, "Ventana flotante en desarrollo", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.opcionRutaSegura -> {
                    // Aquí hacemos visible y expandimos el BottomSheet
                    bottomSheetLayout.visibility = View.VISIBLE
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    true
                }
                R.id.opcionCerrarSesion -> {
                    // --- LÓGICA DE CERRAR SESIÓN CORREGIDA ---
                    auth.signOut() // Cierra la sesión de Firebase
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Actualiza la cabecera del menú de navegación con los datos del usuario logueado.
     */
    private fun updateNavHeader() {
        val navigationView = findViewById<NavigationView>(R.id.navigation_drawer)
        val headerView = navigationView.getHeaderView(0)
        val navUsername = headerView.findViewById<TextView>(R.id.text_username)
        val navUserEmail = headerView.findViewById<TextView>(R.id.text_email)
        val navUserPhoto = headerView.findViewById<ImageView>(R.id.image_profile)

        val currentUser = auth.currentUser

        if (currentUser != null) {
            navUsername.text = currentUser.displayName ?: "Usuario"
            navUserEmail.text = currentUser.email

            // --- INICIA LÓGICA CORREGIDA PARA LA FOTO DE PERFIL ---

            // Variable para almacenar la URL de la foto
            var photoUrl: String? = currentUser.photoUrl?.toString()

            // Revisa los proveedores de la cuenta para encontrar el de Facebook
            for (profile in currentUser.providerData) {
                if (profile.providerId == "facebook.com") {
                    // Si el proveedor es Facebook, construye la URL manualmente
                    val facebookUserId = profile.uid
                    photoUrl = "https://graph.facebook.com/$facebookUserId/picture?type=large"
                    break // Sal del bucle una vez que encuentres el proveedor de Facebook
                }
            }

            // Carga la foto de perfil con Glide usando la URL correcta
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ico_user1) // Usa tu ícono por defecto
                .circleCrop()
                .into(navUserPhoto)
            // --- FIN DE LÓGICA CORREGIDA ---
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

    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationUI(location)
                }
            }
        }
    }

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
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateLocationUI(location: Location) {
        if (!::mMap.isInitialized) return

        if (isFirstLocationUpdate) {
            val userLocation = LatLng(location.latitude, location.longitude)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
            isFirstLocationUpdate = false
        }

        val geocoder = Geocoder(this, Locale.getDefault())
        try {
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val addressLine = addresses[0].getAddressLine(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Lee el archivo locations.json desde assets, lo parsea y dibuja los círculos en el mapa.
     */
    private fun addMapBubbles() {
        try {
            val inputStream = assets.open("locations.json")
            val reader = InputStreamReader(inputStream)
            val mapPointType = object : TypeToken<List<MapPoint>>() {}.type
            val points: List<MapPoint> = Gson().fromJson(reader, mapPointType)


            points.forEach { point ->
                drawCircleOnMap(point)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al cargar las zonas", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Dibuja un único círculo en el mapa basado en un objeto MapPoint.
     */
    private fun drawCircleOnMap(point: MapPoint) {
        val latLng = LatLng(point.latitude, point.longitude)
        val fillColor = if (point.type == "unsafe_zone") {
            ContextCompat.getColor(this, R.color.color_bubble_red)
        } else {
            ContextCompat.getColor(this, R.color.color_bubble_blue)
        }

        val circleOptions = CircleOptions()
            .center(latLng)
            .radius(point.radius) // El radio se define en metros
            .fillColor(fillColor)
            .strokeWidth(0f) // Sin borde para un look más limpio


        mMap.addCircle(circleOptions)
    }

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