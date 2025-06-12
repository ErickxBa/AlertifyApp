package com.proyecto.alertify.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout // Import TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var tvLoginTab: TextView
    private lateinit var tvRegisterTab: TextView
    private lateinit var etUsernameEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilConfirmPassword: TextInputLayout // NEW: Reference to Confirm Password TextInputLayout
    private lateinit var etConfirmPassword: TextInputEditText // NEW: Reference to Confirm Password EditText
    private lateinit var tvForgotPassword: TextView
    private lateinit var btnAction: Button // RENAMED: Action button (Login/Register)
    private lateinit var btnFacebookLogin: ImageButton
    private lateinit var btnAppleLogin: ImageButton
    private lateinit var btnGoogleLogin: ImageButton
    private lateinit var ivBackgroundImage: ImageView
    private lateinit var loginCardView: CardView
    private lateinit var tvOrSeparator: TextView // NEW: Reference to "or" TextView
    private lateinit var llSocialLogins: LinearLayout // NEW: Reference to Social Logins LinearLayout

    private var isLoginMode: Boolean = true // Track current mode (Login by default)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Views
        tvLoginTab = findViewById(R.id.tv_login_tab)
        tvRegisterTab = findViewById(R.id.tv_register_tab)
        etUsernameEmail = findViewById(R.id.et_username_email)
        etPassword = findViewById(R.id.et_password)
        tilConfirmPassword = findViewById(R.id.til_confirm_password) // Initialize
        etConfirmPassword = findViewById(R.id.et_confirm_password) // Initialize
        tvForgotPassword = findViewById(R.id.tv_forgot_password)
        btnAction = findViewById(R.id.btn_action) // Initialize with new ID
        btnFacebookLogin = findViewById(R.id.btn_facebook_login)
        btnGoogleLogin = findViewById(R.id.btn_google_login)
        ivBackgroundImage = findViewById(R.id.iv_background_image)
        loginCardView = findViewById(R.id.login_card_view)
        tvOrSeparator = findViewById(R.id.tv_or_separator) // Initialize
        llSocialLogins = findViewById(R.id.ll_social_logins) // Initialize

        // Apply blur to the background image
        applyBlurToBackground()

        // Set initial state for tabs (Login mode by default)
        updateUIMode(true)

        // Click listeners for tabs
        tvLoginTab.setOnClickListener { updateUIMode(true) }
        tvRegisterTab.setOnClickListener { updateUIMode(false) }

        // Click listener for Action button (Login/Register)
        btnAction.setOnClickListener {
            if (isLoginMode) {
                performLogin()
            } else {
                performRegistration()
            }
        }

        // Click listener for Forgot Password
        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Funcionalidad de recuperar contraseña no implementada.", Toast.LENGTH_SHORT).show()
        }

        // Click listeners for social login buttons (placeholders)
        btnFacebookLogin.setOnClickListener {
            Toast.makeText(this, "Login con Facebook (no implementado)", Toast.LENGTH_SHORT).show()
        }
        btnAppleLogin.setOnClickListener {
            Toast.makeText(this, "Login con Apple (no implementado)", Toast.LENGTH_SHORT).show()
        }
        btnGoogleLogin.setOnClickListener {
            Toast.makeText(this, "Login con Google (no implementado)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUIMode(toLoginMode: Boolean) {
        isLoginMode = toLoginMode

        // Update toggle tab appearance
        if (isLoginMode) {
            tvLoginTab.setBackgroundResource(R.drawable.shape_toggle_button_selected)
            tvLoginTab.setTextColor(getColor(R.color.toggle_unselected)) // white
            tvRegisterTab.setBackgroundResource(R.drawable.shape_toggle_button_unselected)
            tvRegisterTab.setTextColor(getColor(R.color.toggle_selected)) // black
        } else {
            tvLoginTab.setBackgroundResource(R.drawable.shape_toggle_button_unselected)
            tvLoginTab.setTextColor(getColor(R.color.toggle_selected)) // black
            tvRegisterTab.setBackgroundResource(R.drawable.shape_toggle_button_selected)
            tvRegisterTab.setTextColor(getColor(R.color.toggle_unselected)) // white
        }

        // Update visibility of Confirm Password and other elements
        tilConfirmPassword.visibility = if (isLoginMode) View.GONE else View.VISIBLE
        tvForgotPassword.visibility = if (isLoginMode) View.VISIBLE else View.GONE
        tvOrSeparator.visibility = if (isLoginMode) View.VISIBLE else View.GONE
        llSocialLogins.visibility = if (isLoginMode) View.VISIBLE else View.GONE

        // Update main action button text
        btnAction.text = if (isLoginMode) getString(R.string.button_login) else getString(R.string.register_tab_text)
    }

    private fun performLogin() {
        val username = etUsernameEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        val demoUsername = getString(R.string.demo_username)
        val demoPassword = getString(R.string.demo_password)

        if (username == demoUsername && password == demoPassword) {
            Toast.makeText(this, getString(R.string.welcome_message), Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            Toast.makeText(this, getString(R.string.error_invalid_credentials), Toast.LENGTH_SHORT).show()
        }
    }

    private fun performRegistration() {
        val username = etUsernameEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // Basic validation for registration
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, getString(R.string.error_passwords_mismatch), Toast.LENGTH_SHORT).show()
            return
        }

        // TODO: Implement actual user registration (e.g., call API, save to database)
        // For now, we'll just show a success message and switch to login mode
        Toast.makeText(this, getString(R.string.registration_successful), Toast.LENGTH_SHORT).show()
        updateUIMode(true) // Switch back to login mode after successful registration (conceptual)
    }

    // Existing blurBitmap function
    private fun applyBlurToBackground() {
        val originalBitmap = BitmapFactory.decodeResource(resources, R.drawable.background_login_image)
        if (originalBitmap != null) {
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, originalBitmap.width / 4, originalBitmap.height / 4, false)
            val blurredBitmap = blurBitmap(this, scaledBitmap, 20f)
            ivBackgroundImage.setImageBitmap(blurredBitmap)
            originalBitmap.recycle()
            scaledBitmap.recycle()
        } else {
            Toast.makeText(this, "Error al cargar la imagen de fondo.", Toast.LENGTH_LONG).show()
        }
    }

    private fun blurBitmap(context: Context, bitmap: Bitmap, radius: Float): Bitmap {
        if (radius <= 0f || radius > 25f) {
            throw IllegalArgumentException("radius must be between 0 and 25 (inclusive)")
        }
        val rsContext = RenderScript.create(context)
        val input = Allocation.createFromBitmap(rsContext, bitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT)
        val output = Allocation.createTyped(rsContext, input.type)
        val script = ScriptIntrinsicBlur.create(rsContext, Element.U8_4(rsContext))
        script.setRadius(radius)
        script.setInput(input)
        script.forEach(output)
        output.copyTo(bitmap)

        input.destroy()
        output.destroy()
        script.destroy()
        rsContext.destroy()

        return bitmap
    }
}