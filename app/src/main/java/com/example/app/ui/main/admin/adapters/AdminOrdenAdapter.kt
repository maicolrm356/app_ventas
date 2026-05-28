package com.example.app.ui.main.admin.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.app.R
import com.example.app.modelos.Orden

data class AdminOrdenDisplay(
    val orden: Orden,
    val emailUsuario: String
)

class AdminOrdenAdapter(
    private var items: List<AdminOrdenDisplay>,
    private val onAvanzar: (Orden) -> Unit,
    private val onCancelar: (Orden) -> Unit,
    private val onClick: (Orden) -> Unit = {}
) : RecyclerView.Adapter<AdminOrdenAdapter.AdminOrdenViewHolder>() {

    fun actualizarLista(nueva: List<AdminOrdenDisplay>) {
        items = nueva
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminOrdenViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.cardview_admin_orden, parent, false)
        return AdminOrdenViewHolder(view)
    }

    override fun onBindViewHolder(holder: AdminOrdenViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class AdminOrdenViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvId: TextView = itemView.findViewById(R.id.tv_admin_orden_id)
        private val tvUsuario: TextView = itemView.findViewById(R.id.tv_admin_orden_usuario)
        private val tvEstado: TextView = itemView.findViewById(R.id.tv_admin_orden_estado)
        private val tvTotal: TextView = itemView.findViewById(R.id.tv_admin_orden_total)
        private val layoutAcciones: LinearLayout = itemView.findViewById(R.id.layout_admin_acciones)
        private val btnAvanzar: Button = itemView.findViewById(R.id.btn_admin_avanzar_estado)
        private val btnCancelar: Button = itemView.findViewById(R.id.btn_admin_cancelar)

        fun bind(displayItem: AdminOrdenDisplay) {
            val orden = displayItem.orden
            itemView.setOnClickListener { onClick(orden) }
            tvId.text = "Orden #${orden.id.take(8)}"
            tvUsuario.text = displayItem.emailUsuario
            tvTotal.text = "$ ${String.format("%.2f", orden.total)}"

            tvEstado.text = orden.estado.replace("_", " ").replaceFirstChar { it.uppercase() }
            val color = when (orden.estado) {
                "pendiente" -> R.color.orange
                "confirmado" -> R.color.blue
                "en_proceso" -> R.color.blue_light
                "enviado" -> R.color.botones
                "entregado" -> R.color.green_light
                "cancelado" -> R.color.black
                else -> R.color.color_text
            }
            tvEstado.setTextColor(ContextCompat.getColor(itemView.context, color))

            val estados = listOf("pendiente", "confirmado", "en_proceso", "enviado", "entregado")
            val index = estados.indexOf(orden.estado)

            if (orden.estado == "cancelado" || orden.estado == "entregado") {
                layoutAcciones.visibility = View.GONE
            } else if (index in 0..3) {
                layoutAcciones.visibility = View.VISIBLE
                btnAvanzar.text = "→ ${estados[index + 1].replace("_", " ").replaceFirstChar { it.uppercase() }}"
                btnAvanzar.setOnClickListener { onAvanzar(orden) }
                btnCancelar.visibility = View.VISIBLE
                btnCancelar.setOnClickListener { onCancelar(orden) }
            } else {
                layoutAcciones.visibility = View.GONE
            }
        }
    }
}
