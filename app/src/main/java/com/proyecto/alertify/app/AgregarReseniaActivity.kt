package com.proyecto.alertify.app

import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AgregarReseniaActivity : AppCompatActivity() {
    private lateinit var stars: List<ImageView>
    private var rating = 0
    private lateinit var btnRegresar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agregar_resenia)

        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/map.html")

        btnRegresar = findViewById(R.id.btnRegresar)

        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)
        btnMenu.setOnClickListener { view ->
            showPopupMenu(view)
        }
        btnRegresar.setOnClickListener {
            finish()
        }

        stars = listOf(
            findViewById(R.id.star1),
            findViewById(R.id.star2),
            findViewById(R.id.star3),
            findViewById(R.id.star4),
            findViewById(R.id.star5)
        )

        stars.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                setRating(index + 1)
                Toast.makeText(this, "Puntaje: ${index + 1} estrellas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setRating(value: Int) {
        rating = value
        for (i in stars.indices) {
            if (i < rating) {
                stars[i].setImageResource(R.drawable.ico_estrellamarcada)
            } else {
                stars[i].setImageResource(R.drawable.ico_estrella)
            }
        }
    }
    private fun showPopupMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.menu_popup, popup.menu)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.item1 -> {
                    Toast.makeText(this, "Opción 1 seleccionada", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.item2 -> {
                    Toast.makeText(this, "Opción 2 seleccionada", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.item3 -> {
                    Toast.makeText(this, "Opción 3 seleccionada", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }
}