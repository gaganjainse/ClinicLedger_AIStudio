package com.villageclinicledger.ui.patientdetail.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.villageclinicledger.R
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.ui.util.LayoutScaler

/** RecyclerView adapter that displays a patient's transaction history.
 * Each row shows the transaction type (color-coded), date, formatted amount
 * (red for debits, green for credits), and optional notes. */
class TransactionAdapter : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)
        holder.bind(transaction)
    }

    /** Holds item_transaction views. Binds type, date, colored amount, and notes. */
    class TransactionViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
        private val typeTextView: TextView = itemView.findViewById(R.id.typeTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val amountTextView: TextView = itemView.findViewById(R.id.amountTextView)
        private val notesTextView: TextView = itemView.findViewById(R.id.notesTextView)

        /** Binds a transaction. Translates the internal type string to a
         * localized label, applies the type-specific color, and renders
         * the amount in red (debit/medicine) or green (credit/payment). */
        fun bind(transaction: Transaction) {
            LayoutScaler.scaleTextSize(typeTextView, 15f)
            LayoutScaler.scaleTextSize(dateTextView, 13f)
            LayoutScaler.scaleTextSize(amountTextView, 16f)
            LayoutScaler.scaleTextSize(notesTextView, 12f)

            typeTextView.text = when (transaction.type) {
                "medicine" -> itemView.context.getString(R.string.medicine_type)
                "payment" -> itemView.context.getString(R.string.payment_type)
                "adjustment" -> itemView.context.getString(R.string.adjustment_type)
                else -> transaction.type
            }
            typeTextView.setTextColor(transaction.typeColor)

            dateTextView.text = transaction.createdAt.toString()

            amountTextView.text = transaction.formattedAmount
            amountTextView.setTextColor(
                if (transaction.isDebit) 
                    android.graphics.Color.parseColor("#E11D48") 
                else 
                    android.graphics.Color.parseColor("#16A34A")
            )

            notesTextView.text = transaction.notes.ifEmpty { "" }
        }
    }

    /** DiffCallback comparing transactions by ID for identity and full
     * equality for content changes. */
    class DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
