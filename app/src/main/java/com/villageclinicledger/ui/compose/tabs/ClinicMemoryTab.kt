package com.villageclinicledger.ui.compose.tabs

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.villageclinicledger.R
import com.villageclinicledger.data.models.FamilyGroup
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Village
import com.villageclinicledger.data.repository.PatientRepository
import com.villageclinicledger.ui.compose.FamilyTreeDialog
import com.villageclinicledger.ui.compose.components.*
import com.villageclinicledger.ui.util.LocaleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ClinicMemoryTab(
    repository: PatientRepository,
    onNavigateToDetail: (Long) -> Unit,
    villages: List<Village>,
    familyGroups: List<FamilyGroup>,
    villageMap: Map<Long, String>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var allPatients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var selectedVillageId by remember { mutableStateOf<Long?>(null) }
    var showCreateFamilyDialog by remember { mutableStateOf(false) }

    var expandedFamilyGroupId by remember { mutableStateOf<Long?>(null) }
    var activeTreeFamilyGroup by remember { mutableStateOf<FamilyGroup?>(null) }
    var activeTreeMembers by remember { mutableStateOf<List<Patient>>(emptyList()) }

    activeTreeFamilyGroup?.let { family ->
        FamilyTreeDialog(
            familyName = LocaleManager.getLocalizedText(family.name),
            members = activeTreeMembers,
            onDismiss = { activeTreeFamilyGroup = null },
            onNavigateToPatient = onNavigateToDetail
        )
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            allPatients = repository.getAllPatientsSync()
        }
    }

    val refreshPatients = {
        scope.launch(Dispatchers.IO) {
            allPatients = repository.getAllPatientsSync()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FamilyRestroom,
                            contentDescription = "Clinic Memory",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.clinic_memory_os),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.clinic_memory_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.village_networks),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedVillageId == null,
                        onClick = { selectedVillageId = null },
                        label = { Text(stringResource(R.string.all_villages)) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    villages.forEach { village ->
                        val count = allPatients.count { it.villageId == village.id }
                        FilterChip(
                            selected = selectedVillageId == village.id,
                            onClick = { selectedVillageId = village.id },
                            label = { Text("${LocaleManager.getLocalizedVillage(village.name, village.nameHindi)} ($count)") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.family_groups_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )

                TextButton(onClick = { showCreateFamilyDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.new_family_button), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        val filteredFamilies = if (selectedVillageId == null) {
            familyGroups
        } else {
            familyGroups.filter { it.villageId == selectedVillageId }
        }

        if (filteredFamilies.isEmpty()) {
            item {
                ClinicEmptyState(
                    message = stringResource(R.string.no_families_in_village_msg)
                )
            }
        } else {
            items(filteredFamilies, key = { it.id }) { family ->
                val members = allPatients.filter { it.familyGroupId == family.id }
                val isExpanded = expandedFamilyGroupId == family.id
                val totalFamilyBalance = members.sumOf { it.currentBalance }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            expandedFamilyGroupId = if (isExpanded) null else family.id
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = LocaleManager.getLocalizedText(family.name),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    IconButton(
                                        onClick = {
                                            activeTreeFamilyGroup = family
                                            activeTreeMembers = members
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AccountTree,
                                            contentDescription = "Family Tree",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Cabin,
                                        contentDescription = "Village",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = LocaleManager.getLocalizedText(villageMap[family.villageId] ?: "Unknown"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (family.caste.isNotBlank() || family.familyHeadName.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val info = listOfNotNull(
                                        if (family.caste.isNotBlank()) stringResource(R.string.caste_label_prefix, family.caste) else null,
                                        if (family.familyHeadName.isNotBlank()) stringResource(R.string.head_label_prefix, family.familyHeadName) else null
                                    ).joinToString(", ")
                                    Text(
                                        text = info,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(R.string.total_due_sub),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = LocaleManager.formatCurrency(totalFamilyBalance),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = if (totalFamilyBalance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                    contentDescription = "Toggle",
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                                Text(
                                    text = stringResource(R.string.family_members_count_title, members.size),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                if (members.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.no_members_linked_to_family),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    members.forEach { member ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { onNavigateToDetail(member.id) }
                                                .padding(vertical = 10.dp, horizontal = 8.dp)
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Person,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = LocaleManager.formatPatientName(member.name),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    if (member.relationship.isNotBlank()) {
                                                        Text(
                                                            text = stringResource(R.string.relationship_label_prefix, LocaleManager.getLocalizedText(member.relationship)),
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = LocaleManager.formatCurrency(member.currentBalance),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (member.currentBalance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.institutional_insights),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.quick_facts),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    HorizontalDivider()

                    val totalUnlinked = allPatients.count { it.familyGroupId == null }
                    val villageCount = villages.size
                    val familiesCount = familyGroups.size

                    FactRow(stringResource(R.string.total_registered_villages), villageCount.toString())
                    FactRow(stringResource(R.string.total_family_groups), familiesCount.toString())
                    FactRow(stringResource(R.string.unlinked_patients_label), totalUnlinked.toString(), isError = true)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showCreateFamilyDialog) {
        val successMsg = stringResource(R.string.new_family_toast)
        CreateFamilyGroupDialog(
            villages = villages,
            onDismiss = { showCreateFamilyDialog = false },
            onCreate = { name, caste, headName, villageId ->
                scope.launch {
                    withContext(Dispatchers.IO) {
                        repository.insertFamilyGroup(
                            FamilyGroup(
                                name = name,
                                caste = caste,
                                familyHeadName = headName,
                                villageId = villageId
                            )
                        )
                    }
                    showCreateFamilyDialog = false
                    refreshPatients()
                    Toast.makeText(context, successMsg, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@Composable
fun FactRow(label: String, value: String, isError: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}
