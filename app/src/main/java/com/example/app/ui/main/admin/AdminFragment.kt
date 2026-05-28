package com.example.app.ui.main.admin

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
import com.example.app.modelos.Usuario
import com.example.app.supabase.Supabase
import com.example.app.ui.main.admin.adapters.AdminOrdenAdapter
import com.example.app.ui.main.admin.adapters.AdminOrdenDisplay
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.app.utils.LoadingUtil
import android.app.AlertDialog

class AdminFragment : Fragment() {

    private lateinit var rvOrdenes: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: AdminOrdenAdapter
    private var loadingDialog: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_admin, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvOrdenes = view.findViewById(R.id.rv_admin_ordenes)
        rvOrdenes.layoutManager = LinearLayoutManager(requireContext())
        tvEmpty = view.findViewById(R.id.tv_empty)

        adapter = AdminOrdenAdapter(
            items = emptyList(),
            onAvanzar = { orden -> avanzarEstado(orden) },
            onCancelar = { orden -> cancelarOrden(orden) },
            onClick = { orden -> mostrarDetalleOrden(orden) }
        )
        rvOrdenes.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        cargarOrdenes()
    }

    private fun cargarOrdenes() {
        if (!isAdded) return
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Cargando pedidos...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ordenes = Supabase.client.from("ordenes")
                    .select { order("created_at", Order.DESCENDING) }
                    .decodeList<Orden>()

                val displayList = mutableListOf<AdminOrdenDisplay>()
                for (orden in ordenes) {
                    val usuario = Supabase.client.from("usuarios")
                        .select { filter { eq("id", orden.usuarioId) } }
                        .decodeSingleOrNull<Usuario>()

                    displayList.add(AdminOrdenDisplay(
                        orden = orden,
                        emailUsuario = usuario?.email ?: orden.usuarioId.take(8)
                    ))
                }

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    adapter.actualizarLista(displayList)
                    val isEmpty = displayList.isEmpty()
                    rvOrdenes.visibility = if (isEmpty) View.GONE else View.VISIBLE
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

    private fun avanzarEstado(orden: Orden) {
        val estados = listOf("pendiente", "confirmado", "en_proceso", "enviado", "entregado")
        val index = estados.indexOf(orden.estado)
        if (index < 0 || index >= estados.size - 1) return

        val nuevoEstado = estados[index + 1]
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Actualizando...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Supabase.client.from("ordenes").update(buildJsonObject {
                    put("estado", nuevoEstado)
                }) { filter { eq("id", orden.id) } }

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    Toast.makeText(requireContext(), "Orden actualizada a ${nuevoEstado.replace("_", " ")}", Toast.LENGTH_SHORT).show()
                    cargarOrdenes()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    Toast.makeText(requireContext(), "Error al actualizar orden", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun cancelarOrden(orden: Orden) {
        if (!isAdded) return
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Cancelando...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Supabase.client.from("ordenes").update(buildJsonObject {
                    put("estado", "cancelado")
                }) { filter { eq("id", orden.id) } }

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    Toast.makeText(requireContext(), "Orden cancelada", Toast.LENGTH_SHORT).show()
                    cargarOrdenes()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    Toast.makeText(requireContext(), "Error al cancelar orden", Toast.LENGTH_SHORT).show()
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
                    append("👤 ${orden.usuarioId.take(8)}\n")
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
