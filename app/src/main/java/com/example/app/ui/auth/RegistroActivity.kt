package com.example.app.ui.auth

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.app.R

class RegistroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window,false)
        setContentView(R.layout.activity_registro)

        //manejo del Scroll adaptable por teclado
//        val rootview = findViewById<ViewGroup>(R.id.main)
//        ViewCompat.setOnApplyWindowInsetsListener(rootview) { v, insets ->
//            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
//            val imeInsets =  insets.getInsets(WindowInsetsCompat.Type.ime())
//
//            val bottomPadding = maxOf(systemBars.bottom, imeInsets.bottom)
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, bottomPadding)
//            insets
//        }
    }
}