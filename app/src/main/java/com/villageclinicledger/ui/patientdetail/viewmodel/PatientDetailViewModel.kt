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

class PatientDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PatientRepository(application)

    private val _patientId = MutableLiveData<Long>()

    val patient = MediatorLiveData<Patient?>().apply {
        var previousSource: LiveData<Patient?>? = null
        addSource(_patientId) { id ->
            previousSource?.let { removeSource(it) }
            val newSource = if (id != null && id > 0) repository.getPatientById(id) else MutableLiveData<Patient?>(null)
            previousSource = newSource
            addSource(newSource) { value = it }
        }
    }

    val villages: LiveData<List<Village>> = repository.getAllVillages()

    val aliases = MediatorLiveData<List<Alias>>().apply {
        var previousSource: LiveData<List<Alias>>? = null
        addSource(_patientId) { id ->
            previousSource?.let { removeSource(it) }
            val newSource = if (id != null && id > 0) repository.getAliasesByPatient(id) else MutableLiveData<List<Alias>>(emptyList())
            previousSource = newSource
            addSource(newSource) { value = it }
        }
    }

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

    fun addAlias(patientId: Long, aliasName: String) {
        viewModelScope.launch {
            try {
                repository.insertAlias(Alias(patientId = patientId, alias = aliasName))
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

    fun linkFamilyGroup(patientId: Long, familyId: Long?) {
        viewModelScope.launch {
            try {
                val p = repository.getPatientByIdSync(patientId)
                if (p != null) {
                    repository.updatePatient(p.copy(familyGroupId = familyId))
                }
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to link family group")
            }
        }
    }

    fun createAndLinkFamilyGroup(patientId: Long, name: String, villageId: Long) {
        viewModelScope.launch {
            try {
                val familyId = repository.insertFamilyGroup(FamilyGroup(name = name, villageId = villageId))
                linkFamilyGroup(patientId, familyId)
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to create and link family group")
            }
        }
    }

    fun addTransaction(patientId: Long, type: String, amount: Double, notes: String, date: java.util.Date) {
        viewModelScope.launch {
            try {
                repository.insertTransaction(Transaction(patientId = patientId, type = type, amount = amount, notes = notes, createdAt = date))
            } catch (e: Exception) {
                _error.postValue(e.message ?: "Failed to add transaction")
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
