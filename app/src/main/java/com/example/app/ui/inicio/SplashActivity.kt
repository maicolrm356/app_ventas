package com.example.app.ui.inicio

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.app.AlertDialog
import android.os.Looper
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.app.R
import com.example.app.supabase.Supabase
import com.example.app.utils.LoadingUtil
import com.example.app.ui.MainActivity
import com.example.app.ui.auth.LoginActivity
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : AppCompatActivity() {

    private var loadingDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        loadingDialog = LoadingUtil.mostrarLoading(this, "Cargando...")
        CoroutineScope(Dispatchers.IO).launch {
            val session = Supabase.client.auth.currentUserOrNull()

            withContext(Dispatchers.Main) {
                LoadingUtil.ocultarLoading(loadingDialog)
                Handler(Looper.getMainLooper()).postDelayed({
                    val destino = if (session != null) {
                        Intent(this@SplashActivity, MainActivity::class.java)
                    } else {
                        Intent(this@SplashActivity, InicioActivity::class.java)
                    }
                    startActivity(destino)
                    finish()
                }, 1500)
            }
        }
    }
}
