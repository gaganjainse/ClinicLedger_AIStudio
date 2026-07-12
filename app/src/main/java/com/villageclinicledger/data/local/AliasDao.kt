package com.villageclinicledger.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.villageclinicledger.data.models.Alias

/**
 * Room DAO for the aliases table.
 * Aliases allow patients to be found by nicknames or alternate spellings.
 */
@Dao
interface AliasDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: Alias): Long

    @Delete
    suspend fun deleteAlias(alias: Alias)

    /** Non-observable variant of getAllAliases for coroutine contexts */
    @Query("SELECT * FROM aliases ORDER BY alias ASC")
    suspend fun getAllAliasesSync(): List<Alias>

    /** Returns all aliases belonging to a specific patient */
    @Query("SELECT * FROM aliases WHERE patient_id = :patientId ORDER BY alias ASC")
    fun getAliasesByPatient(patientId: Long): LiveData<List<Alias>>

    /** Removes all aliases for a given patient, used when replacing the alias list */
    @Query("DELETE FROM aliases WHERE patient_id = :patientId")
    suspend fun deleteAliasesByPatient(patientId: Long)

    @Query("DELETE FROM aliases")
    suspend fun deleteAll()
}
