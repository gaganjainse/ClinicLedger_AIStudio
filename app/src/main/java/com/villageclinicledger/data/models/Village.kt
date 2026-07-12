package com.villageclinicledger.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Room entity representing a village that patients belong to.
 * The name column has a unique index to prevent duplicate village entries.
 */
@Entity(
    tableName = "villages",
    indices = [Index("name", unique = true)]
)
data class Village(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Date = Date()
) {
    // Populated at query time; not persisted in the villages table
    @Ignore
    var patients: List<Patient> = emptyList()
}
