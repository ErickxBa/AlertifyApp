package com.proyecto.alertify.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Modelo que representa un ítem del historial de direcciones.
 *
 * @param address La dirección almacenada.
 * @param timestamp Hora o fecha cuando se guardó la dirección.
 * @param type Tipo (ej. origen o destino), aunque ya no se usa para validación.
 */
data class AddressHistoryItem(
    val address: String,
    val timestamp: String,
)

/**
 * Adapter para mostrar y gestionar la lista de historial de direcciones.
 *
 * @property historyList Lista mutable con los ítems del historial.
 * @property onItemClick Callback para manejar clicks sobre un ítem.
 */
class AddressHistoryAdapter(
    private val historyList: MutableList<AddressHistoryItem>,
    private val onItemClick: (AddressHistoryItem) -> Unit
) : RecyclerView.Adapter<AddressHistoryAdapter.HistoryViewHolder>() {

    // Ítem especial "Mi ubicación"
    private val myLocationItem = AddressHistoryItem(
        address = "Mi ubicación",
        timestamp = "" // sin timestamp
    )

    /**
     * ViewHolder que contiene las referencias a las vistas de un ítem.
     */
    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textAddress: TextView = view.findViewById(R.id.text_history_address)
        val textTime: TextView = view.findViewById(R.id.text_history_time)
    }

    /**
     * Infla el layout para un ítem y crea el ViewHolder correspondiente.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_address_history, parent, false)
        return HistoryViewHolder(view)
    }

    /**
     * Vincula los datos de un ítem con las vistas del ViewHolder.
     *
     * @param holder ViewHolder a actualizar.
     * @param position Posición del ítem en la lista.
     */
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        if (position == 0) {
            // Ítem fijo "Mi ubicación"
            holder.textAddress.text = myLocationItem.address
            holder.textTime.visibility = View.GONE

            holder.itemView.setOnClickListener {
                onItemClick(myLocationItem)
            }
        } else {
            // Ítems normales del historial
            val item = historyList[position - 1]
            holder.textAddress.text = item.address
            holder.textTime.text = item.timestamp
            holder.textTime.visibility = View.VISIBLE

            holder.itemView.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    /**
     * Retorna la cantidad total de ítems en la lista.
     */
    override fun getItemCount(): Int = historyList.size + 1

    /**
     * Agrega una nueva dirección al inicio de la lista si no existe ya (evita duplicados por dirección).
     * Si la lista supera 15 ítems, elimina el último para mantener tamaño.
     *
     * @param address Nueva dirección a agregar.
     */
    fun addAddress(address: AddressHistoryItem) {
        // No agregar "Mi ubicación"
        if (address.address == myLocationItem.address) return

        val isDuplicate = historyList.any { it.address == address.address }
        if (isDuplicate) return

        if (historyList.size >= 15) {
            val lastIndex = historyList.size - 1
            historyList.removeAt(lastIndex)
            notifyItemRemoved(lastIndex + 1)
        }

        historyList.add(0, address)
        notifyItemInserted(1) // posición 1, después de "Mi ubicación"
    }

    /**
     * Limpia todo el historial de direcciones y notifica el cambio.
     */
    fun clearHistory() {
        historyList.clear()
        notifyDataSetChanged()
    }
}
