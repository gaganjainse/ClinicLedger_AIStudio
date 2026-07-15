package com.villageclinicledger.ui.compose

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.ui.analytics.viewmodel.AnalyticsViewModel
import com.villageclinicledger.ui.compose.components.*
import com.villageclinicledger.ui.util.LocaleManager
import com.villageclinicledger.ui.util.LocaleManager.LocalIsHindi

@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToPatientDetail: (Long) -> Unit
) {
    AnalyticsContent(
        viewModel = viewModel,
        onNavigateToPatientDetail = onNavigateToPatientDetail
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsContent(
    viewModel: AnalyticsViewModel,
    onNavigateToPatientDetail: (Long) -> Unit
) {
    val isHindi = LocalIsHindi.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshAnalytics()
    }

    var activePatientListTitle by remember { mutableStateOf<String?>(null) }
    var activePatientList by remember { mutableStateOf<List<Patient>?>(null) }
    var activeTransactionListTitle by remember { mutableStateOf<String?>(null) }
    var activeTransactionList by remember { mutableStateOf<List<com.villageclinicledger.ui.analytics.viewmodel.TransactionWithPatient>?>(null) }

    activePatientList?.let { patients ->
        PatientListDialog(
            title = activePatientListTitle ?: "Details",
            patients = patients,
            isHindi = isHindi,
            onDismiss = { activePatientList = null },
            onNavigateToPatient = { patientId ->
                activePatientList = null
                onNavigateToPatientDetail(patientId)
            }
        )
    }

    activeTransactionList?.let { transactions ->
        TransactionListDialog(
            title = activeTransactionListTitle ?: "Transactions",
            transactions = transactions,
            isHindi = isHindi,
            onDismiss = { activeTransactionList = null },
            onNavigateToPatient = { patientId ->
                activeTransactionList = null
                onNavigateToPatientDetail(patientId)
            }
        )
    }

    if (uiState.isLoading) {
        ClinicLoadingState()
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnalyticsCard(
                            title = if (isHindi) "कुल बकाया" else "Total Outstanding",
                            value = LocaleManager.formatCurrency(uiState.totalOutstandingDues),
                            icon = Icons.Rounded.AccountBalanceWallet,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activePatientListTitle = if (isHindi) "कुल बकायादार मरीज" else "Total Outstanding Patients"
                                activePatientList = uiState.outstandingPatientsList
                            }
                        )

                        AnalyticsCard(
                            title = if (isHindi) "आज का संग्रह" else "Collected Today",
                            value = LocaleManager.formatCurrency(uiState.todayCollected),
                            icon = Icons.Rounded.Payments,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activeTransactionListTitle = if (isHindi) "आज प्राप्त संग्रह" else "Collected Today"
                                activeTransactionList = uiState.todayCollectedList
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val recoveryRateText = remember(uiState.recoveryRate) {
                            String.format(java.util.Locale.US, "%.1f%%", uiState.recoveryRate)
                        }
                        AnalyticsCard(
                            title = if (isHindi) "वसूली दर" else "Recovery Rate (Month)",
                            value = recoveryRateText,
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activePatientListTitle = if (isHindi) "सक्रिय ऋणदाता" else "All Debtor Patients"
                                activePatientList = uiState.outstandingPatientsList
                            }
                        )

                        AnalyticsCard(
                            title = if (isHindi) "इस महीने की दवा" else "Medicine Given (Month)",
                            value = LocaleManager.formatCurrency(uiState.thisMonthMedicine),
                            icon = Icons.Rounded.LocalPharmacy,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                activeTransactionListTitle = if (isHindi) "इस माह वितरित दवा" else "This Month's Medicine Ledger"
                                activeTransactionList = uiState.thisMonthMedicineList
                            }
                        )
                    }
                }
            }

            item {
                Text(
                    text = if (isHindi) "गाँव अनुसार बकाया विवरण" else "Dues Breakdown by Village",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }

            if (uiState.villageDuesList.isEmpty()) {
                item {
                    ClinicEmptyState(
                        message = "No outstanding dues recorded.",
                        messageHindi = "कोई बकाया उपलब्ध नहीं है।"
                    )
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            val maxDues = uiState.villageDuesList.maxOfOrNull { it.totalDues } ?: 1.0
                            uiState.villageDuesList.forEach { item ->
                                val progress = (item.totalDues / maxDues).toFloat().coerceIn(0f, 1f)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activePatientListTitle = if (isHindi) "${item.nameHindi.ifEmpty { item.name }} के बकायादार" else "${item.name} Outstanding Patients"
                                            activePatientList = uiState.outstandingPatientsList.filter { it.villageId == item.villageId }
                                        }
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isHindi) item.nameHindi.ifEmpty { item.name } else item.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = LocaleManager.formatCurrency(item.totalDues),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isHindi) "${item.patientCount} मरीज" else "${item.patientCount} Patients",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        val percentageText = remember(item.totalDues, uiState.totalOutstandingDues) {
                                            String.format(java.util.Locale.US, "%.1f%%", if (uiState.totalOutstandingDues > 0) (item.totalDues / uiState.totalOutstandingDues) * 100.0 else 0.0)
                                        }
                                        Text(
                                            text = percentageText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.error,
                                        trackColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = if (isHindi) "बकाया समय सीमा (निष्क्रिय खाते)" else "Overdue Dues Aging (Inactive Accounts)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DefaulterCard(
                        label = if (isHindi) "> 30 दिन" else "> 30 Days",
                        count = uiState.defaulters30Days,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            activePatientListTitle = if (isHindi) "> 30 दिन निष्क्रिय बकायादार" else "> 30 Days Inactive Accounts"
                            activePatientList = uiState.defaulters30List
                        }
                    )
                    DefaulterCard(
                        label = if (isHindi) "> 90 दिन" else "> 90 Days",
                        count = uiState.defaulters90Days,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            activePatientListTitle = if (isHindi) "> 90 दिन निष्क्रिय बकायादार" else "> 90 Days Inactive Accounts"
                            activePatientList = uiState.defaulters90List
                        }
                    )
                    DefaulterCard(
                        label = if (isHindi) "> 180 दिन" else "> 180 Days",
                        count = uiState.defaulters180Days,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            activePatientListTitle = if (isHindi) "> 180 दिन निष्क्रिय बकायादार" else "> 180 Days Inactive Accounts"
                            activePatientList = uiState.defaulters180List
                        }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activePatientListTitle = if (isHindi) "कुल बकायादार मरीज" else "Total Outstanding Patients"
                            activePatientList = uiState.outstandingPatientsList
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "शीर्ष बकाया मरीज (अधिकतम से कम)" else "Top Outstanding Patients",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (isHindi) "सभी देखें" else "View All",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState.topPatientsWithDues.isEmpty()) {
                item {
                    ClinicEmptyState(
                        message = "No patients with outstanding balance found.",
                        messageHindi = "कोई भी बकाया मरीज नहीं मिला।"
                    )
                }
            } else {
                itemsIndexed(uiState.topPatientsWithDues) { index, patient ->
                    PatientListItem(
                        patient = patient,
                        index = index,
                        isHindi = isHindi,
                        onClick = { onNavigateToPatientDetail(patient.id) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            activePatientListTitle = if (isHindi) "६०+ दिनों से नहीं आए मरीज" else "Patients Not Visited in 60+ Days"
                            activePatientList = uiState.inactivePatients60Days
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isHindi) "६०+ दिनों से नहीं आए मरीज (Chronic Follow-up)" else "Patients Not Visited in 60+ Days",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (isHindi) "सभी देखें" else "View All",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState.inactivePatients60Days.isEmpty()) {
                item {
                    ClinicEmptyState(
                        message = "All patients have visited recently.",
                        messageHindi = "कोई भी निष्क्रिय मरीज नहीं मिला।"
                    )
                }
            } else {
                itemsIndexed(uiState.inactivePatients60Days) { index, patient ->
                    PatientListItem(
                        patient = patient,
                        index = index,
                        isHindi = isHindi,
                        onClick = { onNavigateToPatientDetail(patient.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientListDialog(
    title: String,
    patients: List<Patient>,
    isHindi: Boolean,
    onDismiss: () -> Unit,
    onNavigateToPatient: (Long) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredPatients = remember(searchQuery, patients) {
        if (searchQuery.isBlank()) {
            patients
        } else {
            patients.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                (it.village?.name ?: "").contains(searchQuery, ignoreCase = true) ||
                (it.village?.nameHindi ?: "").contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (isHindi) "कुल मरीज: ${patients.size}" else "Total Patients: ${patients.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isHindi) "मरीज खोजें..." else "Search patients...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Rounded.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(filteredPatients) { index, patient ->
                        PatientListItem(
                            patient = patient,
                            index = index,
                            isHindi = isHindi,
                            onClick = { onNavigateToPatient(patient.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListDialog(
    title: String,
    transactions: List<com.villageclinicledger.ui.analytics.viewmodel.TransactionWithPatient>,
    isHindi: Boolean,
    onDismiss: () -> Unit,
    onNavigateToPatient: (Long) -> Unit
) {
    val totalAmount = remember(transactions) { transactions.sumOf { it.amount } }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (isHindi) "कुल: ₹${LocaleManager.formatAmount(totalAmount)} (${transactions.size} लेनदेन)" else "Total: ₹${LocaleManager.formatAmount(totalAmount)} (${transactions.size} Tx)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close"
                            )

                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (transactions.isEmpty()) {
                        item {
                            ClinicEmptyState(
                                message = "No transactions found",
                                messageHindi = "कोई लेनदेन नहीं मिला"
                            )
                        }
                    } else {
                        items(transactions) { item ->
                            ClinicTransactionItem(
                                transaction = com.villageclinicledger.data.models.Transaction(
                                    patientId = item.patientId,
                                    type = item.type,
                                    amount = item.amount,
                                    notes = item.notes
                                ),
                                patientName = item.patientName,
                                villageName = item.villageName,
                                isHindi = isHindi,
                                onClick = { onNavigateToPatient(item.patientId) }
                            )
                        }
                    }
                }
            }
        }
    }
}
