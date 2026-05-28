package com.example.app.ui.main.productos

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.R
import com.example.app.modelos.DetalleOrden
import com.example.app.modelos.Orden
import com.example.app.modelos.Producto
import com.example.app.supabase.Supabase
import com.example.app.ui.main.productos.adapters.OrdenAdapter
import com.example.app.utils.LoadingUtil
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MisComprasFragment : Fragment() {

    private lateinit var rvCompras: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: OrdenAdapter
    private var loadingDialog: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_mis_compras, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvCompras = view.findViewById(R.id.rv_compras)
        rvCompras.layoutManager = LinearLayoutManager(requireContext())
        tvEmpty = view.findViewById(R.id.tv_empty)

        adapter = OrdenAdapter(emptyList()) { orden -> mostrarDetalleOrden(orden) }
        rvCompras.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        cargarCompras()
    }

    private fun cargarCompras() {
        val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return

        if (!isAdded) return
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Cargando compras...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ordenes = Supabase.client.from("ordenes")
                    .select {
                        filter { eq("usuario_id", userId) }
                        order("created_at", Order.DESCENDING)
                    }
                    .decodeList<Orden>()

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    adapter.actualizarLista(ordenes)
                    val isEmpty = ordenes.isEmpty()
                    rvCompras.visibility = if (isEmpty) View.GONE else View.VISIBLE
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

    private fun mostrarDetalleOrden(orden: Orden) {
        if (!isAdded) return
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Cargando detalle...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val detalles = Supabase.client.from("detalle_orden")
                    .select { filter { eq("orden_id", orden.id) } }
                    .decodeList<DetalleOrden>()

                val productosInfo = mutableListOf<String>()
                for (detalle in detalles) {
                    val producto = Supabase.client.from("productos")
                        .select { filter { eq("id", detalle.productoId) } }
                        .decodeSingleOrNull<Producto>()
                    val nombre = producto?.nombre ?: "Producto eliminado"
                    val subtotal = detalle.precioUnitario * detalle.cantidad
                    productosInfo.add("• $nombre x${detalle.cantidad} — $ ${"%.2f".format(detalle.precioUnitario)} c/u = $ ${"%.2f".format(subtotal)}")
                }

                val metodoPago = when (orden.metodoPago) {
                    "tarjeta_credito" -> "Tarjeta de Crédito"
                    "tarjeta_debito" -> "Tarjeta de Débito"
                    else -> orden.metodoPago
                }

                val contenido = buildString {
                    append("📦 #${orden.id.take(8)}\n")
                    append("📅 ${orden.createdAt?.take(10) ?: "—"}\n")
                    append("📍 ${orden.direccionEnvio.ifEmpty { "—" }}\n")
                    append("💳 $metodoPago\n")
                    if (orden.datosTarjeta.isNotBlank()) append("   ${orden.datosTarjeta}\n")
                    append("\n")
                    productosInfo.forEach { append("$it\n") }
                    append("\n🧾 Total: $ ${"%.2f".format(orden.total)}")
                }

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    AlertDialog.Builder(requireContext())
                        .setTitle("Detalle del pedido")
                        .setMessage(contenido)
                        .setPositiveButton("Cerrar", null)
                        .show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    Toast.makeText(requireContext(), "Error al cargar detalle", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
