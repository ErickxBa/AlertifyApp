package com.proyecto.alertify.app

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class InicioRutaActivity : AppCompatActivity() {
    private lateinit var tvChooseMap: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inicio_ruta)

        tvChooseMap = findViewById(R.id.text_eligeRuta)
        tvChooseMap.setOnClickListener {
            val intent = Intent(this, RutaIniciadaActivity::class.java)
            startActivity(intent)
        }
    }
}