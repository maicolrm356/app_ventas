package com.example.app.ui.main.productos.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.app.R
import com.example.app.modelos.Producto

data class ProductoDisplay(
    val producto: Producto,
    val imagenUrl: String? = null
)

class ProductoAdapter(
    private var items: List<ProductoDisplay>,
    private val esAdmin: Boolean,
    private val onAgregarCarrito: (Producto) -> Unit,
    private val onEditar: ((Producto) -> Unit)? = null,
    private val onEliminar: ((Producto) -> Unit)? = null
) : RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder>() {

    fun actualizarLista(nuevos: List<ProductoDisplay>) {
        items = nuevos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.cardview_producto, parent, false)
        return ProductoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductoViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ProductoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivImagen: ImageView = itemView.findViewById(R.id.iv_producto_imagen)
        private val tvNombre: TextView = itemView.findViewById(R.id.tv_producto_nombre)
        private val tvDescripcion: TextView = itemView.findViewById(R.id.tv_producto_descripcion)
        private val tvPrecio: TextView = itemView.findViewById(R.id.tv_producto_precio)
        private val tvStock: TextView = itemView.findViewById(R.id.tv_producto_stock)
        private val layoutAdmin: LinearLayout = itemView.findViewById(R.id.layout_acciones_admin)
        private val btnEditar: ImageButton = itemView.findViewById(R.id.btn_editar_producto)
        private val btnEliminar: ImageButton = itemView.findViewById(R.id.btn_eliminar_producto)
        private val btnAgregarCarrito: Button = itemView.findViewById(R.id.btn_agregar_carrito)

        fun bind(display: ProductoDisplay) {
            val producto = display.producto
            ivImagen.load(display.imagenUrl) {
                placeholder(R.drawable.app_logo)
                error(R.drawable.app_logo)
            }

            tvNombre.text = producto.nombre
            tvDescripcion.text = producto.descripcion
            tvPrecio.text = "$ ${String.format("%.2f", producto.precio)}"

            if (producto.stock > 0) {
                tvStock.text = "Stock: ${producto.stock}"
                tvStock.setTextColor(itemView.context.getColor(R.color.green_light))
            } else {
                tvStock.text = itemView.context.getString(R.string.sin_stock)
                tvStock.setTextColor(itemView.context.getColor(R.color.orange))
            }

            if (esAdmin) {
                layoutAdmin.visibility = View.VISIBLE
                btnAgregarCarrito.visibility = View.GONE
                btnEditar.setOnClickListener { onEditar?.invoke(producto) }
                btnEliminar.setOnClickListener { onEliminar?.invoke(producto) }
            } else {
                layoutAdmin.visibility = View.GONE
                btnAgregarCarrito.visibility = View.VISIBLE
                btnAgregarCarrito.isEnabled = producto.stock > 0
                btnAgregarCarrito.setOnClickListener { onAgregarCarrito(producto) }
            }
        }
    }
}
