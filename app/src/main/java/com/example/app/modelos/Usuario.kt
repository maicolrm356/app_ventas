package com.example.app.modelos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Usuario(
    val id: String,
    val email: String,
    val nombre: String,
    val rol: String,
    @SerialName("created_at") val createdAt: String? = null
)
