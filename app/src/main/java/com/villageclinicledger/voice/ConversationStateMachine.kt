package com.villageclinicledger.voice

import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.data.models.Village
import java.util.Date

/**
 * UI-agnostic state machine for the voice conversation flow.
 * Contains NO Android framework dependencies - pure Kotlin.
 */
class ConversationStateMachine(
    private val listener: StateMachineListener
) {

    // ──────────────────────────────────────────────────────────────────────
    // State
    // ──────────────────────────────────────────────────────────────────────

    private var state = ConversationState.IDLE
    private var currentIntent: ParsedVoiceIntent? = null
    private var matchedPatient: Patient? = null
    private var pendingTransaction: Transaction? = null
    private var lastTranscription = ""
    private var ambiguousPatients: List<Patient>? = null

    // ──────────────────────────────────────────────────────────────────────
    // Public API - Events from UI/speech layer
    // ──────────────────────────────────────────────────────────────────────

    /** Called when SpeechRecognizer reports ready-for-speech */
    fun onListeningStarted() {
        transitionTo(ConversationState.LISTENING)
    }

    /** Called when SpeechRecognizer returns final transcription */
    fun onTranscriptionReceived(text: String, parser: VoiceIntentParser, villages: List<Village>) {
        lastTranscription = text
        val parsed = parser.parse(text, villages)
        currentIntent = parsed
        onIntentParsed(parsed)
    }

    /** Called when SpeechRecognizer returns partial results (for live display) */
    fun onPartialTranscription(text: String) {
        listener.onUpdateRecognizedText(text)
    }

    /** Called when SpeechRecognizer reports an error */
    fun onRecognitionError(errorCode: Int) {
        val msg = when (errorCode) {
            android.speech.SpeechRecognizer.ERROR_NO_MATCH -> "नहीं सुन पाया"
            android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "कुछ नहीं बोला"
            else -> "गड़बड़ी: $errorCode"
        }
        listener.onUpdateRecognizedText(msg)

        if (state == ConversationState.CONFIRMING) {
            // Stay in confirming, just show error briefly
            return
        }
        transitionTo(ConversationState.ERROR)
        listener.onSpeak(msg) { retryListening() }
    }

    /** Called when user taps Yes/Confirm button */
    fun onUserConfirmed() {
        when (state) {
            ConversationState.CONFIRMING -> {
                val intent = currentIntent?.intent ?: return
                when (intent) {
                    IntentType.SEARCH_BALANCE -> {
                        matchedPatient?.let { listener.onNavigateToPatient(it.id) }
                        transitionTo(ConversationState.DONE)
                    }
                    IntentType.MEDICINE, IntentType.PAYMENT, IntentType.MEDICINE_AND_PAYMENT,
                    IntentType.NEW_PATIENT, IntentType.CORRECTION -> {
                        transitionTo(ConversationState.SAVING)
                        listener.onSaveTransaction(
                            intent = intent,
                            patient = matchedPatient,
                            amount = currentIntent?.medicineAmount,
                            paymentAmount = currentIntent?.paymentAmount,
                            patientName = currentIntent?.patientName,
                            villageName = currentIntent?.villageName
                        )
                    }
                    else -> transitionTo(ConversationState.DONE)
                }
            }
            ConversationState.LISTENING -> stopListening()
            ConversationState.ERROR -> {
                transitionTo(ConversationState.LISTENING)
                listener.onStartListening()
            }
        }
    }

    /** Called when user taps No/Reject button */
    fun onUserRejected() {
        when (state) {
            ConversationState.CONFIRMING -> {
                clearPending()
                transitionTo(ConversationState.LISTENING)
                listener.onSpeak("ठीक है, फिर से बोलिए") { listener.onStartListening() }
            }
            ConversationState.LISTENING -> stopListening()
        }
    }

    /** Called when disambiguation selects a patient */
    fun onDisambiguationSelected(patient: Patient, originalIntent: ParsedVoiceIntent) {
        matchedPatient = patient
        ambiguousPatients = null
        currentIntent = originalIntent
        when (originalIntent.intent) {
            IntentType.SEARCH_BALANCE -> handleBalanceQuery(originalIntent)
            IntentType.MEDICINE -> handleMedicineEntry(originalIntent)
            IntentType.PAYMENT -> handlePaymentEntry(originalIntent)
            IntentType.MEDICINE_AND_PAYMENT -> handleMedicineAndPayment(originalIntent)
            else -> showBalanceCard(patient)
        }
    }

    /** Called when database save completes successfully */
    fun onSaveSucceeded() {
        transitionTo(ConversationState.DONE)
        listener.onSpeak("सेव हो गया") {
            listener.onDismiss()
        }
    }

    /** Called when database save fails */
    fun onSaveFailed(error: String) {
        transitionTo(ConversationState.ERROR)
        listener.onSpeak("गड़बड़ी हो गई") { retryListening() }
        listener.onToast("Error: $error")
    }

    // ──────────────────────────────────────────────────────────────────────
    // Internal - Intent-specific handlers
    // ──────────────────────────────────────────────────────────────────────

    private fun onIntentParsed(parsed: ParsedVoiceIntent) {
        when (parsed.intent) {
            IntentType.CONFIRM_YES -> onUserConfirmed()
            IntentType.CONFIRM_NO -> onUserRejected()
            IntentType.SEARCH_BALANCE -> handleBalanceQuery(parsed)
            IntentType.MEDICINE -> handleMedicineEntry(parsed)
            IntentType.PAYMENT -> handlePaymentEntry(parsed)
            IntentType.MEDICINE_AND_PAYMENT -> handleMedicineAndPayment(parsed)
            IntentType.NEW_PATIENT -> handleNewPatient(parsed)
            IntentType.CORRECTION -> handleCorrection()
            IntentType.UNKNOWN -> {
                transitionTo(ConversationState.ERROR)
                listener.onSpeak("समझ नहीं आया, फिर से बोलिए") { retryListening() }
            }
        }
    }

    private suspend fun handleBalanceQuery(parsed: ParsedVoiceIntent) {
        val name = parsed.patientName ?: VoiceIntentParser.extractPatientName(lastTranscription)
        val patient = if (name != null) listener.findPatient(name) else null
        if (patient != null) {
            matchedPatient = patient
            showBalanceCard(patient)
        } else {
            transitionTo(ConversationState.ERROR)
            val msg = if (name != null) "$name नहीं मिले" else "कौन सा रोगी?"
            listener.onSpeak(msg) { retryListening() }
        }
    }

    private suspend fun handleMedicineEntry(parsed: ParsedVoiceIntent) {
        val name = parsed.patientName
        val patient = if (name != null) listener.findPatient(name) else null
        if (patient == null) {
            transitionTo(ConversationState.ERROR)
            val msg = if (name != null) "$name नहीं मिले" else "कौन सा रोगी?"
            listener.onSpeak(msg) { retryListening() }
            return
        }
        matchedPatient = patient
        val amount = parsed.medicineAmount
        if (amount == null || amount <= 0) {
            transitionTo(ConversationState.LISTENING)
            listener.onSpeak("${patient.name} के लिए कितने की दवा?") { listener.onStartListening() }
            return
        }
        pendingTransaction = Transaction(
            patientId = patient.id, type = "medicine", amount = amount, createdAt = Date()
        )
        val newBalance = patient.currentBalance + amount
        showConfirmationCard(
            title = "${patient.name} — दवा",
            detail = "दवा: ₹${amount.toLong()}, पिछला बकाया: ₹${patient.currentBalance.toLong()}",
            balance = "नया बकाया: ₹${newBalance.toLong()}",
            spoken = "दवा ${amount.toLong()} रुपये, नया बकाया ${newBalance.toLong()} रुपये। क्या सही है?"
        )
    }

    private suspend fun handlePaymentEntry(parsed: ParsedVoiceIntent) {
        val name = parsed.patientName
        val patient = if (name != null) listener.findPatient(name) else null
        if (patient == null) {
            transitionTo(ConversationState.ERROR)
            val msg = if (name != null) "$name नहीं मिले" else "कौन सा रोगी?"
            listener.onSpeak(msg) { retryListening() }
            return
        }
        matchedPatient = patient
        val amount = parsed.paymentAmount ?: parsed.medicineAmount
        if (amount == null || amount <= 0) {
            transitionTo(ConversationState.LISTENING)
            listener.onSpeak("${patient.name} से कितने पैसे मिले?") { listener.onStartListening() }
            return
        }
        pendingTransaction = Transaction(
            patientId = patient.id, type = "payment", amount = amount, createdAt = Date()
        )
        val newBalance = patient.currentBalance - amount
        showConfirmationCard(
            title = "${patient.name} — भुगतान",
            detail = "जमा: ₹${amount.toLong()}, पिछला बकाया: ₹${patient.currentBalance.toLong()}",
            balance = "नया बकाया: ₹${maxOf(0.0, newBalance).toLong()}",
            spoken = "भुगतान ${amount.toLong()} रुपये, नया बकाया ${maxOf(0.0, newBalance).toLong()} रुपये। क्या सही है?"
        )
    }

    private suspend fun handleMedicineAndPayment(parsed: ParsedVoiceIntent) {
        val name = parsed.patientName
        val patient = if (name != null) listener.findPatient(name) else null
        if (patient == null) {
            transitionTo(ConversationState.ERROR)
            val msg = if (name != null) "$name नहीं मिले" else "कौन सा रोगी?"
            listener.onSpeak(msg) { retryListening() }
            return
        }
        matchedPatient = patient
        val medAmount = parsed.medicineAmount ?: 0.0
        val payAmount = parsed.paymentAmount ?: 0.0
        if (medAmount <= 0 && payAmount <= 0) {
            transitionTo(ConversationState.LISTENING)
            listener.onSpeak("${patient.name} के लिए कितने की दवा और कितने पैसे दिए?") { listener.onStartListening() }
            return
        }
        val newBalance = patient.currentBalance + medAmount - payAmount
        val detail = buildString {
            if (medAmount > 0) append("दवा: ₹${medAmount.toLong()}, ")
            if (payAmount > 0) append("जमा: ₹${payAmount.toLong()}, ")
            append("पिछला: ₹${patient.currentBalance.toLong()}")
        }
        showConfirmationCard(
            title = "${patient.name} — दवा + भुगतान",
            detail = detail,
            balance = "नया बकाया: ₹${maxOf(0.0, newBalance).toLong()}",
            spoken = "दवा ${medAmount.toLong()}, जमा ${payAmount.toLong()}, नया बकाया ${maxOf(0.0, newBalance).toLong()}। क्या सही है?"
        )
    }

    private suspend fun handleNewPatient(parsed: ParsedVoiceIntent) {
        val name = parsed.patientName ?: run {
            transitionTo(ConversationState.ERROR)
            listener.onSpeak("नए रोगी का नाम नहीं सुन पाया") { retryListening() }
            return
        }
        val existing = listener.findPatient(name)
        if (existing != null) {
            matchedPatient = existing
            showBalanceCard(existing)
            return
        }
        val village = parsed.villageName ?: "Siras"
        showConfirmationCard(
            title = "नया रोगी: $name",
            detail = "गाँव: $village${if (parsed.medicineAmount != null) ", दवा: ₹${parsed.medicineAmount!!.toLong()}" else ""}",
            balance = if (parsed.medicineAmount != null) "बकाया: ₹${parsed.medicineAmount!!.toLong()}" else "कोई दवा नहीं",
            spoken = "नया रोगी $name, गाँव $village${if (parsed.medicineAmount != null) ", दवा ${parsed.medicineAmount!!.toLong()}" else ""}। जोड़ दें?"
        )
    }

    private suspend fun handleCorrection() {
        val lastTx = listener.getLastTransaction()
        if (lastTx == null) {
            transitionTo(ConversationState.ERROR)
            listener.onSpeak("कोई पिछली एंट्री नहीं मिली") { retryListening() }
            return
        }
        val patient = listener.getPatientByIdSync(lastTx.patientId)
        if (patient == null) {
            transitionTo(ConversationState.ERROR)
            listener.onSpeak("पिछली एंट्री का रोगी नहीं मिला") { retryListening() }
            return
        }
        matchedPatient = patient
        val revAmount = if (lastTx.type == "payment") lastTx.amount else -lastTx.amount
        pendingTransaction = Transaction(
            patientId = patient.id,
            type = "adjustment",
            amount = revAmount,
            notes = "Reversal of ${lastTx.type} (ID ${lastTx.id})",
            createdAt = Date()
        )
        val newBalance = patient.currentBalance + revAmount
        val typeText = when (lastTx.type) {
            "medicine" -> "दवा"
            "payment" -> "भुगतान"
            "adjustment" -> "समायोजन"
            else -> lastTx.type
        }
        showConfirmationCard(
            title = "${patient.name} — सुधार",
            detail = "रद्द: $typeText ₹${lastTx.amount.toLong()}",
            balance = "बकाया पुनः स्थापित: ₹${maxOf(0.0, newBalance).toLong()}",
            spoken = "पिछली एंट्री ${patient.name} की $typeText ${lastTx.amount.toLong()} रुपये रद्द कर रहा हूँ। नया बकाया ${newBalance.toLong()} रुपये। क्या सही है?"
        )
    }

    private fun showBalanceCard(patient: Patient) {
        transitionTo(ConversationState.CONFIRMING)
        val balanceHindi = HindiNumberConverter.convertShort(patient.currentBalance)
        listener.onUpdateConfirmationCard(
            title = patient.name,
            detail = "गाँव: ${listener.getVillageName(patient.villageId)}",
            balanceAmount = "बकाया: ₹${patient.currentBalance.toLong()}"
        )
        val spoken = "${patient.name} पर $balanceHindi रुपये बकाया हैं। क्या यह सही है?"
        listener.onSpeak(spoken)
        listener.onSetConfirmButtons(
            yesText = "खोलें",
            onYes = { listener.onNavigateToPatient(patient.id) },
            noText = "वापस",
            onNo = { listener.onDismiss() }
        )
    }

    private fun showConfirmationCard(title: String, detail: String, balance: String, spoken: String) {
        transitionTo(ConversationState.CONFIRMING)
        listener.onUpdateConfirmationCard(title, detail, balance)
        listener.onSpeak(spoken)
        listener.onSetConfirmButtons(
            yesText = "हाँ (सही है)",
            onYes = { onUserConfirmed() },
            noText = "नहीं (बदलो)",
            onNo = { onUserRejected() }
        )
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────

    private fun transitionTo(newState: ConversationState) {
        state = newState
        listener.onStateChanged(newState)
    }

    private fun clearPending() {
        currentIntent = null
        matchedPatient = null
        pendingTransaction = null
        ambiguousPatients = null
    }

    private fun retryListening() {
        transitionTo(ConversationState.LISTENING)
        listener.onStartListening()
    }

    private fun stopListening() {
        listener.onStopListening()
    }
}

/**
 * All possible states in the voice conversation flow.
 */
enum class ConversationState {
    IDLE,
    LISTENING,
    PROCESSING,
    CONFIRMING,
    SAVING,
    DONE,
    ERROR
}

/**
 * Callback interface that the UI layer (Fragment/Activity) implements
 * to receive state machine outputs. Keeps the state machine framework-agnostic.
 */
interface StateMachineListener {
    fun onStateChanged(state: ConversationState)

    // Speech recognition
    fun onStartListening()
    fun onStopListening()

    // TTS
    fun onSpeak(text: String, onComplete: () -> Unit = {})

    // UI updates
    fun onUpdateRecognizedText(text: String)
    fun onUpdateConfirmationCard(title: String, detail: String, balanceAmount: String)
    fun onSetConfirmButtons(
        yesText: String,
        onYes: () -> Unit,
        noText: String,
        onNo: () -> Unit
    )
    fun onToast(message: String)

    // Navigation
    fun onNavigateToPatient(patientId: Long)
    fun onDismiss()

    // Database queries (implemented by repository wrapper)
    fun findPatient(name: String): Patient?
    fun getLastTransaction(): Transaction?
    fun getPatientByIdSync(id: Long): Patient?
    fun getVillageName(villageId: Long): String
}