package com.example.app.ui.main.perfil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.app.R
import com.example.app.modelos.Usuario
import com.example.app.supabase.Supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.app.utils.LoadingUtil
import android.app.AlertDialog

class PerfilFragment : Fragment() {

    private lateinit var tvNombre: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvRol: TextView
    private lateinit var etNombre: EditText
    private lateinit var btnGuardar: Button
    private var loadingDialog: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_perfil, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvNombre = view.findViewById(R.id.tv_perfil_nombre)
        tvEmail = view.findViewById(R.id.tv_perfil_email)
        tvRol = view.findViewById(R.id.tv_perfil_rol)
        etNombre = view.findViewById(R.id.et_perfil_nombre)
        btnGuardar = view.findViewById(R.id.btn_guardar_perfil)

        btnGuardar.setOnClickListener { guardarNombre() }

        cargarPerfil()
    }

    private fun cargarPerfil() {
        val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return

        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Cargando perfil...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val usuario = Supabase.client.from("usuarios")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<Usuario>()

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    usuario?.let {
                        tvNombre.text = it.nombre
                        tvEmail.text = it.email
                        tvRol.text = it.rol.replaceFirstChar { c -> c.uppercase() }
                        etNombre.setText(it.nombre)
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                }
            }
        }
    }

    private fun guardarNombre() {
        val nuevoNombre = etNombre.text.toString().trim()
        if (nuevoNombre.isEmpty()) return

        val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return

        if (!isAdded) return
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Guardando...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Supabase.client.from("usuarios").update(buildJsonObject {
                    put("nombre", nuevoNombre)
                }) { filter { eq("id", userId) } }

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    tvNombre.text = nuevoNombre
                    Toast.makeText(requireContext(), "Nombre actualizado", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    Toast.makeText(requireContext(), "Error al actualizar nombre", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
