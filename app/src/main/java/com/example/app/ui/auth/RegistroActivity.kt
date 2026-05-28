package com.example.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.app.AlertDialog
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.app.R
import com.example.app.supabase.Supabase
import com.example.app.ui.MainActivity
import com.example.app.utils.LoadingUtil
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RegistroActivity : AppCompatActivity() {

    private var loadingDialog: AlertDialog? = null
    private lateinit var etNombre: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegistrar: Button
    private lateinit var yaTienesCuenta: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_registro)

        val rootview = findViewById<ViewGroup>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootview) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomPadding = maxOf(systemBars.bottom, imeInsets.bottom)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)
            insets
        }

        etNombre = findViewById(R.id.et_nombre_registro)
        etEmail = findViewById(R.id.et_email_registro)
        etPassword = findViewById(R.id.et_password_registro)
        etConfirmPassword = findViewById(R.id.et_confirm_password_registro)
        btnRegistrar = findViewById(R.id.btn_registrar)
        yaTienesCuenta = findViewById(R.id.ya_tienes_cuenta)

        yaTienesCuenta.setOnClickListener {
            finish()
        }

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val emailText = etEmail.text.toString().trim()
            val passwordText = etPassword.text.toString().trim()
            val confirmPasswordText = etConfirmPassword.text.toString().trim()

            if (nombre.isEmpty() || emailText.isEmpty() || passwordText.isEmpty()) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passwordText != confirmPasswordText) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (passwordText.length < 6) {
                Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRegistrar.isEnabled = false
            loadingDialog = LoadingUtil.mostrarLoading(this@RegistroActivity, "Registrando...")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Supabase.client.auth.signUpWith(Email) {
                        this.email = emailText
                        this.password = passwordText
                        data = buildJsonObject {
                            put("nombre", JsonPrimitive(nombre))
                        }
                    }

                    withContext(Dispatchers.Main) {
                        LoadingUtil.ocultarLoading(loadingDialog)
                        Toast.makeText(this@RegistroActivity, "Registro exitoso", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@RegistroActivity, MainActivity::class.java))
                        finish()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        LoadingUtil.ocultarLoading(loadingDialog)
                        Toast.makeText(this@RegistroActivity, "Error al registrarse. Intenta de nuevo.", Toast.LENGTH_LONG).show()
                        btnRegistrar.isEnabled = true
                    }
                }
            }
        }
    }
}
