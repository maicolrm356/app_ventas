package com.example.app.modelos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DetalleOrden(
    val id: String = "",
    @SerialName("orden_id") val ordenId: String,
    @SerialName("producto_id") val productoId: String,
    val cantidad: Int,
    @SerialName("precio_unitario") val precioUnitario: Double
)
