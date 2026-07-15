package com.villageclinicledger.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.villageclinicledger.data.local.VillageClinicLedgerDatabase
import java.util.Date
import com.villageclinicledger.data.models.Alias
import com.villageclinicledger.data.models.FamilyGroup
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.data.models.Village
import com.villageclinicledger.ui.util.DateTimeUtils

/**
 * Central repository that coordinates data access across all four DAOs
 * (Patient, Village, Alias, Transaction). ViewModels and UI controllers
 * interact with this repository rather than with DAOs directly.
 *
 * Key responsibility: when a transaction is inserted, the repository
 * automatically recalculates and updates the patient's denormalized
 * balance field so that balance queries remain fast.
 */
class PatientRepository(private val context: Context) {

    private val database: VillageClinicLedgerDatabase by lazy {
        VillageClinicLedgerDatabase.getDatabase(context)
    }
    private val patientDao by lazy { database.patientDao() }
    private val villageDao by lazy { database.villageDao() }
    private val aliasDao by lazy { database.aliasDao() }
    private val transactionDao by lazy { database.transactionDao() }
    private val familyGroupDao by lazy { database.familyGroupDao() }

    suspend fun insertPatient(patient: Patient): Long {
        return patientDao.insertPatient(patient)
    }

    suspend fun updatePatient(patient: Patient) {
        patientDao.updatePatient(patient)
    }

    fun getPatientById(patientId: Long): LiveData<Patient?> {
        return patientDao.getPatientById(patientId)
    }

    suspend fun getPatientByIdSync(patientId: Long): Patient? {
        return patientDao.getPatientByIdSync(patientId)
    }

    suspend fun getAllPatientsSync(): List<Patient> {
        return patientDao.getAllPatientsSync()
    }

    fun getAllPatientsObservable(): LiveData<List<Patient>> {
        return patientDao.getAllPatientsObservable()
    }

    fun getAllTransactions(): LiveData<List<Transaction>> {
        return transactionDao.getAllTransactions()
    }

    suspend fun getAllTransactionsSync(): List<Transaction> {
        return transactionDao.getAllTransactionsSync()
    }

    suspend fun getLastTransaction(): Transaction? {
        return transactionDao.getAllTransactionsSync().firstOrNull()
    }

    suspend fun getDefaultersCount(date: Date): Int {
        return patientDao.getDefaultersCount(date)
    }

    /** Delegates search term directly to the DAO, which adds the SQLite wildcards */
    fun searchPatients(searchQuery: String): LiveData<List<Patient>> {
        return patientDao.searchPatients(searchQuery)
    }

    suspend fun getPatientByName(name: String): Patient? {
        return patientDao.getPatientByName(name)
    }

    suspend fun findPatientByVoice(name: String): List<Patient> {
        return patientDao.findPatientsByNameOrAlias(name)
    }

    fun getRecentPatients(limit: Int = 15): LiveData<List<Patient>> {
        return patientDao.getRecentPatients(limit)
    }

    suspend fun insertVillage(village: Village): Long {
        val updatedVillage = translateVillageBilingual(village)
        return villageDao.insertVillage(updatedVillage)
    }

    suspend fun updateVillage(village: Village) {
        val updatedVillage = translateVillageBilingual(village)
        villageDao.updateVillage(updatedVillage)
    }

    suspend fun deleteVillage(village: Village) {
        villageDao.deleteVillage(village)
    }

    fun getAllVillages(): LiveData<List<Village>> {
        return villageDao.getAllVillages()
    }

    suspend fun getAllVillagesSync(): List<Village> {
        return villageDao.getAllVillagesSync()
    }

    suspend fun insertAlias(alias: Alias): Long {
        return aliasDao.insertAlias(alias)
    }

    suspend fun deleteAlias(alias: Alias) {
        aliasDao.deleteAlias(alias)
    }

    fun getAliasesByPatient(patientId: Long): LiveData<List<Alias>> {
        return aliasDao.getAliasesByPatient(patientId)
    }

    suspend fun deleteAliasesByPatient(patientId: Long) {
        aliasDao.deleteAliasesByPatient(patientId)
    }

    suspend fun insertFamilyGroup(familyGroup: FamilyGroup): Long {
        return familyGroupDao.insertFamilyGroup(familyGroup)
    }

    suspend fun updateFamilyGroup(familyGroup: FamilyGroup) {
        familyGroupDao.updateFamilyGroup(familyGroup)
    }

    suspend fun deleteFamilyGroup(familyGroup: FamilyGroup) {
        familyGroupDao.deleteFamilyGroup(familyGroup)
    }

    fun getAllFamilyGroups(): LiveData<List<FamilyGroup>> {
        return familyGroupDao.getAllFamilyGroups()
    }

    suspend fun getFamilyGroupById(familyGroupId: Long): FamilyGroup? {
        return familyGroupDao.getFamilyGroupById(familyGroupId)
    }

    fun getPatientsByFamilyGroup(familyGroupId: Long): LiveData<List<Patient>> {
        return patientDao.getPatientsByFamilyGroup(familyGroupId)
    }

    /**
     * Recomputes the patient's balance from all their transactions and
     * updates the denormalized current_balance field on the patient record.
     * Called automatically after every transaction insert.
     */
    suspend fun recalculateBalance(patientId: Long, customDate: java.util.Date? = null) {
        val rawBalance = transactionDao.getPatientBalance(patientId) ?: 0.0
        patientDao.setPatientBalance(patientId, rawBalance, customDate ?: java.util.Date())
    }

    /**
     * Inserts a transaction and immediately recalculates the patient's
     * balance to keep the denormalized field in sync.
     */
    suspend fun insertTransaction(transaction: Transaction): Long {
        val transactionId = transactionDao.insertTransaction(transaction)
        recalculateBalance(transaction.patientId, transaction.createdAt)
        return transactionId
    }

    fun getTransactionsByPatient(patientId: Long): LiveData<List<Transaction>> {
        return transactionDao.getTransactionsByPatient(patientId)
    }

    fun getTopPatientsByBalance(limit: Int = 10): LiveData<List<Patient>> {
        return patientDao.getTopPatientsByBalance(limit)
    }

    /** Total medicine costs since a given date, for summary reports */
    suspend fun getTotalMedicineSince(since: Date): Double {
        return transactionDao.getTotalMedicineSince(since)
    }

    /** Total payments received since a given date, for summary reports */
    suspend fun getTotalPaymentsSince(since: Date): Double {
        return transactionDao.getTotalPaymentsSince(since)
    }

    fun getTotalDueObservable(): LiveData<Double> {
        return patientDao.getTotalDueObservable()
    }

    fun getTotalCollectedTodayObservable(): LiveData<Double> {
        val midnight = DateTimeUtils.getStartOfDay()
        return transactionDao.getTotalCollectedSinceObservable(midnight)
    }

    private suspend fun translateVillageBilingual(village: Village): Village {
        val rawName = village.name.trim()
        if (rawName.contains("/")) {
            val parts = rawName.split("/").map { it.trim() }
            if (parts.size >= 2) {
                val eng = parts[0]
                val hin = parts[1]
                val secondHasHindi = hin.any { it in '\u0900'..'\u097F' }
                if (secondHasHindi) {
                    return village.copy(
                        name = if (eng.isNotEmpty()) eng else hin,
                        nameHindi = if (hin.isNotEmpty()) hin else eng
                    )
                }
            }
        }
        
        val hasHindi = rawName.any { it in '\u0900'..'\u097F' }
        return if (hasHindi) {
            village.copy(name = rawName, nameHindi = rawName)
        } else {
            village.copy(name = rawName, nameHindi = "")
        }
    }
}
