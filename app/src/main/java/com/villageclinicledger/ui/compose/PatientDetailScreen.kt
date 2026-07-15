package com.villageclinicledger.ui.compose

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.villageclinicledger.R
import com.villageclinicledger.data.models.Alias
import com.villageclinicledger.data.models.FamilyGroup
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.data.models.Village
import com.villageclinicledger.data.repository.PatientRepository
import com.villageclinicledger.ui.patientdetail.viewmodel.PatientDetailViewModel
import com.villageclinicledger.ui.compose.components.*
import com.villageclinicledger.ui.util.LocaleManager
import com.villageclinicledger.ui.util.LocaleManager.LocalIsHindi
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatientDetailScreen(
    patientId: Long,
    viewModel: PatientDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateToPatientDetail: (Long) -> Unit
) {
    val isHindi = LocalIsHindi.current
    val context = LocalContext.current
    val repository = remember { PatientRepository(context) }

    LaunchedEffect(patientId) {
        viewModel.loadPatient(patientId)
    }

    val patient by viewModel.patient.observeAsState(null)
    val aliases by viewModel.aliases.observeAsState(emptyList())
    val transactions by viewModel.transactions.observeAsState(emptyList())
    val villages by viewModel.villages.observeAsState(emptyList())
    val allFamilyGroups by repository.getAllFamilyGroups().observeAsState(emptyList())

    LaunchedEffect(patient) {
        patient?.familyGroupId?.let {
            viewModel.setFamilyGroupId(it)
        } ?: viewModel.setFamilyGroupId(null)
    }

    val familyGroup by viewModel.familyGroup.observeAsState(null)
    val familyMembers by viewModel.familyMembers.observeAsState(emptyList())

    var showEditPatientDialog by remember { mutableStateOf(false) }
    var showAddAliasDialog by remember { mutableStateOf(false) }
    var showLinkFamilyDialog by remember { mutableStateOf(false) }
    var showAddTransactionDialog by remember { mutableStateOf<String?>(null) } 
    var showFamilyTreeDialog by remember { mutableStateOf(false) }

    if (showFamilyTreeDialog && familyGroup != null) {
        FamilyTreeDialog(
            familyName = LocaleManager.getLocalizedText(familyGroup!!.name),
            members = familyMembers,
            onDismiss = { showFamilyTreeDialog = false },
            onNavigateToPatient = { pId ->
                showFamilyTreeDialog = false
                onNavigateToPatientDetail(pId)
            }
        )
    }

    val villageName = remember(patient, villages) {
        villages.find { it.id == patient?.villageId }?.name ?: "Unknown"
    }

    ClinicScaffold(
        title = if (patient != null) LocaleManager.formatPatientName(patient?.name) else stringResource(R.string.patient_details_title),
        onBack = onNavigateBack,
        actions = {
            IconButton(onClick = { showEditPatientDialog = true }) {
                Icon(imageVector = Icons.Rounded.Edit, contentDescription = "Edit Profile")
            }
            IconButton(onClick = { showLinkFamilyDialog = true }) {
                Icon(imageVector = Icons.Rounded.GroupAdd, contentDescription = "Link Family")
            }
        }
    ) { paddingValues ->
        if (patient == null) {
            ClinicLoadingState()
        } else {
            val currPatient = patient!!
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    item {
                        PatientDetailHeader(
                            patient = currPatient,
                            villageName = villageName,
                            aliases = aliases,
                            onAddAlias = { showAddAliasDialog = true },
                            onDeleteAlias = { viewModel.deleteAlias(it) },
                            onShowFamilyTree = { showFamilyTreeDialog = true },
                            hasFamily = familyGroup != null
                        )
                    }

                    item {
                        BalanceCard(balance = currPatient.currentBalance)
                    }

                    if (familyGroup != null) {
                        item {
                            FamilyGroupSection(
                                familyGroup = familyGroup!!,
                                members = familyMembers.filter { it.id != currPatient.id },
                                isHindi = isHindi,
                                onNavigateToMember = onNavigateToPatientDetail
                            )
                        }
                    }

                    item {
                        TransactionHistoryHeader()
                    }

                    if (transactions.isEmpty()) {
                        item {
                            EmptyTransactionsView(isHindi)
                        }
                    } else {
                        items(transactions.sortedByDescending { it.createdAt }) { tx ->
                            TransactionTimelineItem(transaction = tx)
                        }
                    }
                }

                TransactionActionButtons(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    onAction = { type -> showAddTransactionDialog = type }
                )
            }
        }
    }

    if (showEditPatientDialog && patient != null) {
        AddPatientDialog(
            villages = villages,
            onDismiss = { showEditPatientDialog = false },
            onAddPatient = { updated ->
                viewModel.updatePatient(updated)
                showEditPatientDialog = false
                Toast.makeText(context, context.getString(R.string.profile_updated_toast), Toast.LENGTH_SHORT).show()
            },
            existingPatient = patient
        )
    }

    if (showAddAliasDialog) {
        AddAliasDialog(
            onDismiss = { showAddAliasDialog = false },
            onConfirm = { name ->
                viewModel.addAlias(patientId, name)
                showAddAliasDialog = false
            }
        )
    }

    if (showLinkFamilyDialog) {
        LinkFamilyDialog(
            familyGroups = allFamilyGroups,
            currentFamilyId = patient?.familyGroupId,
            villages = villages,
            onDismiss = { showLinkFamilyDialog = false },
            onLink = { familyId ->
                viewModel.linkFamilyGroup(patientId, familyId)
                showLinkFamilyDialog = false
            },
            onCreateNew = { name, villageId ->
                viewModel.createAndLinkFamilyGroup(patientId, name, villageId)
                showLinkFamilyDialog = false
            }
        )
    }

    showAddTransactionDialog?.let { type ->
        AddTransactionDialog(
            type = type,
            isHindi = isHindi,
            onDismiss = { showAddTransactionDialog = null },
            onConfirm = { amount, notes, date ->
                viewModel.addTransaction(patientId, type, amount, notes, date)
                showAddTransactionDialog = null
            }
        )
    }
}

