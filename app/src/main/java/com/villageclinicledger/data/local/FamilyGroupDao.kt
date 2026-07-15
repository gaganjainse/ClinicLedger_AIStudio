package com.villageclinicledger.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.villageclinicledger.data.models.FamilyGroup

@Dao
interface FamilyGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyGroup(familyGroup: FamilyGroup): Long

    @Update
    suspend fun updateFamilyGroup(familyGroup: FamilyGroup)

    @Delete
    suspend fun deleteFamilyGroup(familyGroup: FamilyGroup)

    @Query("SELECT * FROM family_groups ORDER BY name ASC")
    fun getAllFamilyGroups(): LiveData<List<FamilyGroup>>

    @Query("SELECT * FROM family_groups ORDER BY name ASC")
    suspend fun getAllFamilyGroupsSync(): List<FamilyGroup>

    @Query("SELECT * FROM family_groups WHERE id = :familyGroupId")
    suspend fun getFamilyGroupById(familyGroupId: Long): FamilyGroup?

    @Query("SELECT * FROM family_groups WHERE village_id = :villageId ORDER BY name ASC")
    fun getFamilyGroupsByVillage(villageId: Long): LiveData<List<FamilyGroup>>

    @Query("SELECT * FROM family_groups WHERE name LIKE '%' || :searchQuery || '%' ORDER BY name ASC")
    fun searchFamilyGroups(searchQuery: String): LiveData<List<FamilyGroup>>

    @Query("DELETE FROM family_groups")
    suspend fun deleteAll()
}
