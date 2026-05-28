package com.example.app.ui.main.productos

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.content.PermissionChecker
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.app.R
import com.example.app.modelos.Imagen
import com.example.app.modelos.Producto
import com.example.app.modelos.Usuario
import com.example.app.supabase.Supabase
import com.example.app.ui.main.productos.adapters.ProductoAdapter
import com.example.app.ui.main.productos.adapters.ProductoDisplay
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.util.UUID
import com.example.app.utils.LoadingUtil

class CatalogoFragment : Fragment() {

    private lateinit var rvProductos: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var fabAgregar: FloatingActionButton
    private lateinit var adapter: ProductoAdapter
    private var esAdmin = false
    private var userId: String = ""
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null
    private var dialogImagePreview: ImageView? = null
    private var dialogBtnImage: Button? = null
    private lateinit var imagePickerLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private lateinit var cameraLauncher: androidx.activity.result.ActivityResultLauncher<Uri>
    private lateinit var permissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private var loadingDialog: AlertDialog? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_catalogo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvProductos = view.findViewById(R.id.rv_productos)
        fabAgregar = view.findViewById(R.id.fab_agregar_producto)
        tvEmpty = view.findViewById(R.id.tv_empty)

        rvProductos.layoutManager = LinearLayoutManager(requireContext())

