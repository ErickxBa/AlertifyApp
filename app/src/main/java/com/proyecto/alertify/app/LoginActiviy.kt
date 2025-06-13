package com.proyecto.alertify.app

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class LoginActivity : AppCompatActivity() {

    private lateinit var tvLoginTab: TextView
    private lateinit var tvRegisterTab: TextView
    private lateinit var etUsernameEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var tvForgotPassword: TextView
    private lateinit var btnAction: Button
    private lateinit var btnFacebookLogin: ImageButton
    private lateinit var btnGoogleLogin: ImageButton
    private lateinit var ivBackgroundImage: ImageView
    private lateinit var loginCardView: CardView
    private lateinit var tvOrSeparator: TextView
    private lateinit var llSocialLogins: LinearLayout

    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // View references
        tvLoginTab = findViewById(R.id.tv_login_tab)
        tvRegisterTab = findViewById(R.id.tv_register_tab)
        etUsernameEmail = findViewById(R.id.et_username_email)
        etPassword = findViewById(R.id.et_password)
        tilConfirmPassword = findViewById(R.id.til_confirm_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        tvForgotPassword = findViewById(R.id.tv_forgot_password)
        btnAction = findViewById(R.id.btn_action)
        btnFacebookLogin = findViewById(R.id.btn_facebook_login)
        btnGoogleLogin = findViewById(R.id.btn_google_login)
        ivBackgroundImage = findViewById(R.id.iv_background_image)
        loginCardView = findViewById(R.id.login_card_view)
        tvOrSeparator = findViewById(R.id.tv_or_separator)
        llSocialLogins = findViewById(R.id.ll_social_logins)

        applyBlurToBackground()
        updateUIMode(true)

        tvLoginTab.setOnClickListener { updateUIMode(true) }
        tvRegisterTab.setOnClickListener { updateUIMode(false) }

        btnAction.setOnClickListener {
            hideKeyboard()
            if (isLoginMode) performLogin() else performRegistration()
        }

        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Recuperación no implementada aún.", Toast.LENGTH_SHORT).show()
        }

        btnFacebookLogin.setOnClickListener {
            Toast.makeText(this, "Login con Facebook (pendiente)", Toast.LENGTH_SHORT).show()
        }

        btnGoogleLogin.setOnClickListener {
            Toast.makeText(this, "Login con Google (pendiente)", Toast.LENGTH_SHORT).show()
        }

        //Boton Prueba de Carga
        btnAction.setOnClickListener{
            val intent = Intent(this, NotificacionesActivity::class.java)
            startActivity(intent)
        }
    }

    private fun updateUIMode(toLoginMode: Boolean) {
        isLoginMode = toLoginMode

        if (isLoginMode) {
            tvLoginTab.setBackgroundResource(R.drawable.shape_toggle_button_selected)
            tvLoginTab.setTextColor(getColor(R.color.toggle_unselected))
            tvRegisterTab.setBackgroundResource(R.drawable.shape_toggle_button_unselected)
            tvRegisterTab.setTextColor(getColor(R.color.toggle_selected))
        } else {
            tvLoginTab.setBackgroundResource(R.drawable.shape_toggle_button_unselected)
            tvLoginTab.setTextColor(getColor(R.color.toggle_selected))
            tvRegisterTab.setBackgroundResource(R.drawable.shape_toggle_button_selected)
            tvRegisterTab.setTextColor(getColor(R.color.toggle_unselected))
        }

        tilConfirmPassword.visibility = if (isLoginMode) View.GONE else View.VISIBLE
        tvForgotPassword.visibility = if (isLoginMode) View.VISIBLE else View.GONE
        tvOrSeparator.visibility = if (isLoginMode) View.VISIBLE else View.GONE
        llSocialLogins.visibility = if (isLoginMode) View.VISIBLE else View.GONE

        btnAction.text = if (isLoginMode) getString(R.string.button_login) else getString(R.string.register_tab_text)
    }

    private fun performLogin() {
        val username = etUsernameEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Campos requeridos.", Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("users", Context.MODE_PRIVATE)
        val savedPassword = prefs.getString(username, null)

        if (savedPassword == password) {
            Toast.makeText(this, getString(R.string.welcome_message), Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, getString(R.string.error_invalid_credentials), Toast.LENGTH_SHORT).show()
        }
    }

    private fun performRegistration() {
        val username = etUsernameEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            Toast.makeText(this, "Formato de correo inválido.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, getString(R.string.error_passwords_mismatch), Toast.LENGTH_SHORT).show()
            return
        }

        val prefs = getSharedPreferences("users", Context.MODE_PRIVATE)
        if (prefs.contains(username)) {
            Toast.makeText(this, "Usuario ya registrado.", Toast.LENGTH_SHORT).show()
            return
        }

        prefs.edit().putString(username, password).apply()

        Toast.makeText(this, getString(R.string.registration_successful), Toast.LENGTH_SHORT).show()
        updateUIMode(true)
    }

    private fun applyBlurToBackground() {
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.background_login_image)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ivBackgroundImage.setRenderEffect(
                RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP)
            )
            ivBackgroundImage.setImageBitmap(bitmap)
        } else {
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width / 4, bitmap.height / 4, false)
            val blurred = blurBitmap(this, scaledBitmap, 20f)
            ivBackgroundImage.setImageBitmap(blurred)
            bitmap.recycle()
            scaledBitmap.recycle()
        }
    }

    private fun blurBitmap(context: Context, bitmap: Bitmap, radius: Float): Bitmap {
        val rs = RenderScript.create(context)
        val input = Allocation.createFromBitmap(rs, bitmap)
        val output = Allocation.createTyped(rs, input.type)
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        script.setRadius(radius)
        script.setInput(input)
        script.forEach(output)
        output.copyTo(bitmap)
        rs.destroy()
        return bitmap
    }

    private fun hideKeyboard() {
        val view = currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
