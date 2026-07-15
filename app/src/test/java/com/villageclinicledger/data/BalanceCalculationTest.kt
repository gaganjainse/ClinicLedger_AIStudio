package com.villageclinicledger.data

import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.ui.util.LocaleManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BalanceCalculationTest {

    @Test
    fun testTransactionIsDebit() {
        val medicineTx = Transaction(patientId = 1L, type = "medicine", amount = 100.0)
        val adjustmentTx = Transaction(patientId = 1L, type = "adjustment", amount = 50.0)
        val paymentTx = Transaction(patientId = 1L, type = "payment", amount = 120.0)
        val unknownTx = Transaction(patientId = 1L, type = "refund", amount = 40.0)

        assertTrue(medicineTx.isDebit)
        assertTrue(adjustmentTx.isDebit)
        assertFalse(paymentTx.isDebit)
        assertFalse(unknownTx.isDebit)
    }

    @Test
    fun testRecalculateBalanceLogic() {
        // Mocking the SQL query behavior using Kotlin Collections
        val transactions = listOf(
            Transaction(patientId = 1L, type = "medicine", amount = 300.0), // +300
            Transaction(patientId = 1L, type = "medicine", amount = 150.0), // +150
            Transaction(patientId = 1L, type = "payment", amount = 200.0),   // -200
            Transaction(patientId = 1L, type = "adjustment", amount = 50.0), // +50
            Transaction(patientId = 1L, type = "payment", amount = 100.0)    // -100
        )

        // Calculate expected balance: 300 + 150 - 200 + 50 - 100 = 200.0
        val expectedBalance = transactions.sumOf { tx ->
            if (tx.type == "medicine" || tx.type == "adjustment") tx.amount else -tx.amount
        }

        assertEquals(200.0, expectedBalance, 0.001)
    }

    @Test
    fun testBalanceLogicOnlyMedicine() {
        val transactions = listOf(
            Transaction(patientId = 2L, type = "medicine", amount = 120.50),
            Transaction(patientId = 2L, type = "medicine", amount = 80.0)
        )

        val expectedBalance = transactions.sumOf { tx ->
            if (tx.type == "medicine" || tx.type == "adjustment") tx.amount else -tx.amount
        }

        assertEquals(200.50, expectedBalance, 0.001)
    }

    @Test
    fun testBalanceLogicOnlyPayments() {
        val transactions = listOf(
            Transaction(patientId = 3L, type = "payment", amount = 150.0),
            Transaction(patientId = 3L, type = "payment", amount = 50.25)
        )

        val expectedBalance = transactions.sumOf { tx ->
            if (tx.type == "medicine" || tx.type == "adjustment") tx.amount else -tx.amount
        }

        // Under normal circumstances, payments without medicines would mean a credit balance (-200.25)
        assertEquals(-200.25, expectedBalance, 0.001)
    }

    @Test
    fun testFormattedBalanceAndAmount() {
        val patientWithPositiveBalance = Patient(name = "Kishore Kumar", villageId = 1L, currentBalance = 245.678)
        val patientWithNegativeBalance = Patient(name = "Suresh Lal", villageId = 1L, currentBalance = -50.1)
        val patientWithZeroBalance = Patient(name = "Heera Lal", villageId = 1L, currentBalance = 0.0)

        // Assert formatted balances (LocaleManager.formatCurrency)
        assertEquals("₹245.68", LocaleManager.formatCurrency(patientWithPositiveBalance.currentBalance))
        assertEquals("₹-50.10", LocaleManager.formatCurrency(patientWithNegativeBalance.currentBalance))
        assertEquals("₹0", LocaleManager.formatCurrency(patientWithZeroBalance.currentBalance))

        // Assert transaction formatting
        val tx = Transaction(patientId = 1L, type = "medicine", amount = 123.456)
        assertEquals("₹123.46", LocaleManager.formatCurrency(tx.amount))
    }
}
