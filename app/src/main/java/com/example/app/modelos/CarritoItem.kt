package com.example.app.modelos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CarritoItem(
    val id: String = "",
    @SerialName("usuario_id") val usuarioId: String,
    @SerialName("producto_id") val productoId: String,
    val cantidad: Int
)
