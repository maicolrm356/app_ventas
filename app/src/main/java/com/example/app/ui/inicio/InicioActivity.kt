package com.example.app.ui.inicio

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.app.R
import com.example.app.ui.auth.LoginActivity

class InicioActivity : AppCompatActivity() {

    private lateinit var btnIngresar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_inicio)

        btnIngresar = findViewById(R.id.btn_ingresar)
        btnIngresar.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}