@Composable
fun PatientDetailHeader(
    patient: Patient,
    villageName: String,
    aliases: List<Alias>,
    onAddAlias: () -> Unit,
    onDeleteAlias: (Alias) -> Unit,
    onShowFamilyTree: () -> Unit,
    hasFamily: Boolean
) {
    val isHindi = LocalIsHindi.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                val initials = patient.name.split(" ").filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1).uppercase() }
                Text(
                    text = if (initials.isNotBlank()) initials else "?",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = LocaleManager.formatPatientName(patient.name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Cabin,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = villageName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!patient.phone.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = patient.phone!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                aliases.forEach { alias ->
                    AssistChip(
                        onClick = { },
                        label = { Text(alias.alias) },
                        trailingIcon = {
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Remove",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onDeleteAlias(alias) }
                            )
                        },
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
                IconButton(onClick = onAddAlias, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Add Alias", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (hasFamily) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onShowFamilyTree,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.AccountTree, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = if (isHindi) "वंशावली देखें" else "View Family Tree", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
fun BalanceCard(balance: Double) {
    val isHindi = LocalIsHindi.current
    val isDebt = balance > 0
    val cardColor = if (isDebt) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
    val contentColor = if (isDebt) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isHindi) "वर्तमान स्थिति" else "Current Balance",
                style = MaterialTheme.typography.labelLarge,
                color = contentColor.copy(alpha = 0.8f)
            )
            Text(
                text = LocaleManager.formatCurrency(balance),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = contentColor
            )
            Text(
                text = if (isDebt) (if (isHindi) "बकाया राशि" else "Outstanding Due") else (if (isHindi) "खाता बराबर है" else "Balanced / Advance"),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun FamilyGroupSection(
    familyGroup: FamilyGroup,
    members: List<Patient>,
    isHindi: Boolean,
    onNavigateToMember: (Long) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = if (isHindi) "परिवार के सदस्य" else "Family Members",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                members.forEach { member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToMember(member.id) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = member.name.take(1).uppercase(),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = LocaleManager.formatPatientName(member.name), style = MaterialTheme.typography.bodyLarge)
                            if (member.relationship.isNotBlank()) {
                                Text(text = member.relationship, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            text = LocaleManager.formatCurrency(member.currentBalance),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (member.currentBalance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                    if (member != members.last()) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp).alpha(0.5f))
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionHistoryHeader() {
    val isHindi = LocalIsHindi.current
    Text(
        text = if (isHindi) "लेनदेन का इतिहास" else "Transaction History",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun EmptyTransactionsView(isHindi: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isHindi) "अभी तक कोई लेनदेन नहीं हुआ है।" else "No transactions recorded yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun TransactionTimelineItem(transaction: Transaction) {
    val isHindi = LocalIsHindi.current
    val isPayment = transaction.type == "payment"
    val accentColor = if (isPayment) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val icon = if (isPayment) Icons.Rounded.Payments else Icons.Rounded.LocalPharmacy
    val prefix = if (isPayment) "-" else "+"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(accentColor)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(60.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val formattedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(transaction.createdAt)
                    Text(
                        text = LocaleManager.getLocalizedTransactionType(transaction.type, isHindi),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    if (transaction.notes.isNotBlank()) {
                        Text(
                            text = transaction.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = "$prefix${LocaleManager.formatCurrency(kotlin.math.abs(transaction.amount))}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = accentColor
                )
            }
        }
    }
}

@Composable
fun TransactionActionButtons(
    modifier: Modifier = Modifier,
    onAction: (String) -> Unit
) {
    val isHindi = LocalIsHindi.current
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(32.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExtendedFloatingActionButton(
            onClick = { onAction("medicine") },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(if (isHindi) "दवाई" else "Medicine") },
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
            modifier = Modifier.height(48.dp)
        )
        ExtendedFloatingActionButton(
            onClick = { onAction("payment") },
            icon = { Icon(Icons.Rounded.ArrowDownward, contentDescription = null) },
            text = { Text(if (isHindi) "जमा" else "Payment") },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.height(48.dp)
        )
        IconButton(
            onClick = { onAction("adjustment") },
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
        ) {
            Icon(Icons.Rounded.Settings, contentDescription = "Adjust", tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAliasDialog(onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val isHindi = LocalIsHindi.current
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isHindi) "उपनाम जोड़ें" else "Add Alias") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(if (isHindi) "नाम" else "Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name) }) {
                Text(if (isHindi) "जोड़ें" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isHindi) "रद्द करें" else "Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkFamilyDialog(
    familyGroups: List<FamilyGroup>,
    currentFamilyId: Long?,
    villages: List<Village>,
    onDismiss: () -> Unit,
    onLink: (Long?) -> Unit,
    onCreateNew: (String, Long) -> Unit
) {
    val isHindi = LocalIsHindi.current
    var showCreateNew by remember { mutableStateOf(false) }
    var newFamilyName by remember { mutableStateOf("") }
    var selectedVillageId by remember { mutableStateOf(villages.firstOrNull()?.id ?: 0L) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isHindi) "परिवार से जोड़ें" else "Link to Family") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                if (showCreateNew) {
                    OutlinedTextField(
                        value = newFamilyName,
                        onValueChange = { newFamilyName = it },
                        label = { Text(if (isHindi) "नया परिवार नाम" else "New Family Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (isHindi) "गाँव चुनें" else "Select Village")
                    villages.forEach { v ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedVillageId = v.id }) {
                            RadioButton(selected = selectedVillageId == v.id, onClick = { selectedVillageId = v.id })
                            Text(LocaleManager.getLocalizedVillage(v.name, v.nameHindi))
                        }
                    }
                } else {
                    TextButton(onClick = { showCreateNew = true }) {
                        Text(if (isHindi) "+ नया परिवार बनाएँ" else "+ Create New Family")
                    }
                    if (currentFamilyId != null) {
                        TextButton(onClick = { onLink(null) }) {
                            Text(if (isHindi) "परिवार से हटाएँ" else "Unlink from Family", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    LazyColumn {
                        items(familyGroups) { group ->
                            val isSelected = group.id == currentFamilyId
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { onLink(group.id) },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(group.name, modifier = Modifier.padding(12.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showCreateNew) {
                Button(onClick = { if (newFamilyName.isNotBlank()) onCreateNew(newFamilyName, selectedVillageId) }) {
                    Text(if (isHindi) "बनाएँ और जोड़ें" else "Create & Link")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = if (showCreateNew) { { showCreateNew = false } } else onDismiss) {
                Text(if (isHindi) "रद्द करें" else "Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    type: String,
    isHindi: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double, String, Date) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    val title = when (type) {
        "medicine" -> if (isHindi) "दवाई जोड़ें" else "Add Medicine"
        "payment" -> if (isHindi) "जमा जोड़ें" else "Add Payment"
        else -> if (isHindi) "समायोजन" else "Adjustment"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text(if (isHindi) "राशि (₹)" else "Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(if (isHindi) "विवरण" else "Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val amt = amount.toDoubleOrNull() ?: 0.0
                if (amt > 0 || type == "adjustment") onConfirm(amt, notes, Date())
            }) {
                Text(if (isHindi) "पुष्टि करें" else "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isHindi) "रद्द करें" else "Cancel")
            }
        }
    )
}
