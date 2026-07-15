package com.villageclinicledger.ui.compose

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.villageclinicledger.R
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Village
import com.villageclinicledger.data.models.FamilyGroup
import com.villageclinicledger.data.repository.PatientRepository
import com.villageclinicledger.ui.compose.components.ClinicScaffold
import com.villageclinicledger.ui.search.viewmodel.SearchViewModel
import com.villageclinicledger.ui.util.LocaleManager
import com.villageclinicledger.ui.util.LocaleManager.LocalIsHindi
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPatientScreen(
    viewModel: SearchViewModel,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onPatientAdded: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { PatientRepository(context) }
    val villages by viewModel.villages.observeAsState(emptyList())
    val familyGroups by viewModel.familyGroups.observeAsState(emptyList())
    val isHindi = LocalIsHindi.current

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var selectedVillage by remember { mutableStateOf<Village?>(null) }
    var selectedFamilyGroup by remember { mutableStateOf<FamilyGroup?>(null) }
    var relationship by remember { mutableStateOf("Self") }
    var relationshipExpanded by remember { mutableStateOf(false) }
    var relationshipCustom by remember { mutableStateOf("") }

    var expanded by remember { mutableStateOf(false) }
    var familyExpanded by remember { mutableStateOf(false) }

    ClinicScaffold(
        title = stringResource(R.string.add_patient_title),
        onBack = onNavigateBack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.patient_name_label)) },
                placeholder = { Text(stringResource(R.string.patient_name_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(stringResource(R.string.phone_number_optional)) },
                placeholder = { Text("10-digit number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedVillage?.let { LocaleManager.getLocalizedVillage(it.name, it.nameHindi) } ?: stringResource(R.string.select_village_label),
                    onValueChange = {},
                    label = { Text(stringResource(R.string.village_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    if (villages.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No villages found. Add villages first.") },
                            onClick = { expanded = false }
                        )
                    } else {
                        villages.forEach { village ->
                            DropdownMenuItem(
                                text = { Text(LocaleManager.getLocalizedVillage(village.name, village.nameHindi)) },
                                onClick = {
                                    selectedVillage = village
                                    if (selectedFamilyGroup?.villageId != village.id) {
                                        selectedFamilyGroup = null
                                    }
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = familyExpanded,
                onExpandedChange = { familyExpanded = !familyExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = selectedFamilyGroup?.let { LocaleManager.getLocalizedText(it.name) } ?: stringResource(R.string.select_family_group_label),
                    onValueChange = {},
                    label = { Text(stringResource(R.string.family_groups_section_title)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = familyExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = familyExpanded,
                    onDismissRequest = { familyExpanded = false }
                ) {
                    val filteredFamilies = if (selectedVillage == null) {
                        familyGroups
                    } else {
                        familyGroups.filter { it.villageId == selectedVillage?.id }
                    }

                    if (filteredFamilies.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("No family groups available") },
                            onClick = { familyExpanded = false }
                        )
                    } else {
                        filteredFamilies.forEach { group ->
                            val groupVillageName = villages.find { it.id == group.villageId }?.name?.let { " (${LocaleManager.getLocalizedText(it)})" } ?: ""
                            DropdownMenuItem(
                                text = { Text(LocaleManager.getLocalizedText(group.name) + groupVillageName) },
                                onClick = {
                                    selectedFamilyGroup = group
                                    if (selectedVillage?.id != group.villageId) {
                                        selectedVillage = villages.find { it.id == group.villageId }
                                    }
                                    familyExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            val relationshipOptions = listOf(
                "Self" to (if (isHindi) "स्वयं (Self)" else "Self"),
                "Spouse" to (if (isHindi) "पति/पत्नी (Spouse)" else "Spouse"),
                "Son" to (if (isHindi) "बेटा (Son)" else "Son"),
                "Daughter" to (if (isHindi) "बेटी (Daughter)" else "Daughter"),
                "Father" to (if (isHindi) "पिता (Father)" else "Father"),
                "Mother" to (if (isHindi) "माता (Mother)" else "Mother"),
                "Brother" to (if (isHindi) "भाई (Brother)" else "Brother"),
                "Sister" to (if (isHindi) "बहन (Sister)" else "Sister"),
                "Daughter-In-Law" to (if (isHindi) "बहू (Daughter-In-Law)" else "Daughter-In-Law"),
                "Son-In-Law" to (if (isHindi) "दामाद (Son-In-Law)" else "Son-In-Law"),
                "Grandson" to (if (isHindi) "पोता/नाती (Grandson)" else "Grandson"),
                "Granddaughter" to (if (isHindi) "पोती/नातिन (Granddaughter)" else "Granddaughter"),
                "Nephew" to (if (isHindi) "भतीजा/भांजा (Nephew)" else "Nephew"),
                "Niece" to (if (isHindi) "भतीजी/भांजी (Niece)" else "Niece"),
                "Uncle" to (if (isHindi) "चाचा/ताऊ/मामा (Uncle)" else "Uncle"),
                "Aunt" to (if (isHindi) "चाची/ताई/मामी (Aunt)" else "Aunt"),
                "Other" to (if (isHindi) "अन्य (Other)" else "Other")
            )

            ExposedDropdownMenuBox(
                expanded = relationshipExpanded,
                onExpandedChange = { relationshipExpanded = !relationshipExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val currentDisplayName = relationshipOptions.find { it.first == relationship }?.second 
                    ?: (if (relationship.isNotBlank()) relationship else (if (isHindi) "स्वयं (Self)" else "Self"))

                OutlinedTextField(
                    readOnly = true,
                    value = currentDisplayName,
                    onValueChange = {},
                    label = { Text(stringResource(R.string.relationship_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = relationshipExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = relationshipExpanded,
                    onDismissRequest = { relationshipExpanded = false }
                ) {
                    relationshipOptions.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt.second) },
                            onClick = {
                                relationship = opt.first
                                relationshipExpanded = false
                            }
                        )
                    }
                }
            }

            if (relationship == "Other") {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = relationshipCustom,
                    onValueChange = { relationshipCustom = it },
                    label = { Text(if (isHindi) "कस्टम संबंध प्रविष्टि" else "Custom Relationship") },
                    placeholder = { Text(if (isHindi) "संबंध लिखें (जैसे: भतीजी आदि)" else "Enter relationship (e.g., Nephew, etc.)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (name.isNotBlank() && selectedVillage != null) {
                        scope.launch {
                            try {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    val dbRel = if (relationship == "Other") relationshipCustom.trim() else relationship.trim()
                                    repository.insertPatient(
                                        Patient(
                                            name = LocaleManager.formatPatientName(name.trim()),
                                            phone = phone.trim(),
                                            villageId = selectedVillage!!.id,
                                            familyGroupId = selectedFamilyGroup?.id,
                                            relationship = dbRel
                                        )
                                    )
                                }
                                Toast.makeText(context, context.getString(R.string.patient_added), Toast.LENGTH_SHORT).show()
                                onPatientAdded()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                enabled = name.isNotBlank() && selectedVillage != null,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    text = stringResource(R.string.add),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
