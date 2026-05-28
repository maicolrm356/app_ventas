package com.example.app.modelos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Imagen(
    val id: Int,
    val url: String,
    @SerialName("created_at") val createdAt: String? = null
)
