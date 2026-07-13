package com.villageclinicledger.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.villageclinicledger.R
import com.villageclinicledger.data.models.Alias
import java.util.Date
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.data.models.Village
import com.villageclinicledger.ui.patientdetail.adapter.AliasAdapter
import com.villageclinicledger.ui.patientdetail.adapter.TransactionAdapter
import com.villageclinicledger.ui.patientdetail.viewmodel.PatientDetailViewModel
import com.villageclinicledger.databinding.FragmentPatientDetailBinding
import com.villageclinicledger.ui.util.LayoutScaler

/** Displays a single patient's details: name, village, phone, current balance,
 * a list of aliases (with long-press to delete), and a chronological list of
 * transactions. Provides buttons to add medicine charges, record payments, and
 * post adjustments (with a required reason field). */
class PatientDetailFragment : Fragment() {

    private var _binding: FragmentPatientDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: PatientDetailViewModel
    private lateinit var aliasAdapter: AliasAdapter
    private lateinit var transactionAdapter: TransactionAdapter

    companion object {
        private const val ARG_PATIENT_ID = "patient_id"

        /** Factory method that creates a fragment instance with the patient ID
         * passed as a safe-args-style bundle argument. */
        fun newInstance(patientId: Long): PatientDetailFragment {
            val fragment = PatientDetailFragment()
            val args = Bundle()
            args.putLong(ARG_PATIENT_ID, patientId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPatientDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this)[PatientDetailViewModel::class.java]

        setupToolbar()
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
        applyScaling()

        val patientId = arguments?.getLong(ARG_PATIENT_ID) ?: 0
        if (patientId > 0) {
            // Setting the patientId triggers the ViewModel's switchMap
            // transformations, which in turn load the patient details,
            // alias list, and transaction list from the repository.
            viewModel.loadPatient(patientId)
        }
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressed()
        }
    }

    private fun setupRecyclerViews() {
        // Alias items are clickable — we trigger a delete confirmation dialog
        aliasAdapter = AliasAdapter { alias ->
            showDeleteAliasDialog(alias)
        }
        binding.aliasesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.aliasesRecyclerView.adapter = aliasAdapter

        transactionAdapter = TransactionAdapter()
        binding.transactionsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.transactionsRecyclerView.adapter = transactionAdapter
    }

    private fun setupClickListeners() {
        binding.btnMedicine.setOnClickListener { showTransactionDialog("medicine") }
        binding.btnPayment.setOnClickListener { showTransactionDialog("payment") }
        binding.btnAdjustment.setOnClickListener { showTransactionDialog("adjustment") }
        binding.btnAddAlias.setOnClickListener { showAddAliasDialog() }
    }

    /** Cached village list, refreshed whenever villages LiveData emits.
     * Used to resolve villageId → display name in updateUI. */
    private var villagesList: List<Village> = emptyList()

