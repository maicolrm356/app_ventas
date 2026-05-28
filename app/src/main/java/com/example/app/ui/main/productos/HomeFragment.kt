package com.example.app.ui.main.productos

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.app.R
import com.example.app.modelos.Usuario
import com.example.app.supabase.Supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.app.utils.LoadingUtil
import android.app.AlertDialog

class HomeFragment : Fragment() {

    private var onNavigate: ((Int) -> Unit)? = null
    private var loadingDialog: AlertDialog? = null

    fun setOnNavigateListener(listener: (Int) -> Unit) {
        onNavigate = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvBienvenida = view.findViewById<TextView>(R.id.tv_bienvenida)
        val layoutButtons = view.findViewById<LinearLayout>(R.id.layout_buttons_home)
        val btnProductos = view.findViewById<Button>(R.id.btn_home_productos)
        val btnCarrito = view.findViewById<Button>(R.id.btn_home_carrito)
        val btnCompras = view.findViewById<Button>(R.id.btn_home_compras)
        val btnGestion = view.findViewById<Button>(R.id.btn_home_gestion)
        val btnUsuarios = view.findViewById<Button>(R.id.btn_home_usuarios)

        val userId = Supabase.client.auth.currentUserOrNull()?.id

        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Cargando...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val usuario = if (userId != null) {
                    Supabase.client.from("usuarios")
                        .select { filter { eq("id", userId) } }
                        .decodeSingleOrNull<Usuario>()
                } else null

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    tvBienvenida.text = "Bienvenido, ${usuario?.nombre ?: "Usuario"}"
                    layoutButtons.visibility = View.VISIBLE

                    val isAdmin = usuario?.rol == "admin"
                    btnGestion.visibility = if (isAdmin) View.VISIBLE else View.GONE
                    btnUsuarios.visibility = if (isAdmin) View.VISIBLE else View.GONE
                    btnCarrito.visibility = if (isAdmin) View.GONE else View.VISIBLE
                    btnCompras.visibility = if (isAdmin) View.GONE else View.VISIBLE
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    tvBienvenida.text = "Bienvenido"
                }
            }
        }

        btnProductos.setOnClickListener { onNavigate?.invoke(R.id.navigation_productos) }
        btnCarrito.setOnClickListener { onNavigate?.invoke(R.id.navigation_carrito) }
        btnCompras.setOnClickListener { onNavigate?.invoke(R.id.navigation_compras) }
        btnGestion.setOnClickListener { onNavigate?.invoke(R.id.navigation_gestion) }
        btnUsuarios.setOnClickListener { onNavigate?.invoke(R.id.navigation_usuarios) }
    }
}