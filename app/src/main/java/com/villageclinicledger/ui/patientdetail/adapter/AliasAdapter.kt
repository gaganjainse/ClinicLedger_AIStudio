package com.villageclinicledger.ui.patientdetail.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.villageclinicledger.R
import com.villageclinicledger.data.models.Alias

/** RecyclerView adapter for displaying patient aliases. Each item shows the
 * alias name and supports long-press to trigger deletion via the callback. */
class AliasAdapter(
    private val onDeleteClick: (Alias) -> Unit
) : ListAdapter<Alias, AliasAdapter.AliasViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AliasViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alias, parent, false)
        return AliasViewHolder(view, onDeleteClick)
    }

    override fun onBindViewHolder(holder: AliasViewHolder, position: Int) {
        val alias = getItem(position)
        holder.bind(alias)
    }

    /** Holds a single alias item view. Long-pressing triggers the delete callback
     * so the Fragment can show a confirmation dialog before removing it. */
    class AliasViewHolder(
        itemView: View,
        private val onDeleteClick: (Alias) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val aliasTextView: TextView = itemView.findViewById(R.id.aliasTextView)

        fun bind(alias: Alias) {
            aliasTextView.text = alias.alias

            itemView.setOnLongClickListener {
                onDeleteClick(alias)
                true
            }
        }
    }

    /** DiffCallback comparing aliases by ID for identity and full equality
     * for content changes. */
    class DiffCallback : DiffUtil.ItemCallback<Alias>() {
        override fun areItemsTheSame(oldItem: Alias, newItem: Alias): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Alias, newItem: Alias): Boolean {
            return oldItem == newItem
        }
    }
}
