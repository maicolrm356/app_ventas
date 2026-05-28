package com.example.app

import android.content.Context
import androidx.core.content.edit


object CredencialesManager {

    private const val PREF_NAME = "auth"
    private const val KEY_CORREO ="correo"
    private const val KEY_CONTRASENA="contrasena"
    private const val KEY_HUELLA ="huella_activa"

    fun guardarCredenciales(context: Context, correo: String, contrasena: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit {
                putString(KEY_CORREO, correo)
                putString(KEY_CONTRASENA, contrasena)
                putBoolean(KEY_HUELLA, true)
            }
    }

    fun limpiarCredenciales(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit { clear() }
    }

    fun huellaActiva(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_HUELLA, false)
    }

    fun obtenerCorreo(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CORREO, null)
    }

    fun obtenerContrasena(context: Context): String? {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CONTRASENA, null)
    }

}