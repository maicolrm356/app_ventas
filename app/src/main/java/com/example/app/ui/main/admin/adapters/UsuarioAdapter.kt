package com.example.app.ui.main.admin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.app.R
import com.example.app.modelos.Usuario

class UsuarioAdapter(
    private var usuarios: List<Usuario>,
    private val onToggleRol: (Usuario) -> Unit,
    private val onEditar: (Usuario) -> Unit,
    private val onEliminar: (Usuario) -> Unit
) : RecyclerView.Adapter<UsuarioAdapter.UsuarioViewHolder>() {

    fun actualizarLista(nueva: List<Usuario>) {
        usuarios = nueva
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.cardview_usuario, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        holder.bind(usuarios[position])
    }

    override fun getItemCount() = usuarios.size

    inner class UsuarioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre: TextView = itemView.findViewById(R.id.tv_usuario_nombre)
        private val tvEmail: TextView = itemView.findViewById(R.id.tv_usuario_email)
        private val tvRol: TextView = itemView.findViewById(R.id.tv_usuario_rol)
        private val btnToggle: Button = itemView.findViewById(R.id.btn_toggle_admin)
        private val btnEditar: Button = itemView.findViewById(R.id.btn_editar_usuario)
        private val btnEliminar: Button = itemView.findViewById(R.id.btn_eliminar_usuario)

        fun bind(usuario: Usuario) {
            tvNombre.text = usuario.nombre
            tvEmail.text = usuario.email
            tvRol.text = usuario.rol.replaceFirstChar { it.uppercase() }
            tvRol.setTextColor(
                if (usuario.rol == "admin") ContextCompat.getColor(itemView.context, R.color.green_light)
                else ContextCompat.getColor(itemView.context, R.color.color_text)
            )

            if (usuario.rol == "admin") {
                btnToggle.text = "Revocar admin"
                btnToggle.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.orange))
            } else {
                btnToggle.text = "Hacer admin"
                btnToggle.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.botones))
            }

            btnToggle.setOnClickListener { onToggleRol(usuario) }
            btnEditar.setOnClickListener { onEditar(usuario) }
            btnEliminar.setOnClickListener { onEliminar(usuario) }
        }
    }
}
