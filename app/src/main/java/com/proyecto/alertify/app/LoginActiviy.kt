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
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class LoginActivity : AppCompatActivity() {

    // Firebase Auth instance
    private lateinit var auth: FirebaseAuth

    // View references
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

        // Initialize Firebase Auth [cite: 527]
        auth = Firebase.auth

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
            if (isLoginMode) {
                performLogin()
            } else {
                performRegistration()
            }
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
        val email = etUsernameEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Campos requeridos.", Toast.LENGTH_SHORT).show()
            return
        }

        // Firebase Authentication for login [cite: 556]
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task -> // [cite: 557]
                if (task.isSuccessful) { // [cite: 558]
                    // Sign in success
                    Log.d("FIREBASE_AUTH", "signInWithEmail:success") // [cite: 559]
                    Toast.makeText(this, getString(R.string.welcome_message), Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java)) // [cite: 561, 563]
                    finish() // [cite: 564]
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("FIREBASE_AUTH", "signInWithEmail:failure", task.exception) // [cite: 566]
                    Toast.makeText(baseContext, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show() // [cite: 567]
                }
            }
    }

    private fun performRegistration() {
        val email = etUsernameEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos.", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Formato de correo inválido.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, getString(R.string.error_passwords_mismatch), Toast.LENGTH_SHORT).show()
            return
        }

        // Firebase Authentication for new user registration [cite: 705]
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task -> // [cite: 706]
                if (task.isSuccessful) { // [cite: 707]
                    // Sign in success
                    Log.d("FIREBASE_AUTH", "createUserWithEmail:success") // [cite: 709]
                    Toast.makeText(baseContext, getString(R.string.registration_successful), Toast.LENGTH_SHORT).show() // [cite: 711]
                    updateUIMode(true) // Switch back to login screen
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("FIREBASE_AUTH", "createUserWithEmail:failure", task.exception) // [cite: 715]
                    Toast.makeText(baseContext, "Falló el registro: ${task.exception?.message}", Toast.LENGTH_SHORT).show() // [cite: 716]
                }
            }
    }

    // --- Helper functions from your original code (unchanged) ---

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