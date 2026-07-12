package com.villageclinicledger.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Room entity representing an alternate name (alias) for a patient.
 * This enables searching by common nicknames or alternative spellings.
 * A foreign key to [Patient] with CASCADE delete ensures aliases are
 * removed when the parent patient is deleted.
 */
@Entity(
    tableName = "aliases",
    foreignKeys = [
        ForeignKey(
            entity = Patient::class,
            parentColumns = ["id"],
            childColumns = ["patient_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("patient_id")]
)
data class Alias(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "patient_id")
    val patientId: Long,

    // The alternate name or nickname for the patient
    @ColumnInfo(name = "alias")
    val alias: String,

    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Date = Date()
) {
    // Populated at query time; not persisted in the aliases table
    @Ignore
    var patient: Patient? = null
}
