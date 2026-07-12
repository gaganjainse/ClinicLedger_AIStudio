package com.villageclinicledger.ui.backup

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import androidx.room.withTransaction
import com.villageclinicledger.data.local.VillageClinicLedgerDatabase
import com.villageclinicledger.R
import com.villageclinicledger.databinding.ActivityBackupBinding
import com.villageclinicledger.service.BackupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/** Allows the user to export all app data (villages, patients, aliases,
 * transactions) to a JSON file stored in the app's internal files directory,
 * or import previously exported data by selecting a JSON file via the system
 * file picker. Import replaces all existing data. */
class BackupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBackupBinding
    private lateinit var database: VillageClinicLedgerDatabase
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /** Launcher for picking a JSON file from the device. */
    private val importLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { importData(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = VillageClinicLedgerDatabase.getDatabase(this)

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnExport.setOnClickListener { exportData() }
        binding.btnImport.setOnClickListener { importLauncher.launch("application/json") }

        updateAutoBackupStatus()
    }

    private fun updateAutoBackupStatus() {
        val isEnabled = BackupService.isBackupScheduled(this)
        binding.autoBackupStatus.text = if (isEnabled) {
            getString(R.string.auto_backup_enabled)
        } else {
            getString(R.string.auto_backup_disabled)
        }

        val lastBackup = BackupService.getLastBackupTime(this)
        binding.lastBackupText.text = if (lastBackup != null) {
            String.format(getString(R.string.last_backup), lastBackup)
        } else {
            getString(R.string.no_backup_yet)
        }
    }

    /** Serializes all four entity tables to a single JSON file and writes
     * it to the app's internal storage. The filename includes a timestamp
     * for uniqueness. */
    private fun exportData() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val villages = database.villageDao().getAllVillagesSync()
                val patients = database.patientDao().getAllPatientsSync()
                val aliases = database.aliasDao().getAllAliasesSync()
                val transactions = database.transactionDao().getAllTransactionsSync()

                val backupData = BackupData(
                    villages = villages,
                    patients = patients,
                    aliases = aliases,
                    transactions = transactions
                )

                val json = gson.toJson(backupData)
                val fileName = "village_clinic_backup_${System.currentTimeMillis()}.json"
                val file = File(filesDir, fileName)
                file.writeText(json)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BackupActivity, String.format(getString(R.string.exported_to), fileName), Toast.LENGTH_LONG).show()
                    binding.statusText.text = String.format(getString(R.string.status_exported), fileName)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BackupActivity, String.format(getString(R.string.export_failed), e.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** Reads a JSON backup file from the given URI, deserializes it into
     * a BackupData object, clears all existing data, and inserts the
     * restored records. Runs entirely on IO to avoid ANRs. */
    private fun importData(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@BackupActivity, "Could not open file", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                val json = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }

                val backupData = gson.fromJson(json, BackupData::class.java)

                if (backupData.version != BackupData.CURRENT_VERSION) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@BackupActivity, "Unsupported backup version ${backupData.version}. Expected version ${BackupData.CURRENT_VERSION}.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                database.withTransaction {
                    database.transactionDao().deleteAll()
                    database.aliasDao().deleteAll()
                    database.patientDao().deleteAll()
                    database.villageDao().deleteAll()

                    for (village in backupData.villages) {
                        database.villageDao().insertVillage(village)
                    }
                    for (patient in backupData.patients) {
                        database.patientDao().insertPatient(patient)
                    }
                    for (alias in backupData.aliases) {
                        database.aliasDao().insertAlias(alias)
                    }
                    for (transaction in backupData.transactions) {
                        database.transactionDao().insertTransaction(transaction)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BackupActivity, getString(R.string.import_successful), Toast.LENGTH_LONG).show()
                    binding.statusText.text = String.format(
                        getString(R.string.status_imported),
                        backupData.patients.size,
                        backupData.villages.size,
                        backupData.aliases.size,
                        backupData.transactions.size
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BackupActivity, String.format(getString(R.string.import_failed), e.message), Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
