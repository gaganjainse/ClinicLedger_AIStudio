package com.villageclinicledger.ui.backup

import com.villageclinicledger.data.models.Alias
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.data.models.Village

/** Data transfer object used by BackupActivity for JSON serialization of
 * the entire database. Contains version and export timestamp metadata
 * alongside the full lists of all four entity types. */
data class BackupData(
    val version: Int = CURRENT_VERSION,
    val exportedAt: String = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).format(java.util.Date()),
    val villages: List<Village>,
    val patients: List<Patient>,
    val aliases: List<Alias>,
    val transactions: List<Transaction>
) {
    companion object {
        const val CURRENT_VERSION = 1
    }
}
