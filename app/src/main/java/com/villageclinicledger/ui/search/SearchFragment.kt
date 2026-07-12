package com.villageclinicledger.ui.search

import android.Manifest
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.villageclinicledger.R
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.ui.PatientDetailFragment
import com.villageclinicledger.ui.search.adapter.SearchResultsAdapter
import com.villageclinicledger.ui.search.viewmodel.SearchViewModel
import com.villageclinicledger.voice.VoiceInputSheet
import com.villageclinicledger.databinding.FragmentSearchBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

/** Primary search screen. Provides a search bar with voice input support,
 * displays recent patients by default, and switches to filtered results as
 * the user types. Includes a quick-entry FAB for recording a transaction
 * without navigating to the patient detail screen. */
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: SearchViewModel
    private lateinit var adapter: SearchResultsAdapter
    private var allPatients: List<Patient> = emptyList()

    /** Maps village IDs to display names, refreshed whenever the villages
     * LiveData emits. Used by SearchResultsAdapter to resolve villageId. */
    private var villagesMap: Map<Long, String> = emptyMap()
    private var familyGroupsMap: Map<Long, String> = emptyMap()

    /** Launcher for the RECORD_AUDIO permission request. On grant, opens the
     * voice conversation sheet; on denial, shows a toast explaining why. */
    private val voicePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            VoiceInputSheet().show(parentFragmentManager, "VoiceInputSheet")
        } else {
            Toast.makeText(requireContext(), "Voice search requires microphone permission", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[SearchViewModel::class.java]

        setupToolbar()
        setupRecyclerView()
        setupSearchEditText()
        setupVoiceSearch()
        setupFab()
        observeViewModel()
        // ViewModel uses switchMap on _query LiveData; each text change
        // triggers a Room query via the repository.
    }

    /** Configures the search input's end-icon to open the full voice
     * conversation sheet (the talking ledger experience). Requests
     * RECORD_AUDIO permission if not yet granted. */
    private fun setupVoiceSearch() {
        binding.searchInputLayout.setEndIconOnClickListener {
            val permission = Manifest.permission.RECORD_AUDIO
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), permission)
                == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                VoiceInputSheet().show(parentFragmentManager, "VoiceInputSheet")
            } else {
                voicePermissionLauncher.launch(permission)
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupRecyclerView() {
        adapter = SearchResultsAdapter(
            onItemClick = { patient -> openPatientDetail(patient.id) },
            getVillageName = { villageId -> villagesMap[villageId] ?: "" },
            getFamilyGroupName = { groupId -> if (groupId != null) familyGroupsMap[groupId] ?: "" else "" }
        )
        binding.searchResultsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.searchResultsRecyclerView.adapter = adapter
    }

    /** Attaches a TextWatcher to the search EditText that forwards every
     * keystroke to the ViewModel for reactive filtering. */
    private fun setupSearchEditText() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.searchPatients(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    /** FAB opens the quick-entry dialog for recording a transaction against
     * any patient without leaving the search screen. */
    private fun setupFab() {
        binding.fabQuickEntry.setOnClickListener { showQuickEntryDialog() }
    }

    /** Observes all ViewModel LiveData streams.
     * isShowingRecent controls whether we display the recent patients list
     * (when query is blank) or the filtered search results (when query is
     * non-blank). The villages map is rebuilt on each emission so that
     * patient village IDs can be resolved to display names. */
    private fun observeViewModel() {
        viewModel.isShowingRecent.observe(viewLifecycleOwner) { isRecent ->
            if (isRecent) {
                adapter.submitList(allPatients)
            }
        }

        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            if (!(viewModel.isShowingRecent.value ?: true)) {
                adapter.submitList(results)
                updateEmptyState(results.isEmpty())
            }
        }

        viewModel.recentPatients.observe(viewLifecycleOwner) { recent ->
            allPatients = recent
            if (viewModel.isShowingRecent.value ?: true) {
                adapter.submitList(recent)
                updateEmptyState(recent.isEmpty())
            }
        }

        viewModel.villages.observe(viewLifecycleOwner) { villages ->
            villagesMap = villages.associate { it.id to it.name }
            adapter.notifyDataSetChanged()
        }

        viewModel.familyGroups.observe(viewLifecycleOwner) { groups ->
            familyGroupsMap = groups.associate { it.id to it.name }
            adapter.notifyDataSetChanged()
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    private fun updateEmptyState(showEmpty: Boolean) {
        binding.emptyStateView.visibility = if (showEmpty) View.VISIBLE else View.GONE
    }

    /** Navigates to PatientDetailFragment by replacing the current fragment
     * in the nav host container. Adds the transaction to the back stack so
     * the user can return to search results. */
    private fun openPatientDetail(patientId: Long) {
        val fragment = PatientDetailFragment.newInstance(patientId)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    /** Shows a dialog for quickly recording a medicine, payment, or adjustment
     * transaction against any known patient without navigating away from search.
     * The patient is selected via an AutoCompleteTextView populated with all
     * loaded patient names. Adjustments show an additional reason field. */
    private fun showQuickEntryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_quick_entry, null)
        val patientInput = dialogView.findViewById<AutoCompleteTextView>(R.id.quickPatientInput)
        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.quickAmountInput)
        val typeGroup = dialogView.findViewById<RadioGroup>(R.id.quickTypeGroup)
        val notesLayout = dialogView.findViewById<View>(R.id.quickNotesLayout)
        val notesInput = dialogView.findViewById<TextInputEditText>(R.id.quickNotesInput)

        val patientNames = allPatients.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, patientNames)
        patientInput.setAdapter(adapter)

        typeGroup.setOnCheckedChangeListener { _, checkedId ->
            notesLayout.visibility = if (checkedId == R.id.quickTypeAdjustment) View.VISIBLE else View.GONE
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.quick_entry_title))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val patientName = patientInput.text.toString().trim()
                val amount = amountInput.text.toString().toDoubleOrNull()

                if (patientName.isBlank()) {
                    Toast.makeText(requireContext(), getString(R.string.select_patient), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val type = when (typeGroup.checkedRadioButtonId) {
                    R.id.quickTypePayment -> "payment"
                    R.id.quickTypeAdjustment -> "adjustment"
                    else -> "medicine"
                }
                val isValid = if (type == "adjustment") amount != null && amount != 0.0 else amount != null && amount > 0
                if (!isValid) {
                    Toast.makeText(requireContext(), getString(R.string.enter_valid_amount), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val patient = allPatients.find { it.name == patientName }
                if (patient == null) {
                    Toast.makeText(requireContext(), getString(R.string.patient_not_found), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val notes = if (type == "adjustment") notesInput.text.toString().trim() else ""

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        viewModel.insertTransaction(
                            Transaction(
                                patientId = patient.id,
                                type = type,
                                amount = amount!!,
                                notes = notes,
                                createdAt = Date()
                            )
                        )
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), getString(R.string.saved), Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
