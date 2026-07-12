package com.villageclinicledger.ui.patientdetail.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.villageclinicledger.R
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.ui.util.LayoutScaler
import java.util.Date

/**
 * Dialog for recording a transaction (medicine, payment, or adjustment).
 *
 * Usage: create via [newInstance], show with FragmentManager, implement
 * [TransactionDialogListener] in the host Fragment/Activity to receive result.
 */
class TransactionDialogFragment : DialogFragment() {

    /** Enum defining the three supported transaction types. */
    enum class TransactionType {
        MEDICINE,
        PAYMENT,
        ADJUSTMENT
    }

    /** Callback interface for the host to receive the confirmed transaction. */
    interface TransactionDialogListener {
        fun onTransactionConfirmed(transaction: Transaction)
    }

    // ──────────────────────────────────────────────────────────────────────
    // Arguments
    // ──────────────────────────────────────────────────────────────────────

    companion object {
        private const val ARG_TYPE = "type"
        private const val ARG_INITIAL_BALANCE = "initial_balance"
        private const val ARG_PATIENT_ID = "patient_id"

        /** Factory method to create a configured instance. */
        fun newInstance(
            type: TransactionType,
            initialBalance: Double,
            patientId: Long
        ): TransactionDialogFragment {
            val fragment = TransactionDialogFragment()
            val args = Bundle()
            args.putString(ARG_TYPE, type.name)
            args.putDouble(ARG_INITIAL_BALANCE, initialBalance)
            args.putLong(ARG_PATIENT_ID, patientId)
            fragment.arguments = args
            return fragment
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // State
    // ──────────────────────────────────────────────────────────────────────

    private var type: TransactionType = TransactionType.MEDICINE
    private var initialBalance = 0.0
    private var patientId = 0L
    private var listener: TransactionDialogListener? = null

    // ──────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ──────────────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments ?: return
        type = TransactionType.valueOf(args.getString(ARG_TYPE) ?: "MEDICINE")
        initialBalance = args.getDouble(ARG_INITIAL_BALANCE)
        patientId = args.getLong(ARG_PATIENT_ID)
    }

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        if (parentFragment is TransactionDialogListener) {
            listener = parentFragment as TransactionDialogListener
        } else if (activity is TransactionDialogListener) {
            listener = activity as TransactionDialogListener
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    // ──────────────────────────────────────────────────────────────────────
    // Dialog construction
    // ──────────────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_transaction, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = when (type) {
            TransactionType.MEDICINE -> getString(R.string.add_medicine)
            TransactionType.PAYMENT -> getString(R.string.record_payment)
            TransactionType.ADJUSTMENT -> getString(R.string.add_adjustment)
        }

        val amountInput = view.findViewById<TextInputEditText>(R.id.transactionAmountInput)
        val notesInput = view.findViewById<TextInputEditText>(R.id.transactionNotesInput)
        val reasonLabel = view.findViewById<TextView>(R.id.reasonLabel)
        val reasonInput = view.findViewById<TextInputEditText>(R.id.transactionReasonInput)
        val chipsContainer = view.findViewById<View>(R.id.chipsContainer)

        // Quick-amount chips
        val chip100 = view.findViewById<MaterialButton>(R.id.chip100)
        val chip200 = view.findViewById<MaterialButton>(R.id.chip200)
        val chip300 = view.findViewById<MaterialButton>(R.id.chip300)
        val chip500 = view.findViewById<MaterialButton>(R.id.chip500)
        val chip750 = view.findViewById<MaterialButton>(R.id.chip750)
        val chip1000 = view.findViewById<MaterialButton>(R.id.chip1000)

        // Preview balance card
        val previewPrevious = view.findViewById<TextView>(R.id.txtPreviewPrevious)
        val previewNew = view.findViewById<TextView>(R.id.txtPreviewNew)

        LayoutScaler.scaleTextSize(amountInput, 16f)
        LayoutScaler.scaleTextSize(notesInput, 16f)
        LayoutScaler.scaleTextSize(reasonInput, 16f)

        // Adjustment requires a reason; others use notes + quick chips
        if (type == TransactionType.ADJUSTMENT) {
            reasonLabel.visibility = View.VISIBLE
            reasonInput.visibility = View.VISIBLE
            chipsContainer.visibility = View.GONE
        } else {
            reasonLabel.visibility = View.GONE
            reasonInput.visibility = View.GONE
            chipsContainer.visibility = View.VISIBLE

            val setAmount = { amt: String ->
                amountInput.setText(amt)
                amountInput.setSelection(amt.length)
            }
            chip100.setOnClickListener { setAmount("100") }
            chip200.setOnClickListener { setAmount("200") }
            chip300.setOnClickListener { setAmount("300") }
            chip500.setOnClickListener { setAmount("500") }
            chip750.setOnClickListener { setAmount("750") }
            chip1000.setOnClickListener { setAmount("1000") }
        }

        // Live Balance Preview
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
            .setView(view)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val amount = amountInput.text.toString().toDoubleOrNull()
                val isValid = if (type == TransactionType.ADJUSTMENT) {
                    amount != null && amount != 0.0
                } else {
                    amount != null && amount > 0
                }
                if (isValid) {
                    val notes = if (type == TransactionType.ADJUSTMENT) {
                        reasonInput.text.toString()
                    } else {
                        notesInput.text.toString()
                    }
                    val transaction = Transaction(
                        patientId = patientId,
                        type = type.name.lowercase(),
                        amount = amount!!,
                        notes = notes,
                        createdAt = Date()
                    )
                    listener?.onTransactionConfirmed(transaction)
                } else {
                    val msg = if (type == TransactionType.ADJUSTMENT) {
                        getString(R.string.enter_reason)
                    } else {
                        getString(R.string.enter_valid_amount)
                    }
                    androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setMessage(msg)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}