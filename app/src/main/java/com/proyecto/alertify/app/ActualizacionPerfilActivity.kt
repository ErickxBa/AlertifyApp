package com.proyecto.alertify.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import androidx.activity.enableEdgeToEdge

class ActualizacionPerfilActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_actualizacion_perfil)

        val editButton = findViewById<TextView>(R.id.editButton)
        editButton.setOnClickListener {
            val intent = Intent(this, ModificarPerfilActivity::class.java)
            startActivity(intent)
        }
    }
}
