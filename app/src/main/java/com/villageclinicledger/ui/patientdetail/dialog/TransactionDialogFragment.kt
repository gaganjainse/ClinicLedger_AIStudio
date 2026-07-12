package com.villageclinicledger.ui.patientdetail.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.villageclinicledger.R
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.ui.util.LayoutScaler
import java.util.Date

/**
 * DialogFragment for recording a transaction (medicine, payment, or adjustment).
 *
 * Type-specific behavior:
 * - MEDICINE / PAYMENT: require positive amount, show quick-amount chips, notes optional
 * - ADJUSTMENT: require non-zero amount, show reason field (required), no chips
 *
 * The host fragment must implement [TransactionDialogListener] to receive the result.
 */
class TransactionDialogFragment : DialogFragment() {

    private var _binding: View? = null
    private val binding get() = _binding!!
    private lateinit var listener: TransactionDialogListener
    private var type: TransactionType = TransactionType.MEDICINE
    private var initialBalance: Double = 0.0
    private var patientId: Long = 0

    companion object {
        private const val ARG_TYPE = "type"
        private const val ARG_INITIAL_BALANCE = "initial_balance"
        private const val ARG_PATIENT_ID = "patient_id"

        fun newInstance(type: TransactionType, initialBalance: Double, patientId: Long): TransactionDialogFragment {
            val args = Bundle()
            args.putString(ARG_TYPE, type.name)
            args.putDouble(ARG_INITIAL_BALANCE, initialBalance)
            args.putLong(ARG_PATIENT_ID, patientId)
            return TransactionDialogFragment().apply { arguments = args }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = inflater.inflate(R.layout.dialog_transaction, container, false)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = arguments!!
        type = TransactionType.valueOf(args.getString(ARG_TYPE)!!)
        initialBalance = args.getDouble(ARG_INITIAL_BALANCE)
        patientId = args.getLong(ARG_PATIENT_ID)

        setupView()
        applyScaling()
    }

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        if (parentFragment is TransactionDialogListener) {
            listener = parentFragment as TransactionDialogListener
        } else if (context is TransactionDialogListener) {
            listener = context
        } else {
            throw ClassCastException("Host must implement TransactionDialogListener")
        }
    }

    override fun onDetach() {
        listener = NullListener
        super.onDetach()
    }

    private fun setupView() {
        val (titleRes, showReason, showChips) = when (type) {
            TransactionType.MEDICINE -> R.string.add_medicine to false to true
            TransactionType.PAYMENT -> R.string.record_payment to false to true
            TransactionType.ADJUSTMENT -> R.string.add_adjustment to true to false
        }

        val amountInput = binding.findViewById<TextInputEditText>(R.id.transactionAmountInput)
        val notesInput = binding.findViewById<TextInputEditText>(R.id.transactionNotesInput)
        val reasonLabel = binding.findViewById<TextView>(R.id.reasonLabel)
        val reasonInput = binding.findViewById<TextInputEditText>(R.id.transactionReasonInput)
        val chipsContainer = binding.findViewById<View>(R.id.chipsContainer)

        // Configure reason field visibility
        reasonLabel.visibility = if (showReason) View.VISIBLE else View.GONE
        reasonInput.visibility = if (showReason) View.VISIBLE else View.GONE
        chipsContainer.visibility = if (showChips) View.VISIBLE else View.GONE

        if (showChips) {
            setupChips(amountInput)
        }

        // Live balance preview
        val previewPrevious = binding.findViewById<TextView>(R.id.txtPreviewPrevious)
        val previewNew = binding.findViewById<TextView>(R.id.txtPreviewNew)
        previewPrevious.text = getString(R.string.preview_previous, initialBalance)

        val updatePreview = {
            val amt = amountInput.text.toString().toDoubleOrNull() ?: 0.0
            val newBalance = when (type) {
                TransactionType.MEDICINE -> initialBalance + amt
                TransactionType.PAYMENT -> initialBalance - amt
                TransactionType.ADJUSTMENT -> initialBalance + amt
            }
            previewNew.text = getString(R.string.preview_new, newBalance)
        }

        amountInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePreview()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })
        updatePreview()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(titleRes)
            .setView(binding)
            .setPositiveButton(R.string.save) { _, _ ->
                val amountStr = amountInput.text.toString()
                val amount = amountStr.toDoubleOrNull()
                val isValid = when (type) {
                    TransactionType.ADJUSTMENT -> amount != null && amount != 0.0
                    else -> amount != null && amount > 0
                }
                if (!isValid) {
                    toast(R.string.enter_valid_amount)
                    return@setPositiveButton
                }
                val notes = if (showReason) reasonInput.text.toString() else notesInput.text.toString()
                if (showReason && notes.isBlank()) {
                    toast(R.string.enter_reason)
                    return@setPositiveButton
                }
                listener.onTransactionConfirmed(
                    Transaction(
                        patientId = patientId,
                        type = type.dbValue,
                        amount = amount!!,
                        notes = notes,
                        createdAt = Date()
                    )
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupChips(amountInput: TextInputEditText) {
        val selectAmount = { amt: String ->
            amountInput.setText(amt)
            amountInput.setSelection(amt.length)
        }
        val chips = listOf(
            binding.findViewById<MaterialButton>(R.id.chip100),
            binding.findViewById<MaterialButton>(R.id.chip200),
            binding.findViewById<MaterialButton>(R.id.chip300),
            binding.findViewById<MaterialButton>(R.id.chip500),
            binding.findViewById<MaterialButton>(R.id.chip750),
            binding.findViewById<MaterialButton>(R.id.chip1000)
        )
        val amounts = listOf("100", "200", "300", "500", "750", "1000")
        chips.zip(amounts).forEach { (chip, amt) ->
            chip.setOnClickListener { selectAmount(amt) }
        }
    }

    private fun applyScaling() {
        val amountInput = binding.findViewById<TextInputEditText>(R.id.transactionAmountInput)
        val notesInput = binding.findViewById<TextInputEditText>(R.id.transactionNotesInput)
        val reasonInput = binding.findViewById<TextInputEditText>(R.id.transactionReasonInput)
        LayoutScaler.scaleTextSize(amountInput, 16f)
        LayoutScaler.scaleTextSize(notesInput, 16f)
        LayoutScaler.scaleTextSize(reasonInput, 16f)
    }

    private fun toast(@androidx.annotation.StringRes resId: Int) {
        android.widget.Toast.makeText(requireContext(), resId, android.widget.Toast.LENGTH_SHORT).show()
    }

    /** Callback interface for the host to receive the confirmed transaction. */
    interface TransactionDialogListener {
        fun onTransactionConfirmed(transaction: Transaction)
    }

    private object NullListener : TransactionDialogListener {
        override fun onTransactionConfirmed(transaction: Transaction) {}
    }

    /** Transaction type with string representation matching DB column. */
    enum class TransactionType(val dbValue: String) {
        MEDICINE("medicine"),
        PAYMENT("payment"),
        ADJUSTMENT("adjustment")
    }
}