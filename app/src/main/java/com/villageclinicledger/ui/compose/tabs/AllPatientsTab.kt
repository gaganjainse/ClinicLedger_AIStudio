package com.villageclinicledger.ui.compose.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.ui.compose.components.PatientListItem

@Composable
fun AllPatientsTab(
    allPatients: List<Patient>,
    onNavigateToDetail: (Long) -> Unit,
    isHindi: Boolean
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(allPatients, query) {
        allPatients.filter { 
            it.name.contains(query, ignoreCase = true) || 
            (it.phone ?: "").contains(query) 
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            placeholder = { Text(if (isHindi) "सभी मरीजों में खोजें..." else "Search all patients...") },
            leadingIcon = { Icon(Icons.Rounded.People, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { patient ->
                PatientListItem(
                    patient = patient, 
                    isHindi = isHindi, 
                    onClick = { onNavigateToDetail(patient.id) }
                )
            }
        }
    }
}
