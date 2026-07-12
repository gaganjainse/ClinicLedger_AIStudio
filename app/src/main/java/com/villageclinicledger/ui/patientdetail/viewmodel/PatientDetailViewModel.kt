package com.villageclinicledger.ui.patientdetail.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.villageclinicledger.data.models.Alias
import com.villageclinicledger.data.models.FamilyGroup
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.data.models.Village
import com.villageclinicledger.data.repository.PatientRepository
import kotlinx.coroutines.launch

/** ViewModel for the Patient Detail screen. Maintains a single patient ID
 * and uses switchMap to reactively load the patient record, their aliases,
 * and their transaction history whenever the ID changes. */
class PatientDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PatientRepository(getApplication())

    /** The currently viewed patient's ID. Setting this triggers all three
     * reactive loads below. */
    private val _patientId = MutableLiveData<Long>()

    /** The full Patient object, or null if the ID is invalid. */
    val patient = MediatorLiveData<Patient?>().apply {
        var previousSource: LiveData<Patient?>? = null
        addSource(_patientId) { id ->
            previousSource?.let { removeSource(it) }
            val newSource = if (id != null && id > 0) repository.getPatientById(id) else MutableLiveData<Patient?>(null)
            previousSource = newSource
            addSource(newSource) { value = it }
        }
    }

    /** All villages (used by the Fragment to resolve the village name). */
    val villages: LiveData<List<Village>> = repository.getAllVillages()

    /** Alias list for the current patient, reactively reloaded on patient ID change. */
    val aliases = MediatorLiveData<List<Alias>>().apply {
        var previousSource: LiveData<List<Alias>>? = null
        addSource(_patientId) { id ->
            previousSource?.let { removeSource(it) }
            val newSource = if (id != null && id > 0) repository.getAliasesByPatient(id) else MutableLiveData<List<Alias>>(emptyList())
            previousSource = newSource
            addSource(newSource) { value = it }
        }
    }

    /** Transaction list for the current patient, sorted by date (via Room query). */
    val transactions = MediatorLiveData<List<Transaction>>().apply {
        var previousSource: LiveData<List<Transaction>>? = null
        addSource(_patientId) { id ->
            previousSource?.let { removeSource(it) }
            val newSource = if (id != null && id > 0) repository.getTransactionsByPatient(id) else MutableLiveData<List<Transaction>>(emptyList())
            previousSource = newSource
            addSource(newSource) { value = it }
        }
    }

    private val _familyGroupId = MutableLiveData<Long?>()

    val familyGroup = MediatorLiveData<FamilyGroup?>().apply {
        var previousSource: LiveData<FamilyGroup?>? = null
        addSource(_familyGroupId) { id ->
            previousSource?.let { removeSource(it) }
            val newSource = if (id != null && id > 0) {
                MutableLiveData<FamilyGroup?>().also { liveData ->
                    viewModelScope.launch { liveData.value = repository.getFamilyGroupById(id) }
                }
            } else MutableLiveData<FamilyGroup?>(null)
            previousSource = newSource
            addSource(newSource) { value = it }
        }
    }

    val familyMembers = MediatorLiveData<List<Patient>>().apply {
        var previousSource: LiveData<List<Patient>>? = null
        addSource(_familyGroupId) { id ->
            previousSource?.let { removeSource(it) }
            val newSource = if (id != null && id > 0) repository.getPatientsByFamilyGroup(id) else MutableLiveData<List<Patient>>(emptyList())
            previousSource = newSource
            addSource(newSource) { value = it }
        }
    }

    fun setFamilyGroupId(id: Long?) {
        _familyGroupId.value = id
    }

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        MediatorLiveData<Boolean>().apply {
            addSource(patient) { _isLoading.postValue(false) }
        }
    }

    /** Loads data for the given patient ID. Called once from the Fragment's
     * onViewCreated. Subsequent calls will replace the existing data. */
    fun loadPatient(patientId: Long) {
        _patientId.value = patientId
        _isLoading.value = true
    }

    fun updatePatient(patient: Patient) {
        viewModelScope.launch {
            try {
                repository.updatePatient(patient)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to update patient")
            }
        }
    }

    /** Inserts a new alias for the current patient on a background coroutine. */
    fun addAlias(alias: Alias) {
        viewModelScope.launch {
            try {
                repository.insertAlias(alias)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to add alias")
            }
        }
    }

    fun deleteAlias(alias: Alias) {
        viewModelScope.launch {
            try {
                repository.deleteAlias(alias)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to delete alias")
            }
        }
    }

    /** Inserts a new transaction (medicine charge, payment, or adjustment)
     * for the current patient. The Room DAO automatically recalculates
     * the patient's balance via a trigger or app-level logic. */
    fun addTransaction(transaction: Transaction) {
        viewModelScope.launch {
            try {
                repository.insertTransaction(transaction)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to add transaction")
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
