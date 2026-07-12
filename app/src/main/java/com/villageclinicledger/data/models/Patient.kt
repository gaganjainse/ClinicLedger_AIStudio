package com.villageclinicledger.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Room entity representing a patient in the clinic ledger.
 * Each patient belongs to a village and carries a running balance of
 * medicine costs minus payments. The balance is recalculated from
 * transactions rather than being the source of truth.
 */
@Entity(tableName = "patients")
data class Patient(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    // Foreign key referencing the village this patient belongs to
    @ColumnInfo(name = "village_id")
    val villageId: Long,

    @ColumnInfo(name = "phone", defaultValue = "")
    val phone: String = "",

    @ColumnInfo(name = "family_group_id")
    val familyGroupId: Long? = null,

    // Denormalized balance derived from transactions; recalculated on each insert/update
    @ColumnInfo(name = "current_balance", defaultValue = "0.0")
    val currentBalance: Double = 0.0,

    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "updated_at", defaultValue = "CURRENT_TIMESTAMP")
    var updatedAt: Date = Date()
) {
    // Populated at query time via JOINs; not persisted in the patients table
    @Ignore
    var village: Village? = null

    @Ignore
    var aliases: List<Alias> = emptyList()

    @Ignore
    var transactions: List<Transaction> = emptyList()

    /** Returns the balance formatted to two decimal places for UI display */
    val formattedBalance: String
        get() = String.format("%.2f", currentBalance)
}
