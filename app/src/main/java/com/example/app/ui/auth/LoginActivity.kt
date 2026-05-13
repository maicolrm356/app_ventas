package com.example.app.ui.auth

import android.content.Intent
import android.widget.Button
import android.os.Bundle
import android.view.ViewGroup
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

        val rootview = findViewById<ViewGroup>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootview) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val imeInsets =  insets.getInsets(WindowInsetsCompat.Type.ime())

            val bottomPadding = maxOf(systemBars.bottom, imeInsets.bottom)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)
            insets
        }

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
