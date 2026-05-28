package com.example.app.modelos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Orden(
    val id: String = "",
    @SerialName("usuario_id") val usuarioId: String,
    val total: Double,
    val estado: String = "pendiente",
    @SerialName("direccion_envio") val direccionEnvio: String = "",
    @SerialName("metodo_pago") val metodoPago: String = "",
    @SerialName("datos_tarjeta") val datosTarjeta: String = "",
    @SerialName("created_at") val createdAt: String? = null
)
