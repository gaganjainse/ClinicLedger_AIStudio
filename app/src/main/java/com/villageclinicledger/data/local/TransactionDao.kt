package com.villageclinicledger.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.villageclinicledger.data.models.Transaction
import java.util.Date

/**
 * Room DAO for the transactions table.
 * Transactions track medicine costs, payments, and balance adjustments.
 * The patient's running balance is computed by aggregating transactions
 * rather than being stored as a separate field.
 */
@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    /** Returns all transactions newest-first */
    @Query("SELECT * FROM transactions ORDER BY created_at DESC")
    fun getAllTransactions(): LiveData<List<Transaction>>

    /** Non-observable variant of getAllTransactions for coroutine contexts */
    @Query("SELECT * FROM transactions ORDER BY created_at DESC")
    suspend fun getAllTransactionsSync(): List<Transaction>

    /** Returns all transactions for a specific patient, newest first */
    @Query("SELECT * FROM transactions WHERE patient_id = :patientId ORDER BY created_at DESC")
    fun getTransactionsByPatient(patientId: Long): LiveData<List<Transaction>>

    /**
     * Computes the patient's balance by summing debits (medicine/adjustment)
     * as positive and credits (payment) as negative. Returns null when
     * no transactions exist for the patient.
     */
    @Query("SELECT SUM(CASE WHEN type = 'medicine' OR type = 'adjustment' THEN amount ELSE -amount END) FROM transactions WHERE patient_id = :patientId")
    suspend fun getPatientBalance(patientId: Long): Double?

    /** Total medicine costs since a given date, for reporting */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'medicine' AND created_at >= :since")
    suspend fun getTotalMedicineSince(since: Date): Double

    /** Total payments received since a given date, for reporting */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = 'payment' AND created_at >= :since")
    suspend fun getTotalPaymentsSince(since: Date): Double

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
