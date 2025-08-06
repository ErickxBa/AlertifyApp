package com.proyecto.alertify.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
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
import com.google.android.gms.maps.model.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.maps.android.PolyUtil
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.io.InputStreamReader
import java.util.Locale

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>
    private lateinit var bottomSheetLayout: LinearLayout

    private lateinit var auth: FirebaseAuth
    private lateinit var locationCallback: LocationCallback
    private var isFirstLocationUpdate = true
    private var routePolylines = mutableListOf<Polyline>()
    private lateinit var lastKnownLatLng: LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        bottomSheetLayout = findViewById(R.id.bottom_sheet_include)

        setupBottomSheet()
        setupClickListeners()
        setupRouteButtons()
        updateNavHeader()
        createLocationCallback()
    }

    /** Configura los botones para iniciar y cancelar la ruta */
    private fun setupRouteButtons() {
        val buttonStart = findViewById<Button>(R.id.button_start_route)
        val buttonCancel = findViewById<Button>(R.id.button_cancel_route)
        val editOrigin = findViewById<EditText>(R.id.edit_origin)
        val editDestination = findViewById<EditText>(R.id.edit_destination)

        buttonStart.text = getString(R.string.app_button_start)
        buttonCancel.text = getString(R.string.app_button_cancel)

        buttonStart.setOnClickListener {
            val origin = editOrigin.text.toString().trim()
            val destination = editDestination.text.toString().trim()

            if (origin.isEmpty() || destination.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_invalid_input), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, getString(R.string.toast_searching_route, origin, destination), Toast.LENGTH_SHORT).show()
            fetchAndDrawRoutes(origin, destination)
        }

        buttonCancel.setOnClickListener {
            editOrigin.text.clear()
            editDestination.text.clear()
            clearRoutes()
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            Toast.makeText(this, getString(R.string.toast_route_cleared), Toast.LENGTH_SHORT).show()
        }
    }

    /** Inicia actualizaciones de ubicación cuando la app está visible */
    override fun onResume() {
        super.onResume()
        if (isLocationPermissionGranted()) {
            startLocationUpdates()
        }
    }

    /** Detiene actualizaciones de ubicación para ahorrar batería */
    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    /** Inicializa el mapa cuando está listo */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation()
        addMapBubbles()
    }

    /** Configura el BottomSheet con hints y comportamiento de expansión */
    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)
        val density = resources.displayMetrics.density
        bottomSheetBehavior.peekHeight = (150 * density).toInt()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetLayout.visibility = View.GONE

        val editOrigin = findViewById<EditText>(R.id.edit_origin)
        val editDestination = findViewById<EditText>(R.id.edit_destination)

        editOrigin.hint = getString(R.string.app_hint_origin)
        editDestination.hint = getString(R.string.app_hint_destination)

        val expandOnFocusListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus && bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        editOrigin.onFocusChangeListener = expandOnFocusListener
        editDestination.onFocusChangeListener = expandOnFocusListener
    }

    /** Configura listeners para abrir menú y responder a selecciones */
    private fun setupClickListeners() {
        val menuButton = findViewById<ImageButton>(R.id.menu_button)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationDrawer = findViewById<NavigationView>(R.id.navigation_drawer)

        menuButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START)
            else drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationDrawer.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.opcionInicio -> true
                R.id.opcionPerfil -> {
                    Toast.makeText(this, getString(R.string.toast_feature_in_progress), Toast.LENGTH_SHORT).show()
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
                    Toast.makeText(this, getString(R.string.toast_feature_in_progress), Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.opcionRutaSegura -> {
                    bottomSheetLayout.visibility = View.VISIBLE
                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                    true
                }
                R.id.opcionCerrarSesion -> {
                    auth.signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

    /** Actualiza la cabecera del menú con datos y foto del usuario */
    private fun updateNavHeader() {
        val navigationView = findViewById<NavigationView>(R.id.navigation_drawer)
        val headerView = navigationView.getHeaderView(0)
        val navUsername = headerView.findViewById<TextView>(R.id.text_username)
        val navUserEmail = headerView.findViewById<TextView>(R.id.text_email)
        val navUserPhoto = headerView.findViewById<ImageView>(R.id.image_profile)

        val currentUser = auth.currentUser

        if (currentUser != null) {
            navUsername.text = currentUser.displayName ?: getString(R.string.default_username)
            navUserEmail.text = currentUser.email

            var photoUrl: String? = currentUser.photoUrl?.toString()

            for (profile in currentUser.providerData) {
                if (profile.providerId == "facebook.com") {
                    val facebookUserId = profile.uid
                    photoUrl = "https://graph.facebook.com/$facebookUserId/picture?type=large"
                    break
                }
            }

            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.ico_user1)
                .circleCrop()
                .into(navUserPhoto)
        }
    }

    /** Habilita la capa de ubicación del usuario si hay permisos */
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

    /** Crea el callback para recibir actualizaciones de ubicación */
    private fun createLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    updateLocationUI(location)
                }
            }
        }
    }

    /** Inicia la solicitud de actualizaciones de ubicación */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10_000L
            fastestInterval = 5_000L
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    /** Detiene las actualizaciones de ubicación */
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /** Actualiza la UI y centra la cámara en la ubicación del usuario */
    private fun updateLocationUI(location: Location) {
        if (!::mMap.isInitialized) return

        lastKnownLatLng = LatLng(location.latitude, location.longitude)

        if (isFirstLocationUpdate) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastKnownLatLng, 15f))
            isFirstLocationUpdate = false
        }

        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addressLine = addresses[0].getAddressLine(0)
                // Se puede usar addressLine para mostrar si se desea
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Lee el archivo JSON de zonas y dibuja círculos en el mapa */
    private fun addMapBubbles() {
        try {
            val inputStream = assets.open("locations.json")
            val reader = InputStreamReader(inputStream)
            val mapPointType = object : TypeToken<List<MapPoint>>() {}.type
            val points: List<MapPoint> = Gson().fromJson(reader, mapPointType)

            points.forEach { point -> drawCircleOnMap(point) }

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, getString(R.string.toast_zone_load_error), Toast.LENGTH_SHORT).show()
        }
    }

    /** Dibuja un círculo en el mapa según el tipo de zona */
    private fun drawCircleOnMap(point: MapPoint) {
        val latLng = LatLng(point.latitude, point.longitude)
        val fillColor = if (point.type == "unsafe_zone") {
            ContextCompat.getColor(this, R.color.color_bubble_red)
        } else {
            ContextCompat.getColor(this, R.color.color_bubble_blue)
        }

        val circleOptions = CircleOptions()
            .center(latLng)
            .radius(point.radius)
            .fillColor(fillColor)
            .strokeWidth(0f)

        mMap.addCircle(circleOptions)
    }

    /** Verifica si se tiene permiso de ubicación */
    private fun isLocationPermissionGranted() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    /** Solicita permiso de ubicación al usuario */
    private fun requestLocationPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Toast.makeText(this, getString(R.string.toast_permission_rationale), Toast.LENGTH_LONG).show()
        }
        locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            enableMyLocation()
        } else {
            Toast.makeText(this, getString(R.string.toast_permission_denied), Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Realiza la consulta a Google Directions API para obtener rutas y las dibuja en el mapa.
     * Muestra hasta dos rutas, la principal en azul y la alternativa en rojo.
     */
    private fun fetchAndDrawRoutes(origin: String, destination: String) {
        if (origin.isEmpty() || destination.isEmpty()) {
            clearRoutes()
            return
        }

        val apiKey = "AIzaSyDCnE4BWWrfuuGaxVEKH1TmixIS0qq94Dk"

        val resolvedOrigin = if (origin.contains("actual", ignoreCase = true)) {
            if (::lastKnownLatLng.isInitialized) {
                "${lastKnownLatLng.latitude},${lastKnownLatLng.longitude}"
            } else {
                Toast.makeText(this, getString(R.string.toast_current_location_unavailable), Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            origin.replace(" ", "+")
        }

        val resolvedDestination = destination.replace(" ", "+")

        val url = "https://maps.googleapis.com/maps/api/directions/json" +
                "?origin=$resolvedOrigin" +
                "&destination=$resolvedDestination" +
                "&alternatives=true" +
                "&mode=driving" +
                "&key=$apiKey"

        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    clearRoutes()
                    Toast.makeText(
                        this@MainActivity,
                        getString(R.string.toast_route_fetch_error, e.message),
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("MainActivity", "Error en fetchAndDrawRoutes: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("MainActivity", "Respuesta API Directions: $responseBody")

                if (responseBody == null) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, getString(R.string.error_empty_routes_response), Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                val json = JSONObject(responseBody)
                val routes = json.optJSONArray("routes")

                if (routes == null || routes.length() == 0) {
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, getString(R.string.toast_no_routes_found), Toast.LENGTH_SHORT).show()
                        Log.w("MainActivity", "No routes found in response")
                    }
                    return
                }

                runOnUiThread {
                    clearRoutes()

                    val maxRoutesToDraw = 2
                    val routesToDraw = minOf(routes.length(), maxRoutesToDraw)
                    val boundsBuilder = LatLngBounds.builder()

                    for (i in 0 until routesToDraw) {
                        val route = routes.getJSONObject(i)
                        val overviewPolyline = route.getJSONObject("overview_polyline").getString("points")
                        val points = PolyUtil.decode(overviewPolyline)

                        val color = if (i == 0) {
                            ContextCompat.getColor(this@MainActivity, R.color.color_ruta_segura)
                        } else {
                            ContextCompat.getColor(this@MainActivity, R.color.color_ruta_insegura)
                        }

                        val polylineOptions = PolylineOptions()
                            .addAll(points)
                            .width(12f)
                            .color(color)
                            .geodesic(true)

                        val polyline = mMap.addPolyline(polylineOptions)
                        routePolylines.add(polyline)

                        points.forEach { boundsBuilder.include(it) }
                    }

                    val bounds = boundsBuilder.build()
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))

                    bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    Toast.makeText(this@MainActivity, getString(R.string.toast_route_displayed), Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    /** Elimina todas las rutas dibujadas del mapa */
    private fun clearRoutes() {
        routePolylines.forEach { it.remove() }
        routePolylines.clear()
    }
}
