// AddressHistoryAdapter.kt
package com.proyecto.alertify.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class AddressHistoryItem(
    val address: String,
    val timestamp: String,
    val type: String // "origin" o "destination"
)

class AddressHistoryAdapter(
    private val historyList: MutableList<AddressHistoryItem>,
    private val onItemClick: (AddressHistoryItem) -> Unit
) : RecyclerView.Adapter<AddressHistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconHistory: ImageView = view.findViewById(R.id.icon_history_address)
        val textAddress: TextView = view.findViewById(R.id.text_history_address)
        val textTime: TextView = view.findViewById(R.id.text_history_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_address_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyList[position]

        holder.textAddress.text = item.address
        holder.textTime.text = item.timestamp

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = historyList.size

    fun addAddress(address: AddressHistoryItem) {
        // Evitar duplicados recientes del mismo tipo
        val isDuplicate = historyList.any {
            it.address == address.address && it.type == address.type
        }

        if (!isDuplicate) {
            historyList.add(0, address) // Agregar al inicio
            if (historyList.size > 15) { // Mantener solo las 15 más recientes
                historyList.removeAt(historyList.size - 1)
            }
            notifyItemInserted(0)
        }
    }

    fun clearHistory() {
        historyList.clear()
        notifyDataSetChanged()
    }
}