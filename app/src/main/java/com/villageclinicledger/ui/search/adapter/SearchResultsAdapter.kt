package com.villageclinicledger.ui.search.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.villageclinicledger.R
import com.google.android.material.imageview.ShapeableImageView
import com.villageclinicledger.data.models.Patient

/** RecyclerView adapter for search results. Uses DiffUtil via ListAdapter
 * for efficient list updates. Each item shows the patient name, village
 * (resolved via the getVillageName lambda), phone, and current balance. */
class SearchResultsAdapter(
    private val onItemClick: (Patient) -> Unit,
    private val getVillageName: (Long) -> String = { "" },
    private val getFamilyGroupName: (Long?) -> String = { "" }
) : ListAdapter<Patient, SearchResultsAdapter.PatientViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PatientViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_patient_search, parent, false)
        return PatientViewHolder(view, onItemClick, getVillageName, getFamilyGroupName)
    }

    override fun onBindViewHolder(holder: PatientViewHolder, position: Int) {
        val patient = getItem(position)
        holder.bind(patient)
    }

    /** Holds references to the patient search result item views and binds
     * Patient data to them. Hides the phone row entirely if no phone is set. */
    class PatientViewHolder(
        itemView: View,
        private val onItemClick: (Patient) -> Unit,
        private val getVillageName: (Long) -> String,
        private val getFamilyGroupName: (Long?) -> String
    ) : RecyclerView.ViewHolder(itemView) {
        private val patientNameTextView: TextView = itemView.findViewById(R.id.patientNameTextView)
        private val villageTextView: TextView = itemView.findViewById(R.id.villageTextView)
        private val familyGroupTextView: TextView = itemView.findViewById(R.id.familyGroupTextView)
        private val phoneTextView: TextView = itemView.findViewById(R.id.phoneTextView)
        private val phoneLayout: android.widget.LinearLayout = itemView.findViewById(R.id.phoneLayout)
        private val balanceAmountTextView: TextView = itemView.findViewById(R.id.balanceAmountTextView)
        private val arrowIcon: android.widget.ImageView = itemView.findViewById(R.id.arrowIcon)
        private val patientImageView: ShapeableImageView = itemView.findViewById(R.id.patientImageView)

        /** Binds patient data to the view. Village name is resolved via the
         * injected lambda, and the phone layout is conditionally hidden. */
        fun bind(patient: Patient) {
            patientNameTextView.text = patient.name
            villageTextView.text = getVillageName(patient.villageId)

            val familyName = getFamilyGroupName(patient.familyGroupId)
            if (!familyName.isNullOrBlank()) {
                familyGroupTextView.text = "परिवार: $familyName"
                familyGroupTextView.visibility = View.VISIBLE
            } else {
                familyGroupTextView.visibility = View.GONE
            }

            if (!patient.phone.isNullOrBlank()) {
                phoneTextView.text = patient.phone
                phoneLayout.visibility = View.VISIBLE
            } else {
                phoneLayout.visibility = View.GONE
            }

            balanceAmountTextView.text = patient.formattedBalance

            val initials = patient.name.split(" ").filter { it.isNotBlank() }
                .take(2).joinToString("") { it.first().uppercase() }
            patientImageView.setImageDrawable(initialsDrawable(initials))

            itemView.setOnClickListener {
                onItemClick(patient)
            }
            arrowIcon.setOnClickListener {
                onItemClick(patient)
            }
        }

        private fun initialsDrawable(initials: String): android.graphics.drawable.Drawable {
            val size = 56
            val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#4A90D9")
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawCircle((size / 2).toFloat(), (size / 2).toFloat(), (size / 2).toFloat(), paint)
            paint.color = android.graphics.Color.WHITE
            paint.textSize = 20f
            paint.textAlign = android.graphics.Paint.Align.CENTER
            paint.isFakeBoldText = true
            canvas.drawText(initials, (size / 2).toFloat(), (size / 2 + 20 / 3).toFloat(), paint)
            return android.graphics.drawable.BitmapDrawable(itemView.resources, bitmap)
        }
    }

    /** ListAdapter DiffCallback that compares patients by ID for identity
     * and by full data equality for content changes. */
    class DiffCallback : DiffUtil.ItemCallback<Patient>() {
        override fun areItemsTheSame(oldItem: Patient, newItem: Patient): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Patient, newItem: Patient): Boolean {
            return oldItem == newItem
        }
    }
}
