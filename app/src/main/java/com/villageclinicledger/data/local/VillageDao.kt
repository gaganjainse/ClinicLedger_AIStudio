package com.villageclinicledger.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.villageclinicledger.data.models.Village

/**
 * Room DAO for the villages table.
 * Villages are referenced by patients and used for grouping/filtering.
 */
@Dao
interface VillageDao {

    /** Inserts a village, replacing on conflict. Returns the generated row ID. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVillage(village: Village): Long

    @Update
    suspend fun updateVillage(village: Village)

    @Delete
    suspend fun deleteVillage(village: Village)

    /** Returns all villages sorted alphabetically */
    @Query("SELECT * FROM villages ORDER BY name ASC")
    fun getAllVillages(): LiveData<List<Village>>

    /** Non-observable variant of getAllVillages for use in coroutine contexts */
    @Query("SELECT * FROM villages ORDER BY name ASC")
    suspend fun getAllVillagesSync(): List<Village>

    @Query("DELETE FROM villages")
    suspend fun deleteAll()
}
