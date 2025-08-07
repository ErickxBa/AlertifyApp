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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import android.text.Editable
import android.text.TextWatcher
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.util.Date

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
    private val apiKey = "AIzaSyDCnE4BWWrfuuGaxVEKH1TmixIS0qq94Dk"
    private val TAG = "GeocodingActivity"
    private var searchJob: Job? = null
    private lateinit var originRecyclerView: RecyclerView
    private lateinit var destinationRecyclerView: RecyclerView

    private lateinit var addressHistoryAdapter: AddressHistoryAdapter
    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var noHistoryText: TextView
    private val addressHistory = mutableListOf<AddressHistoryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = Firebase.auth
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        bottomSheetLayout = findViewById(R.id.bottom_sheet_include)
        setupBottomSheetWithAutocomplete()
        setupClickListeners()
        setupRouteButtons()
        setupHistorySection()
        updateNavHeader()
        createLocationCallback()
    }

    // Nuevo método para configurar la sección de historial
    private fun setupHistorySection() {
        historyRecyclerView = findViewById(R.id.recycler_address_history)
        noHistoryText = findViewById(R.id.text_no_history)
        val clearHistoryText = findViewById<TextView>(R.id.text_clear_history)

        // Configurar adapter del historial
        addressHistoryAdapter = AddressHistoryAdapter(addressHistory) { addressItem ->
            val editOrigin = findViewById<EditText>(R.id.edit_origin)
            val editDestination = findViewById<EditText>(R.id.edit_destination)

            val focusedView = currentFocus

            when (focusedView) {
                editOrigin -> {
                    editOrigin.setText(addressItem.address)
                    editOrigin.setSelection(editOrigin.text.length)
                    Toast.makeText(this, "Origen cargado desde historial", Toast.LENGTH_SHORT).show()
                }
                editDestination -> {
                    editDestination.setText(addressItem.address)
                    editDestination.setSelection(editDestination.text.length)
                    Toast.makeText(this, "Destino cargado desde historial", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Si ninguno está enfocado, puedes usar el tipo como fallback
                    when (addressItem.type) {
                        "origin" -> {
                            editOrigin.setText(addressItem.address)
                            editOrigin.setSelection(editOrigin.text.length)
                            Toast.makeText(this, "Origen cargado desde historial", Toast.LENGTH_SHORT).show()
                        }
                        "destination" -> {
                            editDestination.setText(addressItem.address)
                            editDestination.setSelection(editDestination.text.length)
                            Toast.makeText(this, "Destino cargado desde historial", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

        historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = addressHistoryAdapter
        }

        // Configurar botón de limpiar historial
        clearHistoryText.setOnClickListener {
            if (addressHistory.isNotEmpty()) {
                addressHistoryAdapter.clearHistory()
                updateHistoryVisibility()
                Toast.makeText(this, "Historial limpiado", Toast.LENGTH_SHORT).show()
            }
        }

        updateHistoryVisibility()
    }

    private fun updateHistoryVisibility() {
        if (addressHistory.isEmpty()) {
            historyRecyclerView.visibility = View.GONE
            noHistoryText.visibility = View.VISIBLE
        } else {
            historyRecyclerView.visibility = View.VISIBLE
            noHistoryText.visibility = View.GONE
        }
    }

    private fun addAddressToHistory(address: String, type: String) {
        if (address.trim().isEmpty() || address.contains("actual", ignoreCase = true)) {
            return // No agregar direcciones vacías o "ubicación actual"
        }

        val timestamp = getRelativeTime()
        val historyItem = AddressHistoryItem(
            address = address.trim(),
            timestamp = timestamp,
            type = type
        )

        addressHistoryAdapter.addAddress(historyItem)
        updateHistoryVisibility()
    }

    private fun getRelativeTime(): String {
        val now = System.currentTimeMillis()
        val format = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
        return format.format(Date(now))
    }

    // Modificar setupBottomSheet para incluir el autocompletado
    private fun setupBottomSheetWithAutocomplete() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheetLayout)
        val density = resources.displayMetrics.density
        bottomSheetBehavior.peekHeight = (150 * density).toInt()
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetLayout.visibility = View.GONE

        val editOrigin = findViewById<EditText>(R.id.edit_origin)
        val editDestination = findViewById<EditText>(R.id.edit_destination)
        editOrigin.hint = "Escribe tu ubicación de origen..."
        editDestination.hint = "Escribe tu destino..."

        val expandOnFocusListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus && bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        editOrigin.onFocusChangeListener = expandOnFocusListener
        editDestination.onFocusChangeListener = expandOnFocusListener
    }

    // Modificar setupRouteButtons para agregar direcciones al historial
    private fun setupRouteButtons() {
        val buttonStart = findViewById<Button>(R.id.button_start_route)
        val buttonCancel = findViewById<Button>(R.id.button_cancel_route)
        val editOrigin = findViewById<EditText>(R.id.edit_origin)
        val editDestination = findViewById<EditText>(R.id.edit_destination)

        buttonStart.text = getString(R.string.app_button_start)
        buttonCancel.text = getString(R.string.app_button_cancel)

        buttonStart.setOnClickListener {
            val originText = editOrigin.text.toString().trim()
            val destinationText = editDestination.text.toString().trim()

            if (originText.isEmpty() || destinationText.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_invalid_input), Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Agregar direcciones al historial antes de buscar la ruta
            addAddressToHistory(originText, "origin")
            addAddressToHistory(destinationText, "destination")

            Toast.makeText(
                this,
                getString(R.string.toast_searching_route, originText, destinationText),
                Toast.LENGTH_SHORT
            ).show()

            // Resto del código existente para buscar rutas...
            geocodeAddress(originText) { originLatLng ->
                if (originLatLng == null) {
                    Log.e(TAG, "Error: No se pudo geocodificar el origen: $originText")
                    fetchDirectionsApiFallback(originText, destinationText)
                    return@geocodeAddress
                }

                Log.d(
                    TAG,
                    "Origen geocodificado exitosamente: $originText -> Lat: ${originLatLng.latitude}, Lng: ${originLatLng.longitude}"
                )

                geocodeAddress(destinationText) { destinationLatLng ->
                    if (destinationLatLng == null) {
                        Log.e(TAG, "Error: No se pudo geocodificar el destino: $destinationText")
                        fetchDirectionsApiFallback(originText, destinationText)
                        return@geocodeAddress
                    }

                    Log.d(
                        TAG,
                        "Destino geocodificado exitosamente: $destinationText -> Lat: ${destinationLatLng.latitude}, Lng: ${destinationLatLng.longitude}"
                    )

                    fetchRoutesApiRoutesImproved(
                        originLatLng,
                        destinationLatLng,
                        originText,
                        destinationText
                    )
                }
            }
        }

        buttonCancel.setOnClickListener {
            editOrigin.text.clear()
            editDestination.text.clear()
            clearRoutes()
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            Toast.makeText(this, getString(R.string.toast_route_cleared), Toast.LENGTH_SHORT).show()
        }
    }
    // NUEVA VERSIÓN MEJORADA DE Routes API con más opciones
    private fun fetchRoutesApiRoutesImproved(
        origin: LatLng,
        destination: LatLng,
        originText: String,
        destinationText: String
    ) {
        Log.d(TAG, "Iniciando fetchRoutesApiRoutesImproved")
        val url = "https://routes.googleapis.com/directions/v2:computeRoutes"

        val jsonPayload = """
        {
          "origin": {
            "location": {
              "latLng": {
                "latitude": ${origin.latitude},
                "longitude": ${origin.longitude}
              }
            }
          },
          "destination": {
            "location": {
              "latLng": {
                "latitude": ${destination.latitude},
                "longitude": ${destination.longitude}
              }
            }
          },
          "travelMode": "DRIVE",
          "computeAlternativeRoutes": true,
          "routingPreference": "TRAFFIC_AWARE_OPTIMAL",
          "routeModifiers": {
            "avoidTolls": false,
            "avoidHighways": false,
            "avoidFerries": false
          },
          "units": "METRIC"
        }
        """.trimIndent()

        val mediaType = "application/json".toMediaTypeOrNull()
        val body = jsonPayload.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-Goog-Api-Key", apiKey)
            .addHeader(
                "X-Goog-FieldMask",
                "routes.duration,routes.distanceMeters,routes.polyline.encodedPolyline,routes.legs"
            )
            .post(body)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Routes API falló, intentando con Directions API: ${e.message}")
                runOnUiThread {
                    // Fallback automático a Directions API
                    fetchDirectionsApiFallback(originText, destinationText)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val bodyString = response.body?.string()

                if (!response.isSuccessful || bodyString == null) {
                    Log.e(TAG, "Routes API no exitoso, usando Directions API como fallback")
                    runOnUiThread {
                        fetchDirectionsApiFallback(originText, destinationText)
                    }
                    return
                }

                try {
                    val json = JSONObject(bodyString)
                    val routes = json.optJSONArray("routes")

                    if (routes == null || routes.length() == 0) {
                        Log.w(TAG, "No routes en Routes API, usando Directions API")
                        runOnUiThread {
                            fetchDirectionsApiFallback(originText, destinationText)
                        }
                        return
                    }

                    runOnUiThread {
                        processAndDrawRoutes(routes, "Routes API v2")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parseando Routes API: ${e.message}")
                    runOnUiThread {
                        fetchDirectionsApiFallback(originText, destinationText)
                    }
                }
            }
        })
    }

    // VERSIÓN MEJORADA de Directions API como fallback
    private fun fetchDirectionsApiFallback(origin: String, destination: String) {
        Log.d(TAG, "Usando Directions API como fallback")

        val resolvedOrigin = if (origin.contains("actual", ignoreCase = true)) {
            if (::lastKnownLatLng.isInitialized) {
                "${lastKnownLatLng.latitude},${lastKnownLatLng.longitude}"
            } else {
                origin.replace(" ", "+")
            }
        } else {
            origin.replace(" ", "+")
        }

        val resolvedDestination = destination.replace(" ", "+")

        // Múltiples intentos con diferentes parámetros para obtener más rutas
        val requests = listOf(
            // Intento 1: Rutas optimizadas con tráfico
            "https://maps.googleapis.com/maps/api/directions/json" +
                    "?origin=$resolvedOrigin" +
                    "&destination=$resolvedDestination" +
                    "&alternatives=true" +
                    "&mode=driving" +
                    "&departure_time=now" +
                    "&traffic_model=best_guess" +
                    "&key=$apiKey",

            // Intento 2: Rutas evitando autopistas (para más opciones locales)
            "https://maps.googleapis.com/maps/api/directions/json" +
                    "?origin=$resolvedOrigin" +
                    "&destination=$resolvedDestination" +
                    "&alternatives=true" +
                    "&mode=driving" +
                    "&avoid=highways" +
                    "&key=$apiKey",

            // Intento 3: Rutas evitando peajes
            "https://maps.googleapis.com/maps/api/directions/json" +
                    "?origin=$resolvedOrigin" +
                    "&destination=$resolvedDestination" +
                    "&alternatives=true" +
                    "&mode=driving" +
                    "&avoid=tolls" +
                    "&key=$apiKey"
        )

        executeDirectionsRequests(requests, 0, mutableSetOf())
    }

    private fun executeDirectionsRequests(
        requests: List<String>,
        index: Int,
        allRoutes: MutableSet<String>
    ) {
        if (index >= requests.size) {
            // Procesar todas las rutas recolectadas
            if (allRoutes.isNotEmpty()) {
                runOnUiThread {
                    drawCombinedRoutes(allRoutes.toList())
                }
            } else {
                runOnUiThread {
                    Toast.makeText(this, "No se encontraron rutas disponibles", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            return
        }

        val request = Request.Builder().url(requests[index]).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Request ${index + 1} falló: ${e.message}")
                // Continuar con el siguiente request
                executeDirectionsRequests(requests, index + 1, allRoutes)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                if (responseBody != null && response.isSuccessful) {
                    try {
                        val json = JSONObject(responseBody)
                        val routes = json.optJSONArray("routes")

                        if (routes != null && routes.length() > 0) {
                            for (i in 0 until routes.length()) {
                                val route = routes.getJSONObject(i)
                                val overviewPolyline =
                                    route.getJSONObject("overview_polyline").getString("points")
                                allRoutes.add(overviewPolyline)
                            }
                            Log.d(
                                TAG,
                                "Request ${index + 1} agregó ${routes.length()} rutas. Total: ${allRoutes.size}"
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parseando request ${index + 1}: ${e.message}")
                    }
                }

                // Continuar con el siguiente request
                executeDirectionsRequests(requests, index + 1, allRoutes)
            }
        })
    }

    // Actualizar también drawCombinedRoutes para que use solo 3 rutas con colores específicos
    private fun drawCombinedRoutes(polylines: List<String>) {
        Log.d(TAG, "Dibujando rutas combinadas con colores específicos")
        clearRoutes()

        val boundsBuilder = LatLngBounds.builder()

        // Definir exactamente 3 colores específicos
        val routeColors = listOf(
            Triple(ContextCompat.getColor(this, R.color.color_ruta_segura), 12f, "Ruta 1 (Azul)"),
            Triple(ContextCompat.getColor(this, android.R.color.holo_blue_bright), 14f, "Ruta 2 (Azul Seguro)"),
            Triple(ContextCompat.getColor(this, R.color.color_ruta_insegura), 10f, "Ruta 3 (Roja)")
        )

        // Tomar exactamente 3 rutas únicas
        val uniquePolylines = polylines.distinct().take(3)

        uniquePolylines.forEachIndexed { index, polylineEncoded ->
            try {
                val points = PolyUtil.decode(polylineEncoded)
                val (color, width, name) = routeColors[index]

                val polylineOptions = PolylineOptions()
                    .addAll(points)
                    .width(width)
                    .color(color)
                    .geodesic(true)

                val polyline = mMap.addPolyline(polylineOptions)
                routePolylines.add(polyline)

                points.forEach { boundsBuilder.include(it) }
                Log.d(TAG, "$name dibujada exitosamente")

            } catch (e: Exception) {
                Log.e(TAG, "Error dibujando ruta $index: ${e.message}")
            }
        }

        if (routePolylines.isNotEmpty()) {
            val bounds = boundsBuilder.build()
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

            // Agregar al historial
            val editOrigin = findViewById<EditText>(R.id.edit_origin)
            val editDestination = findViewById<EditText>(R.id.edit_destination)
            val originText = editOrigin.text.toString().trim()
            val destinationText = editDestination.text.toString().trim()

            if (originText.isNotEmpty() && destinationText.isNotEmpty()) {
                addAddressToHistory(originText, "origin")
                addAddressToHistory(destinationText, "destination")
            }

            Toast.makeText(
                this,
                "✓ 3 rutas encontradas: Azul rápida, Azul segura, Roja directa",
                Toast.LENGTH_LONG
            ).show()
            Log.i(TAG, "3 rutas específicas dibujadas exitosamente: ${uniquePolylines.size} rutas")
        } else {
            Toast.makeText(this, "No se pudieron dibujar las rutas", Toast.LENGTH_SHORT).show()
        }
    }

    private fun processAndDrawRoutes(routes: org.json.JSONArray, source: String) {
        Log.d(TAG, "Procesando rutas desde $source: ${routes.length()} rutas")
        clearRoutes()

        val boundsBuilder = LatLngBounds.builder()
        val colors = listOf(
            ContextCompat.getColor(this, R.color.color_ruta_segura),
            ContextCompat.getColor(this, R.color.color_ruta_insegura),
            ContextCompat.getColor(this, android.R.color.holo_green_dark),
            ContextCompat.getColor(this, android.R.color.holo_orange_dark),
            ContextCompat.getColor(this, android.R.color.holo_purple)
        )

        for (i in 0 until minOf(routes.length(), 5)) { // Máximo 5 rutas
            try {
                val route = routes.getJSONObject(i)
                val polylineEncoded = if (source.contains("Routes API")) {
                    route.getJSONObject("polyline").getString("encodedPolyline")
                } else {
                    route.getJSONObject("overview_polyline").getString("points")
                }

                val points = PolyUtil.decode(polylineEncoded)
                val color = colors[i % colors.size]
                val width = if (i == 0) 14f else 10f

                val polylineOptions = PolylineOptions()
                    .addAll(points)
                    .width(width)
                    .color(color)
                    .geodesic(true)

                val polyline = mMap.addPolyline(polylineOptions)
                routePolylines.add(polyline)
                points.forEach { boundsBuilder.include(it) }

            } catch (e: Exception) {
                Log.e(TAG, "Error procesando ruta $i: ${e.message}")
            }
        }

        val bounds = boundsBuilder.build()
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        Toast.makeText(
            this,
            "Rutas mostradas desde $source: ${routePolylines.size}",
            Toast.LENGTH_SHORT
        ).show()
    }

    // [El resto de métodos permanecen igual...]

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

    override fun onDestroy() {
        super.onDestroy()
        searchJob?.cancel()
    }

    /** Configura listeners para abrir menú y responder a selecciones */
    private fun setupClickListeners() {
        val menuButton = findViewById<ImageButton>(R.id.menu_button)
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navigationDrawer = findViewById<NavigationView>(R.id.navigation_drawer)

        menuButton.setOnClickListener {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(
                GravityCompat.START
            )
            else drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationDrawer.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.opcionInicio -> true
                R.id.opcionPerfil -> {
                    Toast.makeText(
                        this,
                        getString(R.string.toast_feature_in_progress),
                        Toast.LENGTH_SHORT
                    ).show()
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
                    Toast.makeText(
                        this,
                        getString(R.string.toast_feature_in_progress),
                        Toast.LENGTH_SHORT
                    ).show()
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
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
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

    private fun geocodeAddress(address: String, callback: (LatLng?) -> Unit) {
        val url = "https://maps.googleapis.com/maps/api/geocode/json?address=${
            address.replace(
                " ",
                "+"
            )
        }&key=$apiKey"
        val request = Request.Builder().url(url).build()
        val client = OkHttpClient()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { callback(null) }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body == null) {
                    runOnUiThread { callback(null) }
                    return
                }
                val json = JSONObject(body)
                val results = json.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val location = results.getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONObject("location")
                    val lat = location.getDouble("lat")
                    val lng = location.getDouble("lng")
                    runOnUiThread { callback(LatLng(lat, lng)) }
                } else {
                    runOnUiThread { callback(null) }
                }
            }
        })
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
            Toast.makeText(this, getString(R.string.toast_zone_load_error), Toast.LENGTH_SHORT)
                .show()
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
            Toast.makeText(this, getString(R.string.toast_permission_rationale), Toast.LENGTH_LONG)
                .show()
        }
        locationPermissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            enableMyLocation()
        } else {
            Toast.makeText(this, getString(R.string.toast_permission_denied), Toast.LENGTH_LONG)
                .show()
        }
    }

    /** Elimina todas las rutas dibujadas del mapa */
    private fun clearRoutes() {
        routePolylines.forEach { it.remove() }
        routePolylines.clear()
    }
}