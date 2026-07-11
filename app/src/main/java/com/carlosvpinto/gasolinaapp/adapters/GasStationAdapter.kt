package com.carlosvpinto.gasolinaapp.adapters

import android.view.View
import android.widget.TextView
import com.carlosvpinto.gasolinaapp.GasStation
import com.carlosvpinto.gasolinaapp.R

// --- EL "ADAPTADOR" QUE CONSTRUYE LA LISTA ---
class GasStationAdapter(
    private val stations: List<GasStation>,
    private val userTankCapacity: Double,
    private val govtPrice: Double,
    private val onStationClick: (GasStation) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<GasStationAdapter.ViewHolder>() {

    // Variable para saber cuál está seleccionada (La 0 es la más barata por defecto)
    var selectedPosition = 0

    class ViewHolder(view: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(view) {
        val card = view.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardItem)
        val tvName = view.findViewById<TextView>(R.id.tvItemName)
        val tvBadge = view.findViewById<TextView>(R.id.tvItemBadge)
        val tvPrice = view.findViewById<TextView>(R.id.tvItemPrice)
        val tvDistance = view.findViewById<TextView>(R.id.tvItemDistance)
        val tvSavings = view.findViewById<TextView>(R.id.tvItemSavings)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_gas_station, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val station = stations[position]

        // Matemáticas del ahorro
        val ahorroPorGalon = govtPrice - station.price
        val ahorroTotal = ahorroPorGalon * userTankCapacity

        holder.tvName.text = station.name
        holder.tvPrice.text = "$${station.price}"
        holder.tvDistance.text = String.format("%.1f km", station.distanceKm)

        // Etiqueta de Ahorro
        if (ahorroTotal >= 0) {
            holder.tvSavings.text = String.format("+$%.2f", ahorroTotal)
            holder.tvSavings.setTextColor(android.graphics.Color.parseColor("#22C55E")) // Verde
        } else {
            holder.tvSavings.text = String.format("-$%.2f", Math.abs(ahorroTotal))
            holder.tvSavings.setTextColor(android.graphics.Color.parseColor("#EF4444")) // Rojo
        }

        // ¿Es la #1 (La más barata)? Le ponemos la medalla
        holder.tvBadge.visibility = if (position == 0) View.VISIBLE else View.GONE

        // ¿ESTÁ SELECCIONADA? (Aplicamos los colores de tu HTML)
        if (position == selectedPosition) {
            holder.card.strokeColor = android.graphics.Color.parseColor("#22C55E") // Borde Verde
            holder.card.setCardBackgroundColor(android.graphics.Color.parseColor("#052e16")) // Fondo verde oscuro
        } else {
            holder.card.strokeColor = android.graphics.Color.parseColor("#334155") // Borde gris
            holder.card.setCardBackgroundColor(android.graphics.Color.parseColor("#0F172A")) // Fondo azul oscuro
        }

        // Evento Click
        holder.card.setOnClickListener {
            val oldPosition = selectedPosition
            selectedPosition = position
            notifyItemChanged(oldPosition) // Des-selecciona la anterior
            notifyItemChanged(selectedPosition) // Selecciona la nueva

            onStationClick(station) // Llama a la función del mapa
        }
    }

    override fun getItemCount() = stations.size
}