    private fun observeViewModel() {
        viewModel.patient.observe(viewLifecycleOwner) { patient ->
            if (patient != null) {
                updateUI(patient)
                viewModel.setFamilyGroupId(patient.familyGroupId)
            } else {
                binding.familyGroupSection.visibility = View.GONE
            }
        }

        viewModel.villages.observe(viewLifecycleOwner) { villages ->
            villagesList = villages
            // Re-render patient info now that we can resolve the village name
            viewModel.patient.value?.let { updateUI(it) }
        }

        viewModel.familyGroup.observe(viewLifecycleOwner) { group ->
            if (group != null) {
                binding.familyGroupSection.visibility = View.VISIBLE
                binding.familyGroupName.text = String.format(getString(R.string.family_label), group.name)
            } else {
                binding.familyGroupSection.visibility = View.GONE
            }
        }

        viewModel.familyMembers.observe(viewLifecycleOwner) { members ->
            val currentPatient = viewModel.patient.value
            val otherMembers = members.filter { it.id != currentPatient?.id }
            if (otherMembers.isNotEmpty()) {
                binding.familyMembersSection.visibility = View.VISIBLE
                binding.familyMembersSection.text = otherMembers.joinToString("\n") {
                    "${it.name} - ${it.formattedBalance}"
                }
            } else {
                binding.familyMembersSection.visibility = View.GONE
            }
        }

        viewModel.aliases.observe(viewLifecycleOwner) { aliases ->
            aliasAdapter.submitList(aliases)
        }

        viewModel.transactions.observe(viewLifecycleOwner) { transactions ->
            transactionAdapter.submitList(transactions)
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.clearError()
            }
        }
    }

    /** Populates the patient info header (name, village, phone, balance) from
     * the given Patient model. Resolves the numeric villageId to a display name
     * using the cached villagesList. */
    private fun updateUI(patient: Patient) {
        binding.patientName.text = patient.name
        val villageName = villagesList.find { it.id == patient.villageId }?.name ?: ""
        binding.patientVillage.text = villageName
        binding.patientPhone.text = patient.phone.ifBlank { "" }
        binding.currentBalance.text = patient.formattedBalance
    }

    /** Shows a dialog for recording a transaction of the given type.
     * The "adjustment" type additionally shows a reason field and allows
     * zero or negative amounts, while "medicine" and "payment" require
     * a positive amount. */
    private fun showTransactionDialog(type: String) {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_transaction, null)
        val title = when (type) {
            "medicine" -> getString(R.string.add_medicine)
            "payment" -> getString(R.string.record_payment)
            "adjustment" -> getString(R.string.add_adjustment)
            else -> getString(R.string.transaction_default)
        }

        val amountInput = dialogView.findViewById<TextInputEditText>(R.id.transactionAmountInput)
        val notesInput = dialogView.findViewById<TextInputEditText>(R.id.transactionNotesInput)
        val reasonLabel = dialogView.findViewById<TextView>(R.id.reasonLabel)
        val reasonInput = dialogView.findViewById<TextInputEditText>(R.id.transactionReasonInput)

        val chip100 = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.chip100)
        val chip200 = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.chip200)
        val chip300 = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.chip300)
        val chip500 = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.chip500)
        val chip750 = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.chip750)
        val chip1000 = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.chip1000)
        val chipsContainer = dialogView.findViewById<android.view.View>(R.id.chipsContainer)

        LayoutScaler.scaleTextSize(amountInput, 16f)
        LayoutScaler.scaleTextSize(notesInput, 16f)
        LayoutScaler.scaleTextSize(reasonInput, 16f)

        // Only adjustments require a reason — medicine charges and payments
        // use the generic notes field instead.
        if (type == "adjustment") {
            reasonLabel.visibility = View.VISIBLE
            reasonInput.visibility = View.VISIBLE
            chipsContainer.visibility = View.GONE
        } else {
            reasonLabel.visibility = View.GONE
            reasonInput.visibility = View.GONE
            chipsContainer.visibility = View.VISIBLE
            val selectAmount = { amt: String ->
                amountInput.setText(amt)
                amountInput.setSelection(amt.length)
            }
            chip100.setOnClickListener { selectAmount("100") }
            chip200.setOnClickListener { selectAmount("200") }
            chip300.setOnClickListener { selectAmount("300") }
            chip500.setOnClickListener { selectAmount("500") }
            chip750.setOnClickListener { selectAmount("750") }
            chip1000.setOnClickListener { selectAmount("1000") }
        }

        // Live Balance Preview
        val previewPrevious = dialogView.findViewById<TextView>(R.id.txtPreviewPrevious)
        val previewNew = dialogView.findViewById<TextView>(R.id.txtPreviewNew)
        
        val initialBalance = viewModel.patient.value?.currentBalance ?: 0.0
        previewPrevious.text = String.format("पहले का: ₹%,.2f", initialBalance)
        
        val updatePreview = {
            val amt = amountInput.text.toString().toDoubleOrNull() ?: 0.0
            val newBalance = when (type) {
                "medicine" -> initialBalance + amt
                "payment" -> initialBalance - amt
                "adjustment" -> initialBalance + amt
                else -> initialBalance
            }
            previewNew.text = String.format("नया बकाया: ₹%,.2f", newBalance)
        }
        updatePreview()

        amountInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePreview()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val amount = amountInput.text.toString().toDoubleOrNull()
                // Adjustments can be positive (credit) or negative (debit) but not zero;
                // medicine and payments must always be positive.
                val isValid = if (type == "adjustment") amount != null && amount != 0.0 else amount != null && amount > 0
                if (isValid) {
                    val patient = viewModel.patient.value ?: return@setPositiveButton
                    val notes = if (type == "adjustment") {
                        reasonInput.text.toString()
                    } else {
                        notesInput.text.toString()
                    }

                    viewModel.addTransaction(
                        Transaction(
                            patientId = patient.id,
                            type = type,
                            amount = amount!!,
                            notes = notes,
                            createdAt = Date()
                        )
                    )
                    Toast.makeText(requireContext(), String.format(getString(R.string.transaction_saved), title), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), getString(R.string.enter_valid_amount), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /** Shows a dialog prompting the user to enter an alternative name (alias)
     * for the current patient. Aliases are useful for finding patients who
     * may be registered under different name spellings. */
    private fun showAddAliasDialog() {
        val editText = TextInputEditText(requireContext())
        editText.hint = getString(R.string.alias_hint)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.add_alias_title))
            .setView(editText)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                val aliasText = editText.text.toString().trim()
                if (aliasText.isNotBlank()) {
                    val patient = viewModel.patient.value
                    if (patient != null) {
                        viewModel.addAlias(Alias(patientId = patient.id, alias = aliasText))
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /** Confirmation dialog before removing an alias. The actual deletion is
     * delegated to the ViewModel which runs it on a background coroutine. */
    private fun showDeleteAliasDialog(alias: Alias) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_alias_title))
            .setMessage(getString(R.string.delete_alias_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                viewModel.deleteAlias(alias)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun applyScaling() {
        val context = requireContext()
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val scaleX = screenWidth.toFloat() / 1080f
        val scaleY = screenHeight.toFloat() / 2400f

        val statusBarHeight = (84 * scaleY).toInt()
        val appBarHeight = (96 * scaleY).toInt()

        // 1. Toolbar height & top margin
        val toolbarLp = binding.toolbar.layoutParams as? ViewGroup.MarginLayoutParams
        toolbarLp?.let {
            it.height = appBarHeight
            it.topMargin = statusBarHeight
            binding.toolbar.layoutParams = it
        }

        // 2. Action buttons (Medicine, Payment, Adjustment)
        val btnHeight = (160 * scaleY).toInt()
        binding.btnMedicine.layoutParams?.height = btnHeight
        binding.btnPayment.layoutParams?.height = btnHeight
        binding.btnAdjustment.layoutParams?.height = btnHeight

        // 3. Typography scaling
        LayoutScaler.scaleTextSize(binding.patientName, 26f)
        LayoutScaler.scaleTextSize(binding.patientVillage, 12f)
        LayoutScaler.scaleTextSize(binding.patientPhone, 14f)
        LayoutScaler.scaleTextSize(binding.currentBalance, 32f)
        LayoutScaler.scaleTextSize(binding.familyGroupName, 18f)
        LayoutScaler.scaleTextSize(binding.familyMembersSection, 14f)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
