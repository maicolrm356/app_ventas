package com.example.app.ui.main.productos.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.app.R
import com.example.app.modelos.Orden

class OrdenAdapter(
    private var ordenes: List<Orden>,
    private val onClick: (Orden) -> Unit = {}
) : RecyclerView.Adapter<OrdenAdapter.OrdenViewHolder>() {

    fun actualizarLista(nueva: List<Orden>) {
        ordenes = nueva
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrdenViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.cardview_orden, parent, false)
        return OrdenViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrdenViewHolder, position: Int) {
        holder.bind(ordenes[position])
    }

    override fun getItemCount() = ordenes.size

    inner class OrdenViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvId: TextView = itemView.findViewById(R.id.tv_orden_id)
        private val tvFecha: TextView = itemView.findViewById(R.id.tv_orden_fecha)
        private val tvEstado: TextView = itemView.findViewById(R.id.tv_orden_estado)
        private val tvTotal: TextView = itemView.findViewById(R.id.tv_orden_total)

        fun bind(orden: Orden) {
            itemView.setOnClickListener { onClick(orden) }

            tvId.text = "Orden #${orden.id.take(8)}"
            tvFecha.text = orden.createdAt?.take(10) ?: ""
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
        }
    }
}
