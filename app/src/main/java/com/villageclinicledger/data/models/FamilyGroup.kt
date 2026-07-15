package com.villageclinicledger.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "family_groups",
    indices = [Index("village_id")]
)
data class FamilyGroup(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "caste", defaultValue = "")
    val caste: String = "",
    @ColumnInfo(name = "family_head_name", defaultValue = "")
    val familyHeadName: String = "",
    @ColumnInfo(name = "head_patient_id")
    val headPatientId: Long? = null,
    @ColumnInfo(name = "village_id")
    val villageId: Long,
    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Date = Date()
)
