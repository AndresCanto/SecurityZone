package com.example.securityzone

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class AlertaAdapter(private var alertas: List<Alerta>) : RecyclerView.Adapter<AlertaAdapter.AlertaViewHolder>() {

    class AlertaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textoAlerta: TextView = view.findViewById(R.id.textoAlerta)
        val fechaHoraAlerta: TextView = view.findViewById(R.id.fechaHoraAlerta)
        val layout: ConstraintLayout = view.findViewById(R.id.layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alerta, parent, false)
        return AlertaViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlertaViewHolder, position: Int) {
        val alerta = alertas[position]
        holder.textoAlerta.text = alerta.texto

        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        holder.fechaHoraAlerta.text = sdf.format(alerta.timestamp.toDate())

        if (alerta.msj == true) {
            holder.layout.background =
                ContextCompat.getDrawable(AppContext.context, R.drawable.msj_background_remainder)
        }
    }

    override fun getItemCount() = alertas.size

    fun updateAlertas(newAlertas: List<Alerta>) {
        alertas = newAlertas
        notifyDataSetChanged()
    }
}