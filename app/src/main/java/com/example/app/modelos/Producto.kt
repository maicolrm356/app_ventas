package com.example.app.modelos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Producto(
    val id: String,
    val nombre: String,
    val descripcion: String,
    val precio: Double,
    val stock: Int,
    @SerialName("imagen_id") val imagenId: Int? = null,
    @SerialName("created_at") val createdAt: String? = null
)
