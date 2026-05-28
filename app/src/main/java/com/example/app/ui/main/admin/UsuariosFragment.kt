package com.example.app.ui.main.admin

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.R
import com.example.app.modelos.Usuario
import com.example.app.supabase.Supabase
import com.example.app.ui.main.admin.adapters.UsuarioAdapter
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.app.utils.LoadingUtil

class UsuariosFragment : Fragment() {

    private lateinit var rvUsuarios: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: UsuarioAdapter
    private var loadingDialog: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_usuarios, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvUsuarios = view.findViewById(R.id.rv_usuarios)
        rvUsuarios.layoutManager = LinearLayoutManager(requireContext())
        tvEmpty = view.findViewById(R.id.tv_empty)

        adapter = UsuarioAdapter(
            usuarios = emptyList(),
            onToggleRol = { usuario -> toggleRol(usuario) },
            onEditar = { usuario -> mostrarDialogoEditar(usuario) },
            onEliminar = { usuario -> confirmarEliminar(usuario) }
        )
        rvUsuarios.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        cargarUsuarios()
    }

    private fun cargarUsuarios() {
        val currentUserId = Supabase.client.auth.currentUserOrNull()?.id ?: return

        if (!isAdded) return
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Cargando usuarios...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val usuarios = Supabase.client.from("usuarios")
                    .select()
                    .decodeList<Usuario>()

                val otrosUsuarios = usuarios.filter { it.id != currentUserId }

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    adapter.actualizarLista(otrosUsuarios)
                    val isEmpty = otrosUsuarios.isEmpty()
                    rvUsuarios.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                }
            }
        }
    }

    private fun toggleRol(usuario: Usuario) {
        val nuevoRol = if (usuario.rol == "admin") "cliente" else "admin"

        if (!isAdded) return
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Cambiando rol...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Supabase.client.from("usuarios").update(buildJsonObject {
                    put("rol", nuevoRol)
                }) { filter { eq("id", usuario.id) } }

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    Toast.makeText(requireContext(), "${usuario.nombre} ahora es $nuevoRol", Toast.LENGTH_SHORT).show()
                    cargarUsuarios()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    Toast.makeText(requireContext(), "Error al cambiar rol", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarDialogoEditar(usuario: Usuario) {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_editar_usuario, null)
        val etNombre = view.findViewById<EditText>(R.id.et_editar_usuario_nombre)
        val etEmail = view.findViewById<EditText>(R.id.et_editar_usuario_email)

        etNombre.setText(usuario.nombre)
        etEmail.setText(usuario.email)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Editar usuario")
            .setView(view)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val email = etEmail.text.toString().trim()

            if (nombre.isEmpty() || email.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialog.dismiss()
            actualizarUsuario(usuario, nombre, email)
        }
    }

    private fun actualizarUsuario(usuario: Usuario, nombre: String, email: String) {
        if (!isAdded) return
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Guardando...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Supabase.client.from("usuarios").update(buildJsonObject {
                    put("nombre", nombre)
                    put("email", email)
                }) { filter { eq("id", usuario.id) } }

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    Toast.makeText(requireContext(), "Usuario actualizado", Toast.LENGTH_SHORT).show()
                    cargarUsuarios()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    Toast.makeText(requireContext(), "Error al actualizar usuario", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun confirmarEliminar(usuario: Usuario) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar usuario")
            .setMessage("¿Eliminar a ${usuario.nombre}? Esta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ -> eliminarUsuario(usuario) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun eliminarUsuario(usuario: Usuario) {
        if (!isAdded) return
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Eliminando...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Supabase.client.from("usuarios").delete { filter { eq("id", usuario.id) } }

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    Toast.makeText(requireContext(), "${usuario.nombre} eliminado", Toast.LENGTH_SHORT).show()
                    cargarUsuarios()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    Toast.makeText(requireContext(), "Error al eliminar usuario", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