        userId = Supabase.client.auth.currentUserOrNull()?.id ?: ""

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { setSelectedImage(it) }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && cameraImageUri != null) {
                setSelectedImage(cameraImageUri!!)
            }
        }

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                abrirCamara()
            } else {
                Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }

        adapter = ProductoAdapter(
            items = emptyList(),
            esAdmin = false,
            onAgregarCarrito = { producto -> agregarAlCarrito(producto) },
            onEditar = null,
            onEliminar = null
        )
        rvProductos.adapter = adapter

        cargarRol()
    }

    private fun cargarRol() {
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Cargando...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val usuario = Supabase.client.from("usuarios")
                    .select { filter { eq("id", userId) } }
                    .decodeSingleOrNull<Usuario>()

                esAdmin = usuario?.rol == "admin"

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    adapter = ProductoAdapter(
                        items = emptyList(),
                        esAdmin = esAdmin,
                        onAgregarCarrito = { producto -> agregarAlCarrito(producto) },
                        onEditar = if (esAdmin) { { producto -> mostrarDialogoProducto(producto) } } else null,
                        onEliminar = if (esAdmin) { { producto -> confirmarEliminar(producto) } } else null
                    )
                    rvProductos.adapter = adapter
                    fabAgregar.visibility = if (esAdmin) View.VISIBLE else View.GONE
                    fabAgregar.setOnClickListener { mostrarDialogoProducto(null) }
                    cargarProductos()
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    if (!isAdded) return@withContext
                    cargarProductos()
                }
            }
        }
    }

    private fun cargarProductos() {
        if (!isAdded) return
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Cargando productos...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val productos = Supabase.client.from("productos")
                    .select { order("created_at", Order.DESCENDING) }
                    .decodeList<Producto>()

                val productosDisplay = mutableListOf<ProductoDisplay>()
                for (producto in productos) {
                    var imagenUrl: String? = null
                    if (producto.imagenId != null) {
                        try {
                            val imagen = Supabase.client.from("imagenes")
                    .select { filter { eq("id", producto.imagenId!!) } }
                                .decodeSingleOrNull<Imagen>()
                            imagenUrl = imagen?.url
                        } catch (_: Exception) {}
                    }
                    productosDisplay.add(ProductoDisplay(producto, imagenUrl))
                }

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    adapter.actualizarLista(productosDisplay)
                    val isEmpty = productosDisplay.isEmpty()
                    rvProductos.visibility = if (isEmpty) View.GONE else View.VISIBLE
                    tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    Toast.makeText(requireContext(), "Error al cargar productos", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun agregarAlCarrito(producto: Producto) {
        if (producto.stock <= 0) {
            Toast.makeText(requireContext(), "Producto sin stock", Toast.LENGTH_SHORT).show()
            return
        }
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_cantidad, null)
        val tvNombre = view.findViewById<TextView>(R.id.tv_dialog_cantidad_nombre)
        val tvStock = view.findViewById<TextView>(R.id.tv_dialog_cantidad_stock)
        val tvCantidad = view.findViewById<TextView>(R.id.tv_dialog_cantidad_valor)
        val btnMenos = view.findViewById<Button>(R.id.btn_dialog_cantidad_menos)
        val btnMas = view.findViewById<Button>(R.id.btn_dialog_cantidad_mas)

        tvNombre.text = producto.nombre
        tvStock.text = "Stock disponible: ${producto.stock}"
        var cantidad = 1

        btnMenos.setOnClickListener {
            if (cantidad > 1) {
                cantidad--
                tvCantidad.text = cantidad.toString()
            }
        }
        btnMas.setOnClickListener {
            if (cantidad < producto.stock) {
                cantidad++
                tvCantidad.text = cantidad.toString()
            } else {
                Toast.makeText(requireContext(), "Stock máximo: ${producto.stock}", Toast.LENGTH_SHORT).show()
            }
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Cantidad")
            .setView(view)
            .setPositiveButton("Agregar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            dialog.dismiss()
            agregarAlCarritoConCantidad(producto, cantidad)
        }
    }

    private fun agregarAlCarritoConCantidad(producto: Producto, cantidad: Int) {
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Agregando al carrito...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val existing = Supabase.client.from("carrito")
                    .select {
                        filter {
                            eq("usuario_id", userId)
                            eq("producto_id", producto.id)
                        }
                    }
                    .decodeList<JsonObject>()

                if (existing.isNotEmpty()) {
                    val currentCantidad = existing.first()["cantidad"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    val nuevaCantidad = currentCantidad + cantidad
                    if (nuevaCantidad > producto.stock) {
                        withContext(Dispatchers.Main) {
                            LoadingUtil.ocultarLoading(loadingDialog)
                            Toast.makeText(requireContext(), "Supera el stock disponible (máx: ${producto.stock})", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    Supabase.client.from("carrito")
                        .update(buildJsonObject {
                            put("cantidad", nuevaCantidad)
                        }) {
                            filter {
                                eq("usuario_id", userId)
                                eq("producto_id", producto.id)
                            }
                        }
                } else {
                    if (cantidad > producto.stock) {
                        withContext(Dispatchers.Main) {
                            LoadingUtil.ocultarLoading(loadingDialog)
                            Toast.makeText(requireContext(), "Stock insuficiente (máx: ${producto.stock})", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }
                    Supabase.client.from("carrito")
                        .insert(buildJsonObject {
                            put("usuario_id", userId)
                            put("producto_id", producto.id)
                            put("cantidad", cantidad)
                        })
                }

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    Toast.makeText(requireContext(), "${producto.nombre} agregado al carrito", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    Toast.makeText(requireContext(), "Error al agregar al carrito", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mostrarDialogoProducto(producto: Producto?) {
        selectedImageUri = null

        val builder = AlertDialog.Builder(requireContext())
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_producto, null)

        val etNombre = dialogView.findViewById<EditText>(R.id.et_dialog_nombre)
        val etDescripcion = dialogView.findViewById<EditText>(R.id.et_dialog_descripcion)
        val etPrecio = dialogView.findViewById<EditText>(R.id.et_dialog_precio)
        val etStock = dialogView.findViewById<EditText>(R.id.et_dialog_stock)
        dialogImagePreview = dialogView.findViewById(R.id.iv_dialog_imagen_preview)
        dialogBtnImage = dialogView.findViewById(R.id.btn_dialog_seleccionar_imagen)
        val ivPreview = dialogImagePreview!!
        val btnSeleccionar = dialogBtnImage!!

        if (producto != null) {
            etNombre.setText(producto.nombre)
            etDescripcion.setText(producto.descripcion)
            etPrecio.setText(producto.precio.toString())
            etStock.setText(producto.stock.toString())
            builder.setTitle(R.string.editar_producto)
            if (producto.imagenId != null) {
                cargarImagenPreview(ivPreview, producto)
            }
        } else {
            builder.setTitle(R.string.nuevo_producto)
        }

        btnSeleccionar.setOnClickListener {
            mostrarOpcionesImagen()
        }

        builder.setView(dialogView)
            .setPositiveButton(R.string.guardar, null)
            .setNegativeButton(R.string.cancelar, null)

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            val precioStr = etPrecio.text.toString().trim()
            val stockStr = etStock.text.toString().trim()

            if (nombre.isEmpty() || precioStr.isEmpty()) {
                Toast.makeText(requireContext(), "Nombre y precio son obligatorios", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val precio = precioStr.toDoubleOrNull()
            val stock = stockStr.toIntOrNull() ?: 0
            if (precio == null) {
                Toast.makeText(requireContext(), "Precio inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            dialog.dismiss()

            if (producto != null) {
                actualizarProducto(producto, nombre, descripcion, precio, stock)
            } else {
                crearProducto(nombre, descripcion, precio, stock)
            }
        }
    }

    private fun cargarImagenPreview(ivPreview: ImageView, producto: Producto) {
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Cargando imagen...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val imagen = Supabase.client.from("imagenes")
                    .select { filter { eq("id", producto.imagenId!!) } }
                    .decodeSingleOrNull<Imagen>()
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    ivPreview.visibility = View.VISIBLE
                    ivPreview.load(imagen?.url) {
                        placeholder(R.drawable.app_logo)
                        error(R.drawable.app_logo)
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                }
            }
        }
    }

    private fun crearProducto(nombre: String, descripcion: String, precio: Double, stock: Int) {
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Guardando producto...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var imagenId: Int? = null
                if (selectedImageUri != null) {
                    val bytes = requireContext().contentResolver.openInputStream(selectedImageUri!!)?.use { it.readBytes() }
                    if (bytes != null) {
                        val mimeType = requireContext().contentResolver.getType(selectedImageUri!!) ?: "image/jpeg"
                        val extension = mimeType.substringAfter("/")
                        val fileName = "products/${UUID.randomUUID()}.$extension"
                        Supabase.client.storage.from("productos").upload(fileName, bytes)
                        val publicUrl = Supabase.client.storage.from("productos").publicUrl(fileName)

                        val nuevaImagen = Supabase.client.from("imagenes")
                            .insert(buildJsonObject { put("url", publicUrl) }) { select() }
                            .decodeSingleOrNull<Imagen>()
                        imagenId = nuevaImagen?.id
                    }
                }

                val data = buildJsonObject {
                    put("nombre", nombre)
                    put("descripcion", descripcion)
                    put("precio", precio)
                    put("stock", stock)
                    if (imagenId != null) put("imagen_id", imagenId!!)
                }

                Supabase.client.from("productos").insert(data)
                selectedImageUri = null

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    cargarProductos()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    Toast.makeText(requireContext(), "Error al crear producto", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun actualizarProducto(producto: Producto, nombre: String, descripcion: String, precio: Double, stock: Int) {
        loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Actualizando producto...")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                var imagenId = producto.imagenId
                if (selectedImageUri != null) {
                    val bytes = requireContext().contentResolver.openInputStream(selectedImageUri!!)?.use { it.readBytes() }
                    if (bytes != null) {
                        val mimeType = requireContext().contentResolver.getType(selectedImageUri!!) ?: "image/jpeg"
                        val extension = mimeType.substringAfter("/")
                        val fileName = "products/${UUID.randomUUID()}.$extension"
                        Supabase.client.storage.from("productos").upload(fileName, bytes)
                        val publicUrl = Supabase.client.storage.from("productos").publicUrl(fileName)

                        val nuevaImagen = Supabase.client.from("imagenes")
                            .insert(buildJsonObject { put("url", publicUrl) }) { select() }
                            .decodeSingleOrNull<Imagen>()
                        imagenId = nuevaImagen?.id
                    }
                }

                val data = buildJsonObject {
                    put("nombre", nombre)
                    put("descripcion", descripcion)
                    put("precio", precio)
                    put("stock", stock)
                    if (imagenId != null) put("imagen_id", imagenId!!)
                }

                Supabase.client.from("productos").update(data) { filter { eq("id", producto.id) } }
                selectedImageUri = null

                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    cargarProductos()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingUtil.ocultarLoading(loadingDialog)
                    Toast.makeText(requireContext(), "Error al actualizar producto", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun confirmarEliminar(producto: Producto) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.confirmar_eliminar)
            .setMessage("${producto.nombre}")
            .setPositiveButton(R.string.si) { _, _ ->
                loadingDialog = LoadingUtil.mostrarLoading(requireContext(), "Eliminando producto...")
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Supabase.client.from("productos").delete { filter { eq("id", producto.id) } }
                        withContext(Dispatchers.Main) {
                            LoadingUtil.ocultarLoading(loadingDialog)
                            cargarProductos()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            LoadingUtil.ocultarLoading(loadingDialog)
                            Toast.makeText(requireContext(), "Error al eliminar producto", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(R.string.no, null)
            .show()
    }

    private fun setSelectedImage(uri: Uri) {
        selectedImageUri = uri
        dialogImagePreview?.let { preview ->
            preview.visibility = View.VISIBLE
            preview.setImageURI(uri)
        }
        dialogBtnImage?.text = "Imagen seleccionada"
    }

    private fun mostrarOpcionesImagen() {
        val opciones = arrayOf("Galería", "Cámara")
        AlertDialog.Builder(requireContext())
            .setTitle("Seleccionar imagen")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> imagePickerLauncher.launch("image/*")
                    1 -> verificarPermisoCamara()
                }
            }
            .show()
    }

    private fun verificarPermisoCamara() {
        if (PermissionChecker.checkSelfPermission(requireContext(), android.Manifest.permission.CAMERA)
            == PermissionChecker.PERMISSION_GRANTED) {
            abrirCamara()
        } else {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun abrirCamara() {
        val photoFile = File(requireContext().cacheDir, "camera_${UUID.randomUUID()}.jpg")
        cameraImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            photoFile
        )
        cameraLauncher.launch(cameraImageUri!!)
    }
}
