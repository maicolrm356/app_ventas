package com.example.app.ui.main.productos

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.app.R
import com.example.app.modelos.CarritoItem
import com.example.app.modelos.Producto
import com.example.app.supabase.Supabase
import com.example.app.ui.main.productos.adapters.CarritoAdapter
import com.example.app.ui.main.productos.adapters.CarritoDisplayItem
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.app.utils.LoadingUtil

class CarritoFragment : Fragment() {

    private lateinit var rvCarrito: RecyclerView
    private lateinit var tvTotal: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var btnPagar: Button
    private lateinit var adapter: CarritoAdapter
    private val carritoItems = mutableListOf<CarritoDisplayItem>()
    private var loadingDialog: AlertDialog? = null
    private val total get() = carritoItems.sumOf { it.precioUnitario * it.cantidad }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_carrito, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvCarrito = view.findViewById(R.id.rv_carrito)
        tvTotal = view.findViewById(R.id.tv_total_carrito)
        btnPagar = view.findViewById(R.id.btn_confirmar_pedido)
        tvEmpty = view.findViewById(R.id.tv_empty)

        rvCarrito.layoutManager = LinearLayoutManager(requireContext())

        adapter = CarritoAdapter(
            items = carritoItems,
            onCantidadChange = { item, nuevaCantidad ->
                actualizarCantidad(item, nuevaCantidad)
            },
            onEliminar = { item -> eliminarItem(item) }
        )
        rvCarrito.adapter = adapter

