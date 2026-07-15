package com.villageclinicledger.ui.compose

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.villageclinicledger.R
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.repository.PatientRepository
import com.villageclinicledger.ui.search.viewmodel.SearchViewModel
import com.villageclinicledger.ui.analytics.viewmodel.AnalyticsViewModel
import com.villageclinicledger.ui.compose.components.*
import com.villageclinicledger.ui.compose.tabs.*
import com.villageclinicledger.ui.util.LocaleManager
import com.villageclinicledger.ui.util.LocaleManager.LocalIsHindi
import kotlinx.coroutines.launch

enum class DrawerItem {
    LEDGER, ALL_PATIENTS, CLINIC_MEMORY, TRANSACTIONS, ANALYTICS, VILLAGES_FAMILIES, BACKUP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    analyticsViewModel: AnalyticsViewModel,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAddPatient: () -> Unit,
    onOpenVoiceSheet: () -> Unit,
    onToggleLanguage: () -> Unit
) {
    val isHindi = LocalIsHindi.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    
    val allPatients by viewModel.allPatients.observeAsState(emptyList())
    val allTransactions by viewModel.allTransactions.observeAsState(emptyList())
    val searchResults by viewModel.searchResults.observeAsState(emptyList())
    val recentPatients by viewModel.recentPatients.observeAsState(emptyList())
    val villages by viewModel.villages.observeAsState(emptyList())
    val familyGroups by viewModel.familyGroups.observeAsState(emptyList())
    val totalCollectedToday by viewModel.totalCollectedToday.observeAsState(0.0)
    val isLoading by viewModel.isLoading.observeAsState(false)

    var searchQuery by remember { mutableStateOf("") }
    var transactionsQuery by remember { mutableStateOf("") }
    var selectedDrawerItem by remember { mutableStateOf(DrawerItem.LEDGER) }

    val villageMap = remember(villages) {
        villages.associate { it.id to LocaleManager.getLocalizedVillage(it.name, it.nameHindi) }
    }

    BackHandler(enabled = drawerState.isOpen || selectedDrawerItem != DrawerItem.LEDGER) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            selectedDrawerItem = DrawerItem.LEDGER
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ClinicDrawerContent(
                selectedItem = selectedDrawerItem,
                onItemSelected = {
                    selectedDrawerItem = it
                    scope.launch { drawerState.close() }
                },
                onToggleLanguage = onToggleLanguage
            )
        }
    ) {
        Scaffold(
            topBar = {
                ClinicTopAppBar(
                    title = when (selectedDrawerItem) {
                        DrawerItem.LEDGER -> "Clinic Ledger"
                        DrawerItem.ALL_PATIENTS -> "All Patients"
                        DrawerItem.CLINIC_MEMORY -> "Clinic Memory"
                        DrawerItem.TRANSACTIONS -> "Transactions"
                        DrawerItem.ANALYTICS -> "Analytics"
                        DrawerItem.VILLAGES_FAMILIES -> "Villages & Families"
                        DrawerItem.BACKUP -> "Backup & Restore"
                    },
                    titleHindi = when (selectedDrawerItem) {
                        DrawerItem.LEDGER -> "क्लिनिक लेजर"
                        DrawerItem.ALL_PATIENTS -> "सभी मरीज"
                        DrawerItem.CLINIC_MEMORY -> "क्लिनिक मेमोरी"
                        DrawerItem.TRANSACTIONS -> "लेनदेन"
                        DrawerItem.ANALYTICS -> "विश्लेषण"
                        DrawerItem.VILLAGES_FAMILIES -> "गाँव और परिवार"
                        DrawerItem.BACKUP -> "बैकअप और रीस्टोर"
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (selectedDrawerItem == DrawerItem.LEDGER || selectedDrawerItem == DrawerItem.ALL_PATIENTS) {
                            IconButton(onClick = onOpenVoiceSheet) {
                                Icon(Icons.Rounded.Mic, contentDescription = "Voice Input", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                if (selectedDrawerItem == DrawerItem.LEDGER || selectedDrawerItem == DrawerItem.ALL_PATIENTS) {
                    ExtendedFloatingActionButton(
                        onClick = onNavigateToAddPatient,
                        icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        text = { Text(if (isHindi) "मरीज जोड़ें" else "Add Patient") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (selectedDrawerItem) {
                    DrawerItem.LEDGER -> LedgerTab(
                        searchQuery = searchQuery,
                        onSearchChange = { 
                            searchQuery = it
                            viewModel.searchPatients(it)
                        },
                        recentPatients = recentPatients,
                        searchResults = searchResults,
                        totalCollectedToday = totalCollectedToday,
                        allPatients = allPatients,
                        familyGroups = familyGroups,
                        onNavigateToDetail = onNavigateToDetail,
                        isHindi = isHindi
                    )
                    DrawerItem.ALL_PATIENTS -> AllPatientsTab(
                        allPatients = allPatients,
                        onNavigateToDetail = onNavigateToDetail,
                        isHindi = isHindi
                    )
                    DrawerItem.CLINIC_MEMORY -> ClinicMemoryTab(
                        repository = remember { PatientRepository(context) },
                        onNavigateToDetail = onNavigateToDetail,
                        villages = villages,
                        familyGroups = familyGroups,
                        villageMap = villageMap
                    )
                    DrawerItem.TRANSACTIONS -> TransactionsTab(
                        query = transactionsQuery,
                        onQueryChange = { transactionsQuery = it },
                        transactions = allTransactions,
                        allPatients = allPatients,
                        onNavigateToDetail = onNavigateToDetail,
                        isHindi = isHindi
                    )
                    DrawerItem.ANALYTICS -> AnalyticsContent(
                        viewModel = analyticsViewModel,
                        onNavigateToPatientDetail = onNavigateToDetail
                    )
                    DrawerItem.VILLAGES_FAMILIES -> VillageManagementContent()
                    DrawerItem.BACKUP -> BackupTab()
                }
                
                if (isLoading) ClinicLoadingState()
            }
        }
    }
}

@Composable
fun LedgerTab(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    recentPatients: List<Patient>,
    searchResults: List<Patient>,
    totalCollectedToday: Double,
    allPatients: List<Patient>,
    familyGroups: List<com.villageclinicledger.data.models.FamilyGroup>,
    onNavigateToDetail: (Long) -> Unit,
    isHindi: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            MorningBriefSection(
                allPatients = allPatients,
                familyGroups = familyGroups,
                totalCollectedToday = totalCollectedToday
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(if (isHindi) "मरीज का नाम या मोबाइल..." else "Search name or phone...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Clear")
                        }
                    }
                },
                shape = RoundedCornerShape(28.dp),
                singleLine = true
            )
        }

        if (searchQuery.isEmpty()) {
            item {
                Text(
                    text = if (isHindi) "हाल के मरीज" else "Recent Patients",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(recentPatients) { patient ->
                PatientListItem(patient = patient, isHindi = isHindi, onClick = { onNavigateToDetail(patient.id) })
            }
        } else {
            if (searchResults.isEmpty()) {
                item {
                    ClinicEmptyState(message = "No patients found", messageHindi = "कोई मरीज नहीं मिला")
                }
            } else {
                items(searchResults) { patient ->
                    PatientListItem(patient = patient, isHindi = isHindi, onClick = { onNavigateToDetail(patient.id) })
                }
            }
        }
        
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun ClinicDrawerContent(
    selectedItem: DrawerItem,
    onItemSelected: (DrawerItem) -> Unit,
    onToggleLanguage: () -> Unit
) {
    val isHindi = LocalIsHindi.current
    ModalDrawerSheet {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Clinic Ledger",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider()
        
        DrawerNavItem(DrawerItem.LEDGER, Icons.Rounded.Dashboard, if (isHindi) "होम लेजर" else "Ledger Home", selectedItem, onItemSelected)
        DrawerNavItem(DrawerItem.ALL_PATIENTS, Icons.Rounded.People, if (isHindi) "मरीजों की सूची" else "Patient List", selectedItem, onItemSelected)
        DrawerNavItem(DrawerItem.CLINIC_MEMORY, Icons.Rounded.Psychology, if (isHindi) "क्लिनिक मेमोरी" else "Clinic Memory", selectedItem, onItemSelected)
        DrawerNavItem(DrawerItem.TRANSACTIONS, Icons.AutoMirrored.Rounded.ReceiptLong, if (isHindi) "लेनदेन लॉग" else "Transaction Logs", selectedItem, onItemSelected)
        DrawerNavItem(DrawerItem.ANALYTICS, Icons.Rounded.Analytics, if (isHindi) "विश्लेषण" else "Analytics", selectedItem, onItemSelected)
        DrawerNavItem(DrawerItem.VILLAGES_FAMILIES, Icons.Rounded.Cabin, if (isHindi) "गाँव और परिवार" else "Villages & Families", selectedItem, onItemSelected)
        DrawerNavItem(DrawerItem.BACKUP, Icons.Rounded.Backup, if (isHindi) "बैकअप" else "Backup", selectedItem, onItemSelected)
        
        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider()
        NavigationDrawerItem(
            label = { Text(if (isHindi) "English में बदलें" else "Switch to Hindi") },
            selected = false,
            onClick = onToggleLanguage,
            icon = { Icon(Icons.Rounded.Language, contentDescription = null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DrawerNavItem(
    item: DrawerItem,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selectedItem: DrawerItem,
    onItemSelected: (DrawerItem) -> Unit
) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = selectedItem == item,
        onClick = { onItemSelected(item) },
        icon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}
