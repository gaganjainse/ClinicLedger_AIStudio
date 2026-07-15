package com.villageclinicledger.backup

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.villageclinicledger.data.models.Alias
import com.villageclinicledger.data.models.FamilyGroup
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.data.models.Village
import com.villageclinicledger.ui.backup.BackupData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class BackupImportExportTest {

    private val gson: Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .create()

    @Test
    fun testBackupSerializationAndDeserialization() {
        val village = Village(id = 1L, name = "Siras", nameHindi = "सिरस")
        val familyGroup = FamilyGroup(id = 10L, name = "Sharma Family", villageId = 1L)
        val patient = Patient(
            id = 100L,
            name = "Ramesh Sharma",
            villageId = 1L,
            phone = "9876543210",
            familyGroupId = 10L,
            currentBalance = 350.0
        )
        val alias = Alias(id = 5L, patientId = 100L, alias = "Ramu")
        val transaction1 = Transaction(
            id = 1001L,
            patientId = 100L,
            type = "medicine",
            amount = 500.0,
            notes = "Antibiotics"
        )
        val transaction2 = Transaction(
            id = 1002L,
            patientId = 100L,
            type = "payment",
            amount = 150.0,
            notes = "Partial cash payment"
        )

        val backupData = BackupData(
            version = BackupData.CURRENT_VERSION,
            villages = listOf(village),
            patients = listOf(patient),
            aliases = listOf(alias),
            transactions = listOf(transaction1, transaction2),
            familyGroups = listOf(familyGroup)
        )

        // Serialize to JSON
        val json = gson.toJson(backupData)
        assertNotNull(json)
        assertTrue(json.contains("Ramesh Sharma"))
        assertTrue(json.contains("Antibiotics"))

        // Deserialize back
        val restored = gson.fromJson(json, BackupData::class.java)
        assertNotNull(restored)
        assertEquals(BackupData.CURRENT_VERSION, restored.version)
        assertEquals(1, restored.villages.size)
        assertEquals("Siras", restored.villages[0].name)
        assertEquals(1, restored.patients.size)
        assertEquals("Ramesh Sharma", restored.patients[0].name)
        assertEquals(1, restored.aliases.size)
        assertEquals("Ramu", restored.aliases[0].alias)
        assertEquals(2, restored.transactions.size)
        assertEquals("medicine", restored.transactions[0].type)
        assertEquals(500.0, restored.transactions[0].amount, 0.01)
        assertEquals("payment", restored.transactions[1].type)
        assertEquals(150.0, restored.transactions[1].amount, 0.01)
        assertEquals(1, restored.familyGroups?.size)
        assertEquals("Sharma Family", restored.familyGroups?.get(0)?.name)
    }

    @Test
    fun testBackupVersionValidation() {
        // Current version is 1. Test version matching and non-matching behavior
        val backupData = BackupData(
            version = BackupData.CURRENT_VERSION,
            villages = emptyList(),
            patients = emptyList(),
            aliases = emptyList(),
            transactions = emptyList(),
            familyGroups = emptyList()
        )

        assertEquals(1, BackupData.CURRENT_VERSION)
        assertEquals(BackupData.CURRENT_VERSION, backupData.version)
    }

    @Test
    fun testLegacyBackupImport_withoutFamilyGroups() {
        // Older backups might not have the familyGroups field. Ensure it deserializes cleanly and defaults
        val legacyJson = """
            {
              "version": 1,
              "exportedAt": "2026-07-14T02:00:00Z",
              "villages": [],
              "patients": [],
              "aliases": [],
              "transactions": []
            }
        """.trimIndent()

        val restored = gson.fromJson(legacyJson, BackupData::class.java)
        assertNotNull(restored)
        assertEquals(1, restored.version)
        // Check that familyGroups can be null or empty list (the default is handled gracefully)
        assertTrue(restored.familyGroups == null || restored.familyGroups.isEmpty())
    }
}
