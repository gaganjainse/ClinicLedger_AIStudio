package com.villageclinicledger.ui.compose.tabs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.ui.compose.components.ClinicTransactionItem

@Composable
fun TransactionsTab(
    query: String,
    onQueryChange: (String) -> Unit,
    transactions: List<Transaction>,
    allPatients: List<Patient>,
    onNavigateToDetail: (Long) -> Unit,
    isHindi: Boolean
) {
    val patientMap = remember(allPatients) { allPatients.associateBy { it.id } }
    val filtered = remember(transactions, query) {
        transactions.filter { tx ->
            val pName = patientMap[tx.patientId]?.name ?: ""
            pName.contains(query, ignoreCase = true) || tx.notes.contains(query, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            placeholder = { Text(if (isHindi) "लेनदेन खोजें..." else "Search transactions...") },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.ReceiptLong, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { tx ->
                ClinicTransactionItem(
                    transaction = tx,
                    patientName = patientMap[tx.patientId]?.name ?: "Unknown",
                    onClick = { onNavigateToDetail(tx.patientId) },
                    isHindi = isHindi
                )
            }
        }
    }
}