        btnPagar.setText(R.string.proceder_pago)
        btnPagar.setOnClickListener { iniciarCheckout() }
    }

    override fun onResume() {
        super.onResume()
        cargarCarrito()
    }

    private fun cargarCarrito() {
        val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return

        if (!isAdded) return
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Cargando carrito...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val items = Supabase.client.from("carrito")
                    .select { filter { eq("usuario_id", userId) } }
                    .decodeList<CarritoItem>()

                val displayItems = mutableListOf<CarritoDisplayItem>()
                var totalCalc = 0.0

                for (item in items) {
                    val producto = Supabase.client.from("productos")
                        .select { filter { eq("id", item.productoId) } }
                        .decodeSingleOrNull<Producto>()

                    if (producto != null) {
                        displayItems.add(CarritoDisplayItem(
                            carritoId = item.id,
                            productoId = item.productoId,
                            nombre = producto.nombre,
                            precioUnitario = producto.precio,
                            cantidad = item.cantidad,
                            stock = producto.stock
                        ))
                        totalCalc += producto.precio * item.cantidad
                    }
                }

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    carritoItems.clear()
                    carritoItems.addAll(displayItems)
                    adapter.actualizarItems(displayItems)
                    tvTotal.text = "Total: $ ${String.format("%.2f", totalCalc)}"
                    val isEmpty = displayItems.isEmpty()
                    rvCarrito.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                    btnPagar.isEnabled = !isEmpty
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                }
            }
        }
    }

    private fun actualizarCantidad(item: CarritoDisplayItem, nuevaCantidad: Int) {
        if (nuevaCantidad > item.stock) return
        if (!isAdded) return
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Actualizando...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Supabase.client.from("carrito").update(buildJsonObject {
                    put("cantidad", nuevaCantidad)
                }) { filter { eq("id", item.carritoId) } }
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    cargarCarrito()
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                }
            }
        }
    }

    private fun eliminarItem(item: CarritoDisplayItem) {
        if (!isAdded) return
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Eliminando...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Supabase.client.from("carrito").delete { filter { eq("id", item.carritoId) } }
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    cargarCarrito()
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                }
            }
        }
    }

    private fun iniciarCheckout() {
        if (carritoItems.isEmpty()) return
        mostrarDialogoDireccion()
    }

    private fun mostrarDialogoDireccion() {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_direccion, null)
        val etDireccion = view.findViewById<EditText>(R.id.et_direccion_envio)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Dirección de envío")
            .setView(view)
            .setPositiveButton("Continuar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val direccion = etDireccion.text.toString().trim()
            if (direccion.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresa una dirección", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            mostrarDialogoPago(direccion)
        }
    }

    private fun mostrarDialogoPago(direccion: String) {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_pago, null)
        val rgMetodo = view.findViewById<RadioGroup>(R.id.rg_metodo_pago)
        val etNumero = view.findViewById<EditText>(R.id.et_tarjeta_numero)
        val etTitular = view.findViewById<EditText>(R.id.et_tarjeta_titular)
        val etVencimiento = view.findViewById<EditText>(R.id.et_tarjeta_vencimiento)
        val etCvv = view.findViewById<EditText>(R.id.et_tarjeta_cvv)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Método de pago")
            .setView(view)
            .setPositiveButton("Continuar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val metodo = when (rgMetodo.checkedRadioButtonId) {
                R.id.rb_credito -> "tarjeta_credito"
                R.id.rb_debito -> "tarjeta_debito"
                else -> {
                    Toast.makeText(requireContext(), "Selecciona un método de pago", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
            val numero = etNumero.text.toString().trim()
            val titular = etTitular.text.toString().trim()
            val vencimiento = etVencimiento.text.toString().trim()
            val cvv = etCvv.text.toString().trim()

            if (numero.isEmpty() || titular.isEmpty() || vencimiento.isEmpty() || cvv.isEmpty()) {
                Toast.makeText(requireContext(), "Completa todos los datos de la tarjeta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val ultimos4 = numero.takeLast(4)
            val datosTarjeta = "$metodo | ***$ultimos4 | $titular | $vencimiento"
            dialog.dismiss()
            mostrarResumen(direccion, metodo, datosTarjeta)
        }
    }

    private fun mostrarResumen(direccion: String, metodo: String, datosTarjeta: String) {
        val resumen = buildString {
            append("📍 Dirección:\n$direccion\n\n")
            append("💳 Pago: ${if (metodo == "tarjeta_credito") "Tarjeta de Crédito" else "Tarjeta de Débito"}\n")
            append("   ${datosTarjeta}\n\n")
            append("📦 Productos:\n")
            for (item in carritoItems) {
                append("• ${item.nombre} x${item.cantidad} — $ ${"%.2f".format(item.precioUnitario * item.cantidad)}\n")
            }
            append("\n🧾 Total: $ ${"%.2f".format(total)}")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar pedido")
            .setMessage(resumen)
            .setPositiveButton("Completar pedido") { _, _ ->
                completarPedido(direccion, metodo, datosTarjeta)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun completarPedido(direccion: String, metodo: String, datosTarjeta: String) {
        val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return

        if (!isAdded) return
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Procesando pedido...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val ordenInsert = Supabase.client.from("ordenes")
                    .insert(buildJsonObject {
                        put("usuario_id", userId)
                        put("total", total)
                        put("estado", "pendiente")
                        put("direccion_envio", direccion)
                        put("metodo_pago", metodo)
                        put("datos_tarjeta", datosTarjeta)
                    }) { select() }
                    .decodeSingleOrNull<com.example.app.modelos.Orden>()

                val ordenId = ordenInsert?.id ?: return@launch

                for (item in carritoItems) {
                    Supabase.client.from("detalle_orden").insert(buildJsonObject {
                        put("orden_id", ordenId)
                        put("producto_id", item.productoId)
                        put("cantidad", item.cantidad)
                        put("precio_unitario", item.precioUnitario)
                    })
                }

                Supabase.client.from("carrito").delete { filter { eq("usuario_id", userId) } }

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    Toast.makeText(requireContext(), "Pedido realizado con éxito", Toast.LENGTH_SHORT).show()
                    cargarCarrito()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    Toast.makeText(requireContext(), "Error al procesar el pedido", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
