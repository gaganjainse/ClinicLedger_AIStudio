package com.villageclinicledger.ui.villages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.villageclinicledger.R
import com.villageclinicledger.data.models.Village
import com.villageclinicledger.data.repository.PatientRepository
import com.villageclinicledger.databinding.ActivityVillageManagementBinding
import com.villageclinicledger.ui.util.LayoutScaler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** CRUD activity for managing villages. Provides a text input + add button
 * at the top and a RecyclerView listing all existing villages with edit
 * and delete actions per row. */
class VillageManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVillageManagementBinding
    private lateinit var repository: PatientRepository
    private lateinit var adapter: VillageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVillageManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyScaling()
        repository = PatientRepository(this)

        setupToolbar()
        setupRecyclerView()
        setupAddButton()
        loadVillages()
    }

    private fun applyScaling() {
        val screenHeight = resources.displayMetrics.heightPixels
        val screenWidth = resources.displayMetrics.widthPixels
        val scaleX = screenWidth.toFloat() / 1080f
        val scaleY = screenHeight.toFloat() / 2400f

        val statusBarHeight = (84 * scaleY).toInt()
        val appBarHeight = (96 * scaleY).toInt()

        // 1. Toolbar scaling
        val toolbarLp = binding.toolbar.layoutParams as? ViewGroup.MarginLayoutParams
        toolbarLp?.let {
            it.height = appBarHeight
            it.topMargin = statusBarHeight
            binding.toolbar.layoutParams = it
        }

        // 2. Add Button Height scaling
        val btnHeight = (120 * scaleY).toInt()
        binding.btnAddVillage.layoutParams?.height = btnHeight
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupRecyclerView() {
        adapter = VillageAdapter(
            onEdit = { village -> showEditDialog(village) },
            onDelete = { village -> showDeleteDialog(village) }
        )
        binding.villagesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.villagesRecyclerView.adapter = adapter
    }

    /** Validates input and inserts a new village via the repository on IO thread. */
    private fun setupAddButton() {
        binding.btnAddVillage.setOnClickListener {
            val name = binding.villageNameInput.text.toString().trim()
            if (name.isNotBlank()) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        repository.insertVillage(Village(name = name))
                        withContext(Dispatchers.Main) {
                            binding.villageNameInput.text?.clear()
                            Toast.makeText(this@VillageManagementActivity, getString(R.string.village_added), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@VillageManagementActivity, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    /** Observes the full village list from the repository and submits it to the adapter. */
    private fun loadVillages() {
        repository.getAllVillages().observe(this) { villages ->
            adapter.submitList(villages)
        }
    }

    /** Shows a dialog pre-filled with the village name for editing. Updates
     * the village via repository on confirmation. */
    private fun showEditDialog(village: Village) {
        val editText = com.google.android.material.textfield.TextInputEditText(this)
        editText.setText(village.name)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.edit_village_title))
            .setView(editText)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotBlank()) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            repository.updateVillage(village.copy(name = newName))
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@VillageManagementActivity, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /** Confirmation dialog before deleting a village. Deletion runs on
     * IO thread; errors (e.g. foreign key constraint from existing patients)
     * are shown as toasts. */
    private fun showDeleteDialog(village: Village) {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.delete_village_title))
            .setMessage(String.format(getString(R.string.delete_village_message), village.name))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        repository.deleteVillage(village)
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@VillageManagementActivity, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}

/** Simple RecyclerView adapter (non-ListAdapter) for the village list.
 * Uses a plain mutable list with notifyDataSetChanged since village lists
 * are typically small. */
class VillageAdapter(
    private val onEdit: (Village) -> Unit,
    private val onDelete: (Village) -> Unit
) : RecyclerView.Adapter<VillageAdapter.ViewHolder>() {

    private var items: List<Village> = emptyList()

    fun submitList(list: List<Village>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_village, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameView: TextView = itemView.findViewById(R.id.villageName)
        private val btnEdit: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnEdit)
        private val btnDelete: com.google.android.material.button.MaterialButton = itemView.findViewById(R.id.btnDelete)

        fun bind(village: Village) {
            LayoutScaler.scaleTextSize(nameView, 16f)
            LayoutScaler.scaleTextSize(btnEdit, 14f)
            LayoutScaler.scaleTextSize(btnDelete, 14f)

            nameView.text = village.name
            btnEdit.setOnClickListener { onEdit(village) }
            btnDelete.setOnClickListener { onDelete(village) }
        }
    }
}
