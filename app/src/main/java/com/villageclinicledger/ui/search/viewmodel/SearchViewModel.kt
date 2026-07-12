package com.villageclinicledger.ui.search.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.data.models.Village
import com.villageclinicledger.data.repository.PatientRepository

/** ViewModel for the search screen. Manages the reactive query-to-results
 * pipeline: every change to the `_query` LiveData triggers a Room search via
 * `switchMap`. When the query is blank, the UI shows recent patients instead. */
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PatientRepository(getApplication())

    /** The current search query string. Updated on every keystroke. */
    private val _query = MutableLiveData("")

    /** Filtered patient list based on the current query.
     * Managed via MediatorLiveData so each new query switches sources. */
    val searchResults = MediatorLiveData<List<Patient>>().apply {
        var previousSource: LiveData<List<Patient>>? = null
        addSource(_query) { query ->
            previousSource?.let { removeSource(it) }
            val newSource = if (query.isNullOrBlank()) {
                MutableLiveData(emptyList())
            } else {
                repository.searchPatients(query)
            }
            previousSource = newSource
            addSource(newSource) { value = it }
        }
    }

    /** The 15 most recently created patients, shown when no query is active. */
    val recentPatients: LiveData<List<Patient>> = repository.getRecentPatients(15)

    /** True when the query is blank, meaning we should show recent patients. */
    val isShowingRecent = MediatorLiveData<Boolean>().apply {
        addSource(_query) { value = it.isNullOrBlank() }
    }

    /** All villages, used by the fragment to resolve villageId → name. */
    val villages: LiveData<List<Village>> = repository.getAllVillages()

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        MediatorLiveData<Boolean>().apply {
            addSource(searchResults) { _isLoading.postValue(false) }
            addSource(recentPatients) { _isLoading.postValue(false) }
        }
    }

    fun searchPatients(query: String) {
        _query.value = query
        if (query.isNotBlank()) {
            _isLoading.value = true
        }
    }

    fun setError(msg: String) {
        _error.value = msg
    }

    fun clearError() {
        _error.value = null
    }

    suspend fun getPatientByName(name: String): Patient? {
        return repository.getPatientByName(name)
    }

    suspend fun findPatientByVoice(name: String): List<Patient> {
        return repository.findPatientByVoice(name)
    }

    suspend fun insertTransaction(transaction: Transaction) {
        repository.insertTransaction(transaction)
    }
}
