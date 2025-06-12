package com.proyecto.alertify.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsernameEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvLoginRegisterToggle: TextView
    private lateinit var tvForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize views
        etUsernameEmail = findViewById(R.id.et_username_email)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        tvLoginRegisterToggle = findViewById(R.id.tv_login_register_toggle)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)

        // Set up login button click listener
        btnLogin.setOnClickListener {
            performLogin()
        }

        // Set up toggle to "Registrar" (for future implementation)
        tvLoginRegisterToggle.setOnClickListener {
            // For now, we'll just show a toast or keep it as "Ingresar"
            // In a real app, this would switch to a registration UI
            Toast.makeText(this, getString(R.string.register_title) + " - Coming Soon!", Toast.LENGTH_SHORT).show()
        }

        // Set up forgot password (for future implementation)
        tvForgotPassword.setOnClickListener {
            // In a real app, this would navigate to a forgot password screen
            Toast.makeText(this, getString(R.string.forgot_password) + " - Functionality not yet implemented.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun performLogin() {
        val username = etUsernameEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Get hardcoded credentials from strings.xml
        val demoUsername = getString(R.string.demo_username)
        val demoPassword = getString(R.string.demo_password)

        if (username == demoUsername && password == demoPassword) {
            // Login successful
            Toast.makeText(this, "¡Bienvenido a " + getString(R.string.app_name) + "!", Toast.LENGTH_SHORT).show()

            // Navigate to the main activity (e.g., MapActivity or HomeActivity)
            val intent = Intent(this, MainActivity::class.java) // Replace MainActivity with your actual main screen
            startActivity(intent)
            finish() // Finish LoginActivity so user can't go back to it with back button
        } else {
            // Login failed
            Toast.makeText(this, getString(R.string.error_invalid_credentials), Toast.LENGTH_SHORT).show()
        }
    }
}