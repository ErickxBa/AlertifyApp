package com.proyecto.alertify.app

import android.app.Activity
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FacebookAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.firebase.auth.FacebookAuthProvider

/**
 * An activity that handles user authentication, providing options for
 * email/password login, registration, Google Sign-In, and Facebook Login.
 */
class LoginActivity : AppCompatActivity() {

    // Firebase Auth instance for managing user authentication.
    private lateinit var auth: FirebaseAuth

    // Google Sign In Client for initiating the Google Sign-In flow.
    private lateinit var googleSignInClient: GoogleSignInClient
    // Launcher for handling the result of the Google Sign-In activity.
    private lateinit var googleSignInLauncher: ActivityResultLauncher<Intent>

    // Facebook Callback Manager to handle responses from Facebook Login.
    private lateinit var callbackManager: CallbackManager

    // View references for UI elements.
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

    /**
     * Called when the activity is first created.
     * Initializes Firebase Auth, Google Sign-In, Facebook Login,
     * sets up UI elements and event listeners.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase Auth.
        auth = Firebase.auth

        // --- Google Sign-In Integration ---
        // 1. Configure Google Sign-In options to request an ID Token and user's email.
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Web client ID for Firebase
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // 2. Create the launcher for the Google Sign-In activity result.
        googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    // Google Sign In was successful, authenticate with Firebase
                    val account = task.getResult(ApiException::class.java)!!
                    firebaseAuthWithGoogle(account.idToken!!)
                } catch (e: ApiException) {
                    // Google Sign In failed
                    Log.w("FIREBASE_AUTH", "Google sign in failed", e)
                    Toast.makeText(this, "Falló el inicio de sesión con Google", Toast.LENGTH_SHORT).show()
                }
            }
        }
        // --- End of Google Sign-In Integration ---

        // --- Facebook Login Integration ---
        callbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance().registerCallback(callbackManager, object :
            FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                // Facebook Login successful, authenticate with Firebase
                Log.d("FIREBASE_AUTH", "Facebook onSuccess")
                firebaseAuthWithFacebook(result.accessToken)
            }
            override fun onCancel() {
                // Facebook Login cancelled by user
                Log.d("FIREBASE_AUTH", "Facebook onCancel")
            }
            override fun onError(error: FacebookException) {
                // Facebook Login failed
                Log.w("FIREBASE_AUTH", "Facebook onError", error)
                Toast.makeText(baseContext, "Falló el inicio de sesión con Facebook.", Toast.LENGTH_SHORT).show()
            }
        })
        // --- End of Facebook Login Integration ---

        // Initialize view references.
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

        applyBlurToBackground() // Apply blur effect to the background image.
        updateUIMode(true) // Set initial UI mode to "Login".

        // Set click listeners for switching between Login and Register modes.
        tvLoginTab.setOnClickListener { updateUIMode(true) }
        tvRegisterTab.setOnClickListener { updateUIMode(false) }

        // Set click listener for the main action button (Login/Register).
        btnAction.setOnClickListener {
            hideKeyboard() // Hide the soft keyboard.
            if (isLoginMode) {
                performLogin()
            } else {
                performRegistration()
            }
        }

        // Set click listener for "Forgot Password" (currently a placeholder).
        tvForgotPassword.setOnClickListener {
            Toast.makeText(this, "Recuperación no implementada aún.", Toast.LENGTH_SHORT).show()
        }

        // Set click listener for Facebook login button.
        btnFacebookLogin.setOnClickListener {
            LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
        }

        // Set click listener for Google login button.
        btnGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }
    }

    /**
     * Handles the result from activities started for a result,
     * specifically forwarding the result to the Facebook CallbackManager.
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        // Pass the activity result back to the Facebook SDK
        callbackManager.onActivityResult(requestCode, resultCode, data)
    }

    /**
     * Updates the UI elements based on whether the user is in "Login" or "Register" mode.
     * @param toLoginMode True if switching to login mode, false for register mode.
     */
    private fun updateUIMode(toLoginMode: Boolean) {
        isLoginMode = toLoginMode

        // Update tab appearance (background and text color).
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

        // Show/hide "Confirm Password" field and "Forgot Password" text.
        tilConfirmPassword.visibility = if (isLoginMode) View.GONE else View.VISIBLE
        tvForgotPassword.visibility = if (isLoginMode) View.VISIBLE else View.GONE

        // Ensure "OR" separator and social login buttons are always visible (can be adjusted if needed).
        tvOrSeparator.visibility = View.VISIBLE
        llSocialLogins.visibility = View.VISIBLE

        // Update the text of the main action button.
        btnAction.text = if (isLoginMode) getString(R.string.button_login) else getString(R.string.register_tab_text)
    }

    /**
     * Attempts to log in the user with the provided email and password using Firebase Authentication.
     * Performs basic validation on the input fields.
     */
    private fun performLogin() {
        val email = etUsernameEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validate that email and password are not empty.
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Campos requeridos.", Toast.LENGTH_SHORT).show()
            return
        }

