package com.villageclinicledger.ui.util

import android.content.Context
import java.text.SimpleDateFormat
import java.util.*

object BackupLogger {
    private const val PREFS_NAME = "backup_logs_prefs"
    private const val KEY_LOGS = "backup_logs_list_v2"

    fun logEvent(context: Context, event: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentLogs = prefs.getStringSet(KEY_LOGS, emptySet())?.toMutableList() ?: mutableListOf()
        
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        val logEntry = "[$timestamp] $event"
        
        currentLogs.add(0, logEntry) // Add to top
        val trimmed = currentLogs.take(50) // Keep last 50
        
        prefs.edit().putStringSet(KEY_LOGS, trimmed.toSet()).apply()
    }

    fun getLogs(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_LOGS, emptySet())?.toList()?.sortedByDescending { it } ?: emptyList()
    }
}
