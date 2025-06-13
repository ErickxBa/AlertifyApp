package com.proyecto.alertify.app

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var menuButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        menuButton = findViewById(R.id.menu_button)
        menuButton.setOnClickListener {
            val popupMenu = PopupMenu(this, menuButton)
            popupMenu.menuInflater.inflate(R.menu.drawer_menu, popupMenu.menu)

            // Forzar íconos visibles en versiones modernas
            try {
                val fields = popupMenu.javaClass.declaredFields
                for (field in fields) {
                    if ("mPopup" == field.name) {
                        field.isAccessible = true
                        val menuPopupHelper = field.get(popupMenu)
                        val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                        val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                        setForceIcons.invoke(menuPopupHelper, true)
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        Toast.makeText(this, "Inicio", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.nav_notifications -> {
                        Toast.makeText(this, "Notificaciones", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, NotificacionesActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.nav_update_info -> {
                        Toast.makeText(this, "Actualizar información", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, ActualizacionPerfilActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.nav_start_route -> {
                        Toast.makeText(this, "Comenzar ruta", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, InicioRutaActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.nav_rewards -> {
                        Toast.makeText(this, "Recompensas", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, RecompensasActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.nav_resenia -> {
                        Toast.makeText(this, "Reseñas", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, ReseniasActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.nav_logout -> {
                        Toast.makeText(this, "Cerrar sesión", Toast.LENGTH_SHORT).show()
                        finish()
                        true
                    }
                    else -> false
                }
            }

            popupMenu.show()
        }
    }
}
