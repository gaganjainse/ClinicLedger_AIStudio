package com.villageclinicledger.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.villageclinicledger.data.models.Patient

/**
 * Room DAO for the patients table.
 * Provides CRUD operations plus search, filtering by village,
 * and balance-related queries. All write operations are suspend
 * functions for coroutine-based background execution.
 */
@Dao
interface PatientDao {

    /** Inserts a patient, replacing on conflict. Returns the generated row ID. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPatient(patient: Patient): Long

    @Update
    suspend fun updatePatient(patient: Patient)


    /** Returns a single patient by ID, observed via LiveData */
    @Query("SELECT * FROM patients WHERE id = :patientId")
    fun getPatientById(patientId: Long): LiveData<Patient?>

    /** Non-observable variant of getPatientById for use in coroutine contexts */
    @Query("SELECT * FROM patients WHERE id = :patientId")
    suspend fun getPatientByIdSync(patientId: Long): Patient?

    /** Non-observable variant of getAllPatients for use in coroutine contexts */
    @Query("SELECT * FROM patients ORDER BY name ASC")
    suspend fun getAllPatientsSync(): List<Patient>

    /** Observable variant of getAllPatients for reactive UI updates */
    @Query("SELECT * FROM patients ORDER BY name ASC")
    fun getAllPatientsObservable(): LiveData<List<Patient>>

    /**
     * Searches patients by name, phone, village name, or alias.
     * Uses a LEFT JOIN on aliases so patients match even when the
     * search term matches only an alias rather than the primary name.
     */
    @Query("SELECT DISTINCT p.* FROM patients p LEFT JOIN aliases a ON p.id = a.patient_id LEFT JOIN family_groups fg ON p.family_group_id = fg.id WHERE p.name LIKE '%' || :searchQuery || '%' OR p.phone LIKE '%' || :searchQuery || '%' OR p.village_id IN (SELECT id FROM villages WHERE name LIKE '%' || :searchQuery || '%' OR name_hindi LIKE '%' || :searchQuery || '%') OR a.alias LIKE '%' || :searchQuery || '%' OR fg.name LIKE '%' || :searchQuery || '%' OR fg.caste LIKE '%' || :searchQuery || '%' OR fg.family_head_name LIKE '%' || :searchQuery || '%' OR p.relationship LIKE '%' || :searchQuery || '%' ORDER BY p.name ASC")
    fun searchPatients(searchQuery: String): LiveData<List<Patient>>

    @Query("SELECT * FROM patients WHERE family_group_id = :familyGroupId ORDER BY name ASC")
    fun getPatientsByFamilyGroup(familyGroupId: Long): LiveData<List<Patient>>

    /** Returns the most recently updated patients, useful for the home screen */
    @Query("SELECT * FROM patients ORDER BY updated_at DESC LIMIT :limit")
    fun getRecentPatients(limit: Int): LiveData<List<Patient>>

    /** Directly sets the denormalized balance field; called after transaction changes */
    @Query("UPDATE patients SET current_balance = :balance, updated_at = :updatedAt WHERE id = :patientId")
    suspend fun setPatientBalance(patientId: Long, balance: Double, updatedAt: java.util.Date)

    /** Returns a single patient by exact name match */
    @Query("SELECT * FROM patients WHERE name LIKE :name LIMIT 1")
    suspend fun getPatientByName(name: String): Patient?

    /** Returns patients with the highest outstanding balances, for debt tracking */
    @Query("SELECT * FROM patients ORDER BY current_balance DESC LIMIT :limit")
    fun getTopPatientsByBalance(limit: Int): LiveData<List<Patient>>

    /** Finds patients by exact name or alias match — returns up to 5 for disambiguation */
    @Query("SELECT DISTINCT p.* FROM patients p LEFT JOIN aliases a ON p.id = a.patient_id WHERE p.name LIKE '%' || :query || '%' OR a.alias LIKE '%' || :query || '%' LIMIT 5")
    suspend fun findPatientsByNameOrAlias(query: String): List<Patient>

    /** Returns count of patients with positive balance and updated_at before the given date */
    @Query("SELECT COUNT(*) FROM patients WHERE current_balance > 0 AND updated_at < :date")
    suspend fun getDefaultersCount(date: java.util.Date): Int

    @Query("SELECT COALESCE(SUM(current_balance), 0.0) FROM patients WHERE current_balance > 0")
    fun getTotalDueObservable(): LiveData<Double>

    @Query("DELETE FROM patients")
    suspend fun deleteAll()
}
