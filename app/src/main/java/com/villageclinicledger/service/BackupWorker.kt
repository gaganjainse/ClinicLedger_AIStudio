package com.villageclinicledger.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.GsonBuilder
import com.villageclinicledger.data.local.VillageClinicLedgerDatabase
import com.villageclinicledger.ui.backup.BackupData
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = VillageClinicLedgerDatabase.getDatabase(applicationContext)

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

            val gson = GsonBuilder().setPrettyPrinting().create()
            val json = gson.toJson(backupData)

            val backupDir = File(applicationContext.getExternalFilesDir(null), "backups")
            backupDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(backupDir, "auto_backup_$timestamp.json")
            file.writeText(json)

            cleanupOldBackups(backupDir)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun cleanupOldBackups(backupDir: File) {
        val cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        backupDir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }
}
