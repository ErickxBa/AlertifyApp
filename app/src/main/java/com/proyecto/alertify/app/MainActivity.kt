package com.proyecto.alertify.app // IMPORTANTE: Asegúrate de que este paquete coincida con el de tu proyecto

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Aquí iría la lógica para tu ImageButton (por ejemplo, setOnClickListener)
        val menuButton = findViewById<android.widget.ImageButton>(R.id.menu_button)
        menuButton.setOnClickListener {
            // Implementa la acción del botón de menú aquí
        }

        // El resto de la lógica futura de tu aplicación principal puede ir aquí
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Coordenadas de Quito, Ecuador (ya que esa es tu ubicación actual)
        val quito = LatLng(-0.180653, -78.467834)
        mMap.addMarker(MarkerOptions().position(quito).title("Ubicación Inicial"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(quito, 12f))

        // Opcional: Activar la capa de "Mi Ubicación" si tienes los permisos
        // if (ActivityCompat.checkSelfPermission(...) == PackageManager.PERMISSION_GRANTED) {
        //     mMap.isMyLocationEnabled = true
        // }
    }
}