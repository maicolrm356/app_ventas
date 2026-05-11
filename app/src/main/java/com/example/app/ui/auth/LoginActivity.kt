package com.example.app.ui.auth

import android.content.Intent
import android.widget.Button
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.app.R
import com.example.app.ui.inicio.HomeActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var btnSinCuenta: TextView
    private lateinit var btnIniciarSesion: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        btnSinCuenta = findViewById(R.id.no_tienes_cuenta)
        btnSinCuenta.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        btnIniciarSesion = findViewById(R.id.btn_iniciar_sesion)
        btnIniciarSesion.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
        }

        }
    }
