package com.villageclinicledger.service

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object BackupService {

    private const val WORK_NAME = "daily_auto_backup"

    fun scheduleBackup(context: Context) {
        val request = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(androidx.work.Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build())
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancelBackup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun isBackupScheduled(context: Context): Boolean {
        val workInfo = WorkManager.getInstance(context).getWorkInfosForUniqueWork(WORK_NAME).get()
        return workInfo.any { !it.state.isFinished }
    }

    fun getLastBackupTime(context: Context): String? {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        if (!backupDir.exists()) return null

        val latestFile = backupDir.listFiles()
            ?.filter { it.isFile && it.name.startsWith("auto_backup_") }
            ?.maxByOrNull { it.lastModified() } ?: return null

        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return dateFormat.format(Date(latestFile.lastModified()))
    }
}
