package com.proyecto.alertify.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu

class ReseniasActivity : AppCompatActivity() {
    private lateinit var btnVerResenas: Button
    private lateinit var btnAgregarResena: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resenias)

        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl("file:///android_asset/map.html")

        btnVerResenas = findViewById(R.id.btnVerResenas)
        btnAgregarResena = findViewById(R.id.btnAgregarResena)

        val btnMenu = findViewById<ImageButton>(R.id.btnMenu)
        btnMenu.setOnClickListener { view ->
            showPopupMenu(view)
        }

        btnVerResenas.setOnClickListener {
            val intent = Intent(this, DetalleReseniasActivity::class.java)
            startActivity(intent)
        }
        btnAgregarResena.setOnClickListener{
            val intent = Intent(this, AgregarReseniaActivity::class.java)
            startActivity(intent)
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
