package com.example.app.ui.main.productos.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.app.R

data class CarritoDisplayItem(
    val carritoId: String,
    val productoId: String,
    val nombre: String,
    val precioUnitario: Double,
    var cantidad: Int,
    val stock: Int
)

class CarritoAdapter(
    private var items: MutableList<CarritoDisplayItem>,
    private val onCantidadChange: (CarritoDisplayItem, Int) -> Unit,
    private val onEliminar: (CarritoDisplayItem) -> Unit
) : RecyclerView.Adapter<CarritoAdapter.CarritoViewHolder>() {

    fun actualizarItems(nuevos: List<CarritoDisplayItem>) {
        items = nuevos.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarritoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.cardview_carrito_item, parent, false)
        return CarritoViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarritoViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class CarritoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNombre: TextView = itemView.findViewById(R.id.tv_item_nombre)
        private val tvPrecioUnitario: TextView = itemView.findViewById(R.id.tv_item_precio_unitario)
        private val tvCantidad: TextView = itemView.findViewById(R.id.tv_item_cantidad)
        private val tvSubtotal: TextView = itemView.findViewById(R.id.tv_item_subtotal)
        private val btnDecrementar: Button = itemView.findViewById(R.id.btn_decrementar)
        private val btnIncrementar: Button = itemView.findViewById(R.id.btn_incrementar)
        private val btnEliminar: ImageButton = itemView.findViewById(R.id.btn_eliminar_item)

        fun bind(item: CarritoDisplayItem) {
            tvNombre.text = item.nombre
            tvPrecioUnitario.text = "$ ${String.format("%.2f", item.precioUnitario)} c/u"
            tvCantidad.text = item.cantidad.toString()
            tvSubtotal.text = "$ ${String.format("%.2f", item.precioUnitario * item.cantidad)}"

            btnDecrementar.setOnClickListener {
                if (item.cantidad > 1) {
                    item.cantidad--
                    onCantidadChange(item, item.cantidad)
                    tvCantidad.text = item.cantidad.toString()
                    tvSubtotal.text = "$ ${String.format("%.2f", item.precioUnitario * item.cantidad)}"
                }
            }

            btnIncrementar.setOnClickListener {
                if (item.cantidad < item.stock) {
                    item.cantidad++
                    onCantidadChange(item, item.cantidad)
                    tvCantidad.text = item.cantidad.toString()
                    tvSubtotal.text = "$ ${String.format("%.2f", item.precioUnitario * item.cantidad)}"
                } else {
                    val context = itemView.context
                    android.widget.Toast.makeText(context, "Stock máximo: ${item.stock}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            btnEliminar.setOnClickListener { onEliminar(item) }
        }
    }
}
