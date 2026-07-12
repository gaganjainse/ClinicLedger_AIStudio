package com.villageclinicledger.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.villageclinicledger.R
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Village
import com.villageclinicledger.service.BackupService
import com.villageclinicledger.data.repository.PatientRepository
import com.villageclinicledger.ui.analytics.AnalyticsActivity
import com.villageclinicledger.ui.villages.VillageManagementActivity
import com.villageclinicledger.ui.backup.BackupActivity
import com.villageclinicledger.voice.VoiceInputSheet
import com.villageclinicledger.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** Main entry point of the app. Hosts a SearchFragment via the nav host and
 * provides the toolbar menu for navigation to villages, analytics, backup,
 * and language settings. Also handles the FAB-triggered add-patient dialog. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repository by lazy { PatientRepository(this) }
    /** Cached list of all villages, kept in memory so the add-patient dialog
     * can offer an AutoCompleteTextView without requerying the DB each time. */
    private var villages: List<Village> = emptyList()

    private val voicePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openVoiceSheet() else
            Toast.makeText(this, "Voice requires microphone permission", Toast.LENGTH_SHORT).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        loadVillages()
        BackupService.scheduleBackup(this)

        binding.voiceBottomBar.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                openVoiceSheet()
            } else {
                voicePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun openVoiceSheet() {
        VoiceInputSheet().show(supportFragmentManager, "VoiceInputSheet")
    }

    /** Observes the village list from the repository and caches it locally.
     * The LiveData remains active for the activity's lifetime so the
     * village dropdown always reflects the latest data. */
    private fun loadVillages() {
        repository.getAllVillages().observe(this) { list ->
            villages = list
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /** Dispatches toolbar item clicks to the appropriate activity or dialog.
     * Opens Analytics, VillageManagement, Backup activities, or the language
     * switcher dialog depending on which menu item was tapped. */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_analytics -> {
                startActivity(android.content.Intent(this, AnalyticsActivity::class.java))
                true
            }
            R.id.menu_villages -> {
                startActivity(android.content.Intent(this, VillageManagementActivity::class.java))
                true
            }
            R.id.menu_backup -> {
                startActivity(android.content.Intent(this, BackupActivity::class.java))
                true
            }
            R.id.menu_language -> {
                showLanguageDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /** Shows a Material dialog to add a new patient. Provides an AutoCompleteTextView
     * populated with cached village names for the user to select from. Validates that
     * the selected village exists before saving. */
    fun showAddPatientDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_patient, null)
        val nameInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.patientNameInput)
        val phoneInput = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.patientPhoneInput)
        val villageInput = dialogView.findViewById<AutoCompleteTextView>(R.id.villageAutoComplete)

        val villageNames = villages.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, villageNames)
        villageInput.setAdapter(adapter)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.add_patient_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val name = nameInput.text.toString().trim()
                if (name.isNotBlank()) {
                    val selectedVillageName = villageInput.text.toString().trim()
                    val villageId = villages.find { it.name == selectedVillageName }?.id
                    if (selectedVillageName.isNotBlank() && villageId == null) {
                        Toast.makeText(this, "Village not found. Add village first.", Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                    addPatient(name, phoneInput.text.toString().trim(), villageId ?: 0)
                } else {
                    Toast.makeText(this, getString(R.string.name_required), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /** Inserts a new Patient into the database on a background thread and
     * shows a toast on the main thread upon success or failure. */
    private fun addPatient(name: String, phone: String, villageId: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                repository.insertPatient(
                    Patient(
                        name = name,
                        villageId = villageId,
                        phone = phone
                    )
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, getString(R.string.patient_added), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Failed to add patient: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /** Presents a dialog to switch between English and Hindi locale.
     * The selected locale is applied immediately and the activity is
     * recreated to pick up string resources in the new language. */
    private fun showLanguageDialog() {
        val languages = arrayOf(getString(R.string.lang_english), getString(R.string.lang_hindi))
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.select_language))
            .setItems(languages) { _, which ->
                when (which) {
                    0 -> setLocale("en")
                    1 -> setLocale("hi")
                }
            }
            .show()
    }

    /** Applies the given language code as the device locale and recreates
     * the activity so that all loaded string resources reflect the change. */
    private fun setLocale(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = android.content.res.Configuration(resources.configuration)
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        recreate()
    }
}
