package com.example.app.ui.auth

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.app.AlertDialog
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.example.app.R
import com.example.app.supabase.Supabase
import com.example.app.ui.MainActivity
import com.example.app.utils.LoadingUtil
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.Executor

class LoginActivity : AppCompatActivity() {

    private var loadingDialog: AlertDialog? = null
    private lateinit var btnSinCuenta: TextView
    private lateinit var btnIniciarSesion: Button
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnGoogle: MaterialButton
    private lateinit var tvHuella: ImageView
    private lateinit var prefs: SharedPreferences
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var executor: Executor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val rootview = findViewById<ViewGroup>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootview) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomPadding = maxOf(systemBars.bottom, imeInsets.bottom)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)
            insets
        }

        etEmail = findViewById(R.id.et_email_login)
        etPassword = findViewById(R.id.et_password_login)
        btnSinCuenta = findViewById(R.id.no_tienes_cuenta)
        btnIniciarSesion = findViewById(R.id.btn_iniciar_sesion)
        btnGoogle = findViewById(R.id.btn_iniciar_sesion_google)
        tvHuella = findViewById(R.id.btn_iniciar_sesion_fingerprint)

        prefs = getSharedPreferences("biometric_login", Context.MODE_PRIVATE)
        executor = ContextCompat.getMainExecutor(this)
        configurarBiometricPrompt()
        verificarBiometricoYCredenciales()

        btnSinCuenta.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        btnIniciarSesion.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnIniciarSesion.isEnabled = false
            loadingDialog = LoadingUtil.mostrarLoading(this@LoginActivity, "Iniciando sesión...")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Supabase.client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }
                    withContext(Dispatchers.Main) {
                        LoadingUtil.ocultarLoading(loadingDialog)
                        preguntarGuardarHuella(email, password)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        LoadingUtil.ocultarLoading(loadingDialog)
                        Toast.makeText(this@LoginActivity, "Credenciales incorrectas", Toast.LENGTH_LONG).show()
                        btnIniciarSesion.isEnabled = true
                    }
                }
            }
        }

        btnGoogle.setOnClickListener { iniciarSesionGoogle() }
        tvHuella.setOnClickListener {
            if (!prefs.contains("email") || !prefs.contains("password")) {
                Toast.makeText(this, "Primero inicia sesión con tu correo", Toast.LENGTH_SHORT).show()
            } else {
                biometricPrompt.authenticate(promptInfo)
            }
        }
    }

    private fun configurarBiometricPrompt() {
        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Inicio con huella")
            .setSubtitle("Usa tu huella para iniciar sesión")
            .setNegativeButtonText("Cancelar")
            .build()

        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val email = prefs.getString("email", "")
                val password = prefs.getString("password", "")
                if (email.isNullOrEmpty() || password.isNullOrEmpty()) {
                    Toast.makeText(this@LoginActivity, "No hay credenciales guardadas", Toast.LENGTH_SHORT).show()
                    tvHuella.visibility = View.GONE
                    return
                }
                loadingDialog = LoadingUtil.mostrarLoading(this@LoginActivity, "Iniciando sesión...")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Supabase.client.auth.signInWith(Email) {
                            this.email = email
                            this.password = password
                        }
                        withContext(Dispatchers.Main) {
                            LoadingUtil.ocultarLoading(loadingDialog)
                            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                            finish()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            LoadingUtil.ocultarLoading(loadingDialog)
                            Toast.makeText(this@LoginActivity, "Error al iniciar sesión automáticamente. Ingresa manualmente.", Toast.LENGTH_LONG).show()
                            limpiarCredenciales()
                        }
                    }
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Toast.makeText(this@LoginActivity, errString, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onAuthenticationFailed() {
                Toast.makeText(this@LoginActivity, "Huella no reconocida", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun verificarBiometricoYCredenciales() {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        tvHuella.visibility = if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) View.VISIBLE else View.GONE
    }

    private fun preguntarGuardarHuella(email: String, password: String) {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)

        if (canAuthenticate == BiometricManager.BIOMETRIC_SUCCESS) {
            if (prefs.contains("email")) {
                guardarCredenciales(email, password)
            } else {
                AlertDialog.Builder(this)
                    .setTitle("¿Habilitar huella?")
                    .setMessage("¿Deseas guardar tus datos para iniciar sesión con huella digital?")
                    .setPositiveButton("Sí") { _, _ -> guardarCredenciales(email, password); navegarAMain() }
                    .setNegativeButton("No") { _, _ -> navegarAMain() }
                    .setCancelable(false)
                    .show()
                return
            }
        }
        navegarAMain()
    }

    private fun guardarCredenciales(email: String, password: String) {
        prefs.edit()
            .putString("email", email)
            .putString("password", password)
            .apply()
        tvHuella.visibility = View.VISIBLE
    }

    private fun limpiarCredenciales() {
        prefs.edit().clear().apply()
        tvHuella.visibility = View.GONE
    }

    private fun navegarAMain() {
        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
        finish()
    }

    private fun iniciarSesionGoogle() {
        val rawNonce = UUID.randomUUID().toString()
        val digest = MessageDigest.getInstance("SHA-256").digest(rawNonce.toByteArray())
        val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId("511288017770-t94acncon3rpd5bj8obm811877tugoo9.apps.googleusercontent.com")
            .setNonce(hashedNonce)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val credentialManager = CredentialManager.create(this)

        loadingDialog = LoadingUtil.mostrarLoading(this, "Iniciando con Google...")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(this@LoginActivity, request)
                val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)

                withContext(Dispatchers.IO) {
                    Supabase.client.auth.signInWith(IDToken) {
                        idToken = googleCredential.idToken
                        provider = Google
                        nonce = rawNonce
                    }
                }

                LoadingUtil.ocultarLoading(loadingDialog)
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            } catch (e: Exception) {
                LoadingUtil.ocultarLoading(loadingDialog)
                Toast.makeText(this@LoginActivity, "Error al iniciar sesión con Google", Toast.LENGTH_LONG).show()
            }
        }
    }
}
