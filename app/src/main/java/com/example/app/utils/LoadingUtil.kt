package com.example.app.utils

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import com.example.app.R

object LoadingUtil {

    fun mostrarLoading(context: Context, mensaje: String = "Cargando..."): AlertDialog {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_loading, null)
        view.findViewById<TextView>(R.id.tv_loading_mensaje).text = mensaje

        return AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(false)
            .create()
            .also { it.show() }
    }

    fun ocultarLoading(dialog: AlertDialog?) {
        dialog?.dismiss()
    }
}