        // Firebase Authentication for login.
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d("FIREBASE_AUTH", "signInWithEmail:success")
                    navigateToMainActivity()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("FIREBASE_AUTH", "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    /**
     * Attempts to register a new user with the provided email and password using Firebase Authentication.
     * Performs validation on input fields, including email format and password confirmation.
     */
    private fun performRegistration() {
        val email = etUsernameEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // Validate that all fields are filled.
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos.", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate email format.
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Formato de correo inválido.", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate that passwords match.
        if (password != confirmPassword) {
            Toast.makeText(this, getString(R.string.error_passwords_mismatch), Toast.LENGTH_SHORT).show()
            return
        }

        // Firebase Authentication for new user registration.
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success.
                    Log.d("FIREBASE_AUTH", "createUserWithEmail:success")
                    Toast.makeText(baseContext, getString(R.string.registration_successful), Toast.LENGTH_SHORT).show()
                    updateUIMode(true) // Switch to login mode after successful registration.
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("FIREBASE_AUTH", "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Falló el registro: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // --- Methods for Google Sign-In Flow ---

    /**
     * Initiates the Google Sign-In flow by launching the Google Sign-In intent.
     */
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    /**
     * Authenticates the user with Firebase using the Google ID token.
     * Called after a successful Google Sign-In.
     * @param idToken The Google ID token obtained from Google Sign-In.
     */
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d("FIREBASE_AUTH", "signInWithCredential(Google):success")
                    navigateToMainActivity()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("FIREBASE_AUTH", "signInWithCredential(Google):failure", task.exception)
                    Toast.makeText(baseContext, "Falló la autenticación con Firebase.", Toast.LENGTH_SHORT).show()
                }
            }
    }
    // --- End of Methods for Google Sign-In Flow ---

    // --- Methods for Facebook Login Flow ---
    /**
     * Authenticates the user with Firebase using the Facebook access token.
     * Called after a successful Facebook Login.
     * @param token The Facebook access token.
     */
    private fun firebaseAuthWithFacebook(token: com.facebook.AccessToken) {
        val credential = FacebookAuthProvider.getCredential(token.token)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d("FIREBASE_AUTH", "signInWithCredential(Facebook):success")
                    navigateToMainActivity()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w("FIREBASE_AUTH", "signInWithCredential(Facebook):failure", task.exception)
                    Toast.makeText(baseContext, "Falló la autenticación con Firebase.", Toast.LENGTH_SHORT).show()
                }
            }
    }
    // --- End of Methods for Facebook Login Flow ---

    /**
     * Navigates to the [MainActivity] after successful authentication and finishes this activity.
     * Displays a welcome message.
     */
    private fun navigateToMainActivity() {
        Log.d("FIREBASE_AUTH", "Authentication successful. Navigating to MainActivity.")
        Toast.makeText(this, getString(R.string.welcome_message), Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish() // Finish LoginActivity so user cannot navigate back to it.
    }


    /**
     * Applies a blur effect to the background image view.
     * Uses [RenderEffect] on Android S (API 31) and above,
     * and [RenderScript] for older versions.
     */
    private fun applyBlurToBackground() {
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.background_login_image)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Use RenderEffect for Android S and above for hardware-accelerated blur.
            ivBackgroundImage.setRenderEffect(
                RenderEffect.createBlurEffect(20f, 20f, Shader.TileMode.CLAMP)
            )
            ivBackgroundImage.setImageBitmap(bitmap) // Set the original bitmap
        } else {
            // For older versions, use RenderScript.
            // Scale down bitmap for performance before blurring.
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width / 4, bitmap.height / 4, false)
            val blurred = blurBitmap(this, scaledBitmap, 20f)
            ivBackgroundImage.setImageBitmap(blurred)
            // It's good practice to recycle bitmaps when they are no longer needed,
            // especially if they are large or you are creating many.
            // However, bitmap is used by ivBackgroundImage, so don't recycle it here.
            // scaledBitmap can be recycled as 'blurred' is a new bitmap or modifies scaledBitmap in-place.
            if (blurred != scaledBitmap) { // If blurBitmap returns a new bitmap
                scaledBitmap.recycle()
            }
            // bitmap.recycle() // Do not recycle the original bitmap if it's still needed or directly set
        }
    }

    /**
     * Blurs a given [Bitmap] using [RenderScript].
     * Note: RenderScript is deprecated from API level 31.
     * This method is provided as a fallback for versions older than Android S.
     *
     * @param context The application context.
     * @param bitmap The [Bitmap] to blur.
     * @param radius The blur radius (e.g., 1-25f).
     * @return The blurred [Bitmap].
     */
    @Suppress("DEPRECATION") // Suppress RenderScript deprecation warning for older API levels
    private fun blurBitmap(context: Context, bitmap: Bitmap, radius: Float): Bitmap {
        // Create a new bitmap to hold the blurred result, to avoid modifying the input bitmap directly
        // if it's being used elsewhere or if copyTo would fail on an immutable bitmap.
        val outputBitmap = Bitmap.createBitmap(bitmap)

        val rs = RenderScript.create(context)
        val input = Allocation.createFromBitmap(rs, bitmap) // Use original bitmap for input
        val output = Allocation.createFromBitmap(rs, outputBitmap) // Use the new bitmap for output
        val script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))

        script.setRadius(radius)
        script.setInput(input)
        script.forEach(output)
        output.copyTo(outputBitmap) // Copy the blurred result to outputBitmap

        // Release RenderScript resources
        input.destroy()
        output.destroy()
        script.destroy()
        rs.destroy()

        return outputBitmap
    }

    /**
     * Hides the soft keyboard if it is currently visible.
     */
    private fun hideKeyboard() {
        val view = currentFocus // Get the view that currently has focus.
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
