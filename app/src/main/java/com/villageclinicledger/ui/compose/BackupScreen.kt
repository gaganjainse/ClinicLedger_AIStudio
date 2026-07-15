package com.villageclinicledger.ui.compose

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.room.withTransaction
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.villageclinicledger.R
import com.villageclinicledger.data.local.VillageClinicLedgerDatabase
import com.villageclinicledger.service.BackupService
import com.villageclinicledger.ui.backup.BackupData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { VillageClinicLedgerDatabase.getDatabase(context) }
    val gson = remember { GsonBuilder().setPrettyPrinting().create() }

    var autoBackupEnabled by remember { mutableStateOf(false) }
    var lastBackupTime by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf("") }
    var operationProgress by remember { mutableStateOf(false) }

    fun refreshStatus() {
        autoBackupEnabled = BackupService.isBackupScheduled(context)
        lastBackupTime = BackupService.getLastBackupTime(context)
    }

    LaunchedEffect(Unit) {
        refreshStatus()
        statusMessage = context.getString(R.string.local_ledger_healthy)
    }

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showDemoConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    fun executeRestore(uri: Uri) {
        operationProgress = true
        scope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.could_not_open_file), Toast.LENGTH_LONG).show()
                        operationProgress = false
                    }
                    return@launch
                }
                val json = BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
                val backupData = gson.fromJson(json, BackupData::class.java)

                if (backupData.version != BackupData.CURRENT_VERSION) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, context.getString(R.string.unsupported_backup_version, backupData.version, BackupData.CURRENT_VERSION), Toast.LENGTH_LONG).show()
                        operationProgress = false
                    }
                    return@launch
                }

                database.withTransaction {
                    // Removed destructive deleteAll() calls to prevent accidental wipes during import updates
                    for (v in backupData.villages) {
                        database.villageDao().insertVillage(v)
                    }
                    backupData.familyGroups?.let { fgs ->
                        for (fg in fgs) {
                            database.familyGroupDao().insertFamilyGroup(fg)
                        }
                    }
                    for (p in backupData.patients) {
                        database.patientDao().insertPatient(p)
                    }
                    for (a in backupData.aliases) {
                        database.aliasDao().insertAlias(a)
                    }
                    for (t in backupData.transactions) {
                        database.transactionDao().insertTransaction(t)
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.import_successful), Toast.LENGTH_LONG).show()
                    statusMessage = context.getString(R.string.import_success_status, backupData.patients.size, backupData.villages.size)
                    BackupLogger.logEvent(context, "Restored backup (${backupData.patients.size} patients)")
                    operationProgress = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.import_failed_msg, e.message), Toast.LENGTH_LONG).show()
                    operationProgress = false
                }
            }
        }
    }

    fun executeClearAllData() {
        operationProgress = true
        scope.launch(Dispatchers.IO) {
            try {
                com.villageclinicledger.data.util.DataSeeder.clearAllData(context)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.clear_success_toast), Toast.LENGTH_LONG).show()
                    refreshStatus()
                    statusMessage = context.getString(R.string.local_ledger_healthy)
                    BackupLogger.logEvent(context, "Cleared all local data")
                    operationProgress = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    operationProgress = false
                }
            }
        }
    }

    fun executeImportDemoData() {
        operationProgress = true
        scope.launch(Dispatchers.IO) {
            try {
                com.villageclinicledger.data.util.DataSeeder.seedDemoDataForce(context)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, context.getString(R.string.demo_success_toast), Toast.LENGTH_LONG).show()
                    refreshStatus()
                    statusMessage = context.getString(R.string.local_ledger_healthy)
                    BackupLogger.logEvent(context, "Seeded demo data")
                    operationProgress = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                    operationProgress = false
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingRestoreUri = uri
            showRestoreConfirmDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.backup_restore_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Giant cloud logo
                Icon(
                    imageVector = Icons.Rounded.CloudQueue,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(96.dp)
                )

                Text(
                    text = stringResource(R.string.ledger_backup_center_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.backup_center_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Auto Backup Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Autorenew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = stringResource(R.string.auto_backup_label),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Switch(
                                checked = autoBackupEnabled,
                                onCheckedChange = { isChecked ->
                                    if (isChecked) {
                                        com.villageclinicledger.service.BackupService.scheduleBackup(context)
                                        BackupLogger.logEvent(context, "Enabled Auto Backup")
                                        Toast.makeText(context, "Auto Backup Enabled", Toast.LENGTH_SHORT).show()
                                    } else {
                                        com.villageclinicledger.service.BackupService.cancelBackup(context)
                                        BackupLogger.logEvent(context, "Disabled Auto Backup")
                                        Toast.makeText(context, "Auto Backup Disabled", Toast.LENGTH_SHORT).show()
                                    }
                                    autoBackupEnabled = isChecked
                                }
                            )
                        }

                        Text(
                            text = if (lastBackupTime != null) stringResource(R.string.last_auto_backup_status, lastBackupTime!!) else stringResource(R.string.no_auto_backup_yet),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Action Buttons
                Button(
                    onClick = {
                        operationProgress = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                val villages = database.villageDao().getAllVillagesSync()
                                val patients = database.patientDao().getAllPatientsSync()
                                val aliases = database.aliasDao().getAllAliasesSync()
                                val transactions = database.transactionDao().getAllTransactionsSync()
                                val familyGroups = database.familyGroupDao().getAllFamilyGroupsSync()

                                val backupData = BackupData(
                                    villages = villages,
                                    patients = patients,
                                    aliases = aliases,
                                    transactions = transactions,
                                    familyGroups = familyGroups
                                )

                                val json = gson.toJson(backupData)
                                val fileName = "village_clinic_backup_${System.currentTimeMillis()}.json"
                                val file = File(context.filesDir, fileName)
                                file.writeText(json)

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, String.format(context.getString(R.string.exported_to), fileName), Toast.LENGTH_LONG).show()
                                    statusMessage = context.getString(R.string.export_success_status, fileName)
                                    operationProgress = false
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, context.getString(R.string.export_failed_msg, e.message), Toast.LENGTH_LONG).show()
                                    operationProgress = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.create_backup_button), fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { importLauncher.launch("application/json") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Rounded.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.restore_backup_button), fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Text(
                    text = stringResource(R.string.database_management_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showDemoConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Icon(Icons.Rounded.FolderZip, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.import_demo_data_button), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { showClearConfirmDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Rounded.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.clear_all_data_button), fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Logs Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = if (java.util.Locale.getDefault().language == "hi") "बैकअप/रीस्टोर इतिहास" else "Backup & Restore History",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        val logs = remember(operationProgress, autoBackupEnabled) { BackupLogger.getLogs(context) }
                        if (logs.isEmpty()) {
                            Text(
                                text = if (java.util.Locale.getDefault().language == "hi") "कोई इतिहास नहीं" else "No backup/restore activities logged yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                logs.forEach { log ->
                                    Text(
                                        text = log,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Current Health Status
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (showRestoreConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showRestoreConfirmDialog = false },
                    title = {
                        Text(
                            text = stringResource(R.string.restore_confirm_title),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.restore_confirm_message)
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showRestoreConfirmDialog = false
                                pendingRestoreUri?.let { executeRestore(it) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.restore_confirm_yes))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRestoreConfirmDialog = false }) {
                            Text(stringResource(R.string.restore_confirm_no))
                        }
                    }
                )
            }

            if (showClearConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showClearConfirmDialog = false },
                    title = {
                        Text(
                            text = stringResource(R.string.clear_confirm_title),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.clear_confirm_message)
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showClearConfirmDialog = false
                                executeClearAllData()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(stringResource(R.string.clear_confirm_yes))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirmDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            if (showDemoConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDemoConfirmDialog = false },
                    title = {
                        Text(
                            text = stringResource(R.string.demo_confirm_title),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.demo_confirm_message)
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDemoConfirmDialog = false
                                executeImportDemoData()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(stringResource(R.string.demo_confirm_yes))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDemoConfirmDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            if (operationProgress) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

object BackupLogger {
    fun logEvent(context: android.content.Context, event: String) {
        val prefs = context.getSharedPreferences("clinic_ledger_prefs", android.content.Context.MODE_PRIVATE)
        val logsSet = prefs.getStringSet("backup_logs_list_v2", emptySet()) ?: emptySet()
        val logs = logsSet.toMutableList()
        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        logs.add(0, "[$timestamp] $event") // Newest first
        // Limit to last 8 logs to display nicely
        val trimmed = if (logs.size > 8) logs.take(8) else logs
        prefs.edit().putStringSet("backup_logs_list_v2", trimmed.toSet()).apply()
    }

    fun getLogs(context: android.content.Context): List<String> {
        val prefs = context.getSharedPreferences("clinic_ledger_prefs", android.content.Context.MODE_PRIVATE)
        val logs = prefs.getStringSet("backup_logs_list_v2", emptySet())?.toList() ?: emptyList()
        return logs.sortedWith(compareByDescending { it })
    }
}
