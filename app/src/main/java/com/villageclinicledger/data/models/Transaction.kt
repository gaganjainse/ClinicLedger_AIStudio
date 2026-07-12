package com.villageclinicledger.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Room entity representing a financial transaction for a patient.
 * Each transaction is either a debit (medicine cost or adjustment) or
 * a credit (payment). The patient's running balance is derived by
 * summing all transactions: debits increase the balance, credits decrease it.
 *
 * A foreign key to [Patient] with CASCADE delete ensures that when a
 * patient is removed, all their transactions are removed as well.
 */
@Entity(
    tableName = "transactions",
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
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "patient_id")
    val patientId: Long,

    // Transaction type: "medicine" (debit), "payment" (credit), or "adjustment" (debit)
    @ColumnInfo(name = "type")
    val type: String,

    // Positive monetary amount; sign is determined by the type field
    @ColumnInfo(name = "amount")
    val amount: Double,

    @ColumnInfo(name = "notes", defaultValue = "")
    val notes: String = "",

    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "updated_at", defaultValue = "CURRENT_TIMESTAMP")
    var updatedAt: Date = Date()
) {
    // Populated at query time via JOIN; not persisted in the transactions table
    @Ignore
    var patient: Patient? = null

    /** Amount formatted to two decimal places for UI display */
    val formattedAmount: String
        get() = String.format("%.2f", amount)

    /** Background color associated with the transaction type for visual distinction */
    val typeColor: Int
        get() = when (type) {
            "medicine" -> android.graphics.Color.parseColor("#FFCDD2")
            "payment" -> android.graphics.Color.parseColor("#C8E6C9")
            "adjustment" -> android.graphics.Color.parseColor("#BBDEFB")
            else -> android.graphics.Color.parseColor("#EEEEEE")
        }

    /** True if this transaction increases the patient's balance (medicine or adjustment) */
    val isDebit: Boolean
        get() = type == "medicine" || type == "adjustment"
}
