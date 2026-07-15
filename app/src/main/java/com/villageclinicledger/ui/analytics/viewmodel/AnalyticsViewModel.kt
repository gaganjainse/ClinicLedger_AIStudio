package com.villageclinicledger.ui.analytics.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.repository.PatientRepository
import com.villageclinicledger.ui.util.DateTimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

data class VillageDuesItem(
    val villageId: Long,
    val name: String,
    val nameHindi: String,
    val totalDues: Double,
    val patientCount: Int
)

data class TransactionWithPatient(
    val id: Long,
    val patientId: Long,
    val patientName: String,
    val villageName: String,
    val type: String,
    val amount: Double,
    val notes: String,
    val createdAt: Date
)

data class AnalyticsUiState(
    val totalOutstandingDues: Double = 0.0,
    val todayCollected: Double = 0.0,
    val todayMedicine: Double = 0.0,
    val thisWeekCollected: Double = 0.0,
    val thisWeekMedicine: Double = 0.0,
    val thisMonthCollected: Double = 0.0,
    val thisMonthMedicine: Double = 0.0,
    val recoveryRate: Double = 0.0,
    val defaulters30Days: Int = 0,
    val defaulters90Days: Int = 0,
    val defaulters180Days: Int = 0,
    val villageDuesList: List<VillageDuesItem> = emptyList(),
    val topPatientsWithDues: List<Patient> = emptyList(),
    val inactivePatients60Days: List<Patient> = emptyList(),
    val isLoading: Boolean = true,
    val outstandingPatientsList: List<Patient> = emptyList(),
    val todayCollectedList: List<TransactionWithPatient> = emptyList(),
    val thisMonthMedicineList: List<TransactionWithPatient> = emptyList(),
    val defaulters30List: List<Patient> = emptyList(),
    val defaulters90List: List<Patient> = emptyList(),
    val defaulters180List: List<Patient> = emptyList()
)

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PatientRepository(getApplication())

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        refreshAnalytics()
    }

    fun refreshAnalytics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                calculateStats()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun calculateStats() = withContext(Dispatchers.IO) {
        // Date computations
        val todayMidnight = DateTimeUtils.getStartOfDay()
        val weekStart = DateTimeUtils.getStartOfWeek()
        val monthStart = DateTimeUtils.getStartOfMonth()

        // Defaulters
        val d30Date = DateTimeUtils.getDaysAgo(30)
        val d90Date = DateTimeUtils.getDaysAgo(90)
        val d180Date = DateTimeUtils.getDaysAgo(180)

        // Fetch Raw Data
        val tM = repository.getTotalMedicineSince(todayMidnight)
        val tP = repository.getTotalPaymentsSince(todayMidnight)
        val wM = repository.getTotalMedicineSince(weekStart)
        val wP = repository.getTotalPaymentsSince(weekStart)
        val mM = repository.getTotalMedicineSince(monthStart)
        val mP = repository.getTotalPaymentsSince(monthStart)

        val c30 = repository.getDefaultersCount(d30Date)
        val c90 = repository.getDefaultersCount(d90Date)
        val c180 = repository.getDefaultersCount(d180Date)

        // Fetch All Patients to compute Village Dues and Outstanding stats
        val allPatients = repository.getAllPatientsSync()
        
        // Compute Total Outstanding Dues (sum of all positive balances)
        val totalDuesSum = allPatients.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }

        // Fetch All Villages
        val database = com.villageclinicledger.data.local.VillageClinicLedgerDatabase.getDatabase(getApplication())
        val villageDao = database.villageDao()
        val allVillages = villageDao.getAllVillagesSync()

        // Create a fast map of villageId -> Village
        val villageMap = allVillages.associateBy { it.id }

        // Compute Village Dues
        val villageDues = allPatients.groupBy { it.villageId }.map { (villageId, patients) ->
            val totalDues = patients.filter { it.currentBalance > 0 }.sumOf { it.currentBalance }
            val count = patients.size
            val v = villageMap[villageId]
            VillageDuesItem(
                villageId = villageId,
                name = v?.name ?: "Unknown",
                nameHindi = v?.nameHindi ?: "अज्ञात",
                totalDues = totalDues,
                patientCount = count
            )
        }.sortedByDescending { it.totalDues }

        // Top 10 Patients by Balance (dues)
        val topPatientsList = allPatients.filter { it.currentBalance > 0 }
            .sortedByDescending { it.currentBalance }
            .take(10)
            .map { p ->
                p.apply { village = villageMap[p.villageId] }
            }

        // Calculate inactive patients (no visits/updates in 60+ days)
        val cal60Date = DateTimeUtils.getDaysAgo(60)
        val inactivePatientsList = allPatients.filter { it.updatedAt.before(cal60Date) }
            .sortedBy { it.updatedAt }
            .map { p ->
                p.apply { village = villageMap[p.villageId] }
            }

        // Calculate recovery rate
        val recoveryRate = if (mM > 0) (mP / (mM)) * 100.0 else 100.0

        // Fetch All Transactions
        val allTransactions = repository.getAllTransactionsSync()
        val patientMap = allPatients.associateBy { it.id }

        val outstandingPatients = allPatients.filter { it.currentBalance > 0 }
            .sortedByDescending { it.currentBalance }
            .map { p -> p.apply { village = villageMap[p.villageId] } }

        val todayCollectedList = allTransactions.filter { it.type == "payment" && it.createdAt.after(todayMidnight) }
            .map { t ->
                val p = patientMap[t.patientId]
                val v = p?.let { villageMap[it.villageId] }
                TransactionWithPatient(
                    id = t.id,
                    patientId = t.patientId,
                    patientName = p?.name ?: "Unknown Patient",
                    villageName = v?.let { v.name } ?: "Unknown Village",
                    type = t.type,
                    amount = t.amount,
                    notes = t.notes,
                    createdAt = t.createdAt
                )
            }

        val thisMonthMedicineList = allTransactions.filter { it.type == "medicine" && it.createdAt.after(monthStart) }
            .map { t ->
                val p = patientMap[t.patientId]
                val v = p?.let { villageMap[it.villageId] }
                TransactionWithPatient(
                    id = t.id,
                    patientId = t.patientId,
                    patientName = p?.name ?: "Unknown Patient",
                    villageName = v?.let { v.name } ?: "Unknown Village",
                    type = t.type,
                    amount = t.amount,
                    notes = t.notes,
                    createdAt = t.createdAt
                )
            }

        val d30List = allPatients.filter { it.currentBalance > 0 && it.updatedAt.before(d30Date) }
            .sortedBy { it.updatedAt }
            .map { p -> p.apply { village = villageMap[p.villageId] } }

        val d90List = allPatients.filter { it.currentBalance > 0 && it.updatedAt.before(d90Date) }
            .sortedBy { it.updatedAt }
            .map { p -> p.apply { village = villageMap[p.villageId] } }

        val d180List = allPatients.filter { it.currentBalance > 0 && it.updatedAt.before(d180Date) }
            .sortedBy { it.updatedAt }
            .map { p -> p.apply { village = villageMap[p.villageId] } }

        val newState = AnalyticsUiState(
            totalOutstandingDues = totalDuesSum,
            todayCollected = tP,
            todayMedicine = tM,
            thisWeekCollected = wP,
            thisWeekMedicine = wM,
            thisMonthCollected = mP,
            thisMonthMedicine = mM,
            recoveryRate = recoveryRate,
            defaulters30Days = c30,
            defaulters90Days = c90,
            defaulters180Days = c180,
            villageDuesList = villageDues,
            topPatientsWithDues = topPatientsList,
            inactivePatients60Days = inactivePatientsList,
            isLoading = false,
            outstandingPatientsList = outstandingPatients,
            todayCollectedList = todayCollectedList,
            thisMonthMedicineList = thisMonthMedicineList,
            defaulters30List = d30List,
            defaulters90List = d90List,
            defaulters180List = d180List
        )

        _uiState.value = newState
    }
}
