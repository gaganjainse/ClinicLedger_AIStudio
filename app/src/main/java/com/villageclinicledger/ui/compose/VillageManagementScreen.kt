package com.villageclinicledger.ui.compose

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.villageclinicledger.R
import com.villageclinicledger.data.models.FamilyGroup
import com.villageclinicledger.data.models.Village
import com.villageclinicledger.data.repository.PatientRepository
import com.villageclinicledger.ui.compose.components.*
import com.villageclinicledger.ui.util.LocaleManager
import com.villageclinicledger.ui.util.LocaleManager.LocalIsHindi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VillageManagementScreen(
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    VillageManagementContent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VillageManagementContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { PatientRepository(context) }
    val isHindi = LocalIsHindi.current

    val villages by repository.getAllVillages().observeAsState(emptyList())
    val familyGroups by repository.getAllFamilyGroups().observeAsState(emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(stringResource(R.string.tab_villages), stringResource(R.string.tab_family_groups))

    var inputName by remember { mutableStateOf("") }
    var editingVillage by remember { mutableStateOf<Village?>(null) }
    var editingFamily by remember { mutableStateOf<FamilyGroup?>(null) }
    var villageToDelete by remember { mutableStateOf<Village?>(null) }
    var familyToDelete by remember { mutableStateOf<FamilyGroup?>(null) }
    var showAddFamilyDialog by remember { mutableStateOf(false) }

    val addedSuccessMsg = stringResource(R.string.added_successfully_toast)
    val villageDeletedMsg = stringResource(R.string.village_deleted_toast)
    val familyDeletedMsg = stringResource(R.string.family_deleted_toast)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            edgePadding = 16.dp
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = {
                        selectedTab = index
                        inputName = ""
                    },
                    text = { Text(title, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (selectedTab == 0) {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    placeholder = { Text(stringResource(R.string.new_village_placeholder)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = {
                        if (inputName.isNotBlank()) {
                            scope.launch {
                                withContext(Dispatchers.IO) {
                                    repository.insertVillage(Village(name = inputName.trim()))
                                }
                                Toast.makeText(context, addedSuccessMsg, Toast.LENGTH_SHORT).show()
                                inputName = ""
                            }
                        }
                    },
                    enabled = inputName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(54.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.add), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { showAddFamilyDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Family")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.new_family_placeholder), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (selectedTab == 0) {
                if (villages.isEmpty()) {
                    item {
                        ClinicEmptyState(message = stringResource(R.string.no_villages_msg))
                    }
                } else {
                    items(villages, key = { it.id }) { village ->
                        EntityCard(
                            name = LocaleManager.getLocalizedVillage(village.name, village.nameHindi),
                            icon = Icons.Rounded.Cabin,
                            onEdit = { editingVillage = village },
                            onDelete = { villageToDelete = village }
                        )
                    }
                }
            } else {
                if (familyGroups.isEmpty()) {
                    item {
                        ClinicEmptyState(message = stringResource(R.string.no_families_msg))
                    }
                } else {
                    items(familyGroups, key = { it.id }) { group ->
                        EntityCard(
                            name = LocaleManager.getLocalizedText(group.name),
                            icon = Icons.Rounded.Group,
                            onEdit = { editingFamily = group },
                            onDelete = { familyToDelete = group }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    if (editingVillage != null) {
        var tempName by remember { mutableStateOf(LocaleManager.getLocalizedVillage(editingVillage!!.name, editingVillage!!.nameHindi)) }
        AlertDialog(
            onDismissRequest = { editingVillage = null },
            title = { Text(stringResource(R.string.edit_village_header), fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text(stringResource(R.string.village_name_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                val updatedVillage = if (isHindi) {
                                    editingVillage!!.copy(nameHindi = tempName.trim())
                                } else {
                                    editingVillage!!.copy(name = tempName.trim())
                                }
                                repository.updateVillage(updatedVillage)
                            }
                            editingVillage = null
                        }
                    },
                    enabled = tempName.isNotBlank()
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingVillage = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (editingFamily != null) {
        var tempName by remember { mutableStateOf(editingFamily!!.name) }
        var tempCaste by remember { mutableStateOf(editingFamily!!.caste) }
        var tempHeadName by remember { mutableStateOf(editingFamily!!.familyHeadName) }
        var selectedVillageForFamily by remember { mutableStateOf(villages.find { it.id == editingFamily!!.villageId }) }
        var villageDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { editingFamily = null },
            title = { Text(stringResource(R.string.edit_family_header), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text(stringResource(R.string.family_name_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempCaste,
                        onValueChange = { tempCaste = it },
                        label = { Text("Caste / Category") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempHeadName,
                        onValueChange = { tempHeadName = it },
                        label = { Text("Family Head Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = villageDropdownExpanded,
                        onExpandedChange = { villageDropdownExpanded = !villageDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = selectedVillageForFamily?.let { LocaleManager.getLocalizedVillage(it.name, it.nameHindi) } ?: "Select Village",
                            onValueChange = {},
                            label = { Text("Village") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = villageDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = villageDropdownExpanded,
                            onDismissRequest = { villageDropdownExpanded = false }
                        ) {
                            villages.forEach { village ->
                                DropdownMenuItem(
                                    text = { Text(LocaleManager.getLocalizedVillage(village.name, village.nameHindi)) },
                                    onClick = {
                                        selectedVillageForFamily = village
                                        villageDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                repository.updateFamilyGroup(
                                    editingFamily!!.copy(
                                        name = tempName.trim(),
                                        caste = tempCaste.trim(),
                                        familyHeadName = tempHeadName.trim(),
                                        villageId = selectedVillageForFamily?.id ?: editingFamily!!.villageId
                                    )
                                )
                            }
                            editingFamily = null
                        }
                    },
                    enabled = tempName.isNotBlank() && selectedVillageForFamily != null
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { editingFamily = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAddFamilyDialog) {
        var tempName by remember { mutableStateOf("") }
        var tempCaste by remember { mutableStateOf("") }
        var tempHeadName by remember { mutableStateOf("") }
        var selectedVillageForFamily by remember { mutableStateOf(villages.firstOrNull()) }
        var villageDropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddFamilyDialog = false },
            title = { Text("Add Family Group", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text(stringResource(R.string.family_name_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempCaste,
                        onValueChange = { tempCaste = it },
                        label = { Text("Caste / Category") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempHeadName,
                        onValueChange = { tempHeadName = it },
                        label = { Text("Family Head Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = villageDropdownExpanded,
                        onExpandedChange = { villageDropdownExpanded = !villageDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = selectedVillageForFamily?.let { LocaleManager.getLocalizedVillage(it.name, it.nameHindi) } ?: "Select Village",
                            onValueChange = {},
                            label = { Text("Village") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = villageDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = villageDropdownExpanded,
                            onDismissRequest = { villageDropdownExpanded = false }
                        ) {
                            villages.forEach { village ->
                                DropdownMenuItem(
                                    text = { Text(LocaleManager.getLocalizedVillage(village.name, village.nameHindi)) },
                                    onClick = {
                                        selectedVillageForFamily = village
                                        villageDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                repository.insertFamilyGroup(
                                    FamilyGroup(
                                        name = tempName.trim(),
                                        caste = tempCaste.trim(),
                                        familyHeadName = tempHeadName.trim(),
                                        villageId = selectedVillageForFamily?.id ?: 1L
                                    )
                                )
                            }
                            showAddFamilyDialog = false
                        }
                    },
                    enabled = tempName.isNotBlank() && selectedVillageForFamily != null
                ) {
                    Text(stringResource(R.string.add))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFamilyDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (villageToDelete != null) {
        AlertDialog(
            onDismissRequest = { villageToDelete = null },
            title = { Text(stringResource(R.string.delete_village_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(stringResource(R.string.delete_village_confirm, LocaleManager.getLocalizedVillage(villageToDelete!!.name, villageToDelete!!.nameHindi)))
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    repository.deleteVillage(villageToDelete!!)
                                }
                                Toast.makeText(context, villageDeletedMsg, Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                            villageToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { villageToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (familyToDelete != null) {
        AlertDialog(
            onDismissRequest = { familyToDelete = null },
            title = { Text(stringResource(R.string.delete_family_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(stringResource(R.string.delete_family_confirm, LocaleManager.getLocalizedText(familyToDelete!!.name)))
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    repository.deleteFamilyGroup(familyToDelete!!)
                                }
                                Toast.makeText(context, familyDeletedMsg, Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                            familyToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { familyToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
