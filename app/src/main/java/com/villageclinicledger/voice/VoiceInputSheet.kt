package com.villageclinicledger.voice

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.villageclinicledger.R
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.data.repository.PatientRepository
import com.villageclinicledger.ui.PatientDetailFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

enum class ConversationState {
    IDLE, LISTENING, PROCESSING, CONFIRMING, SAVING, DONE, ERROR
}

class VoiceInputSheet : BottomSheetDialogFragment() {

    private var speechRecognizer: SpeechRecognizer? = null
    private var ttsManager: VoiceTtsManager? = null
    private lateinit var repository: PatientRepository
    private var state = ConversationState.IDLE
    private var currentIntent: ParsedVoiceIntent? = null
    private var matchedPatient: Patient? = null
    private var pendingTransaction: Transaction? = null
    private var transcription = ""
    private var correctEntryId: Long? = null
    private var villagesMap: Map<Long, String> = emptyMap()
    private var ambiguousPatients: List<Patient>? = null
    private var allVillagesList: List<com.villageclinicledger.data.models.Village> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_voice_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = PatientRepository(requireContext())
        ttsManager = VoiceTtsManager(requireContext())
        repository.getAllVillages().observe(viewLifecycleOwner) { villages ->
            allVillagesList = villages
            villagesMap = villages.associate { it.id to it.name }
        }
        setupMicButton()
        setState(ConversationState.LISTENING)
        startListening()
    }

    private fun setState(newState: ConversationState) {
        state = newState
        val view = view ?: return
        val micPulse = view.findViewById<View>(R.id.micPulse)
        val voiceStatus = view.findViewById<TextView>(R.id.voiceStatusText)
        val recognizedText = view.findViewById<TextView>(R.id.recognizedSpeech)
        val confirmCard = view.findViewById<MaterialCardView>(R.id.confirmationCard)
        val confirmButtons = view.findViewById<View>(R.id.confirmButtons)
        val holdHint = view.findViewById<TextView>(R.id.holdHint)
        val progress = view.findViewById<View>(R.id.voiceProgress)
        val micBtn = view.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.voiceMicButton)

        micPulse.clearAnimation()

        when (newState) {
            ConversationState.IDLE -> {
                voiceStatus.text = getString(R.string.voice_listening_hint)
                recognizedText.visibility = View.GONE
                confirmCard.visibility = View.GONE
                confirmButtons.visibility = View.GONE
                holdHint.visibility = View.GONE
                progress.visibility = View.GONE
            }
            ConversationState.LISTENING -> {
                voiceStatus.text = "सुन रहा हूँ... बोलिए"
                recognizedText.visibility = View.VISIBLE
                recognizedText.text = "..."
                confirmCard.visibility = View.GONE
                confirmButtons.visibility = View.GONE
                holdHint.visibility = View.GONE
                progress.visibility = View.VISIBLE
                micPulse.setBackgroundResource(R.drawable.circle_pulse)
                micPulse.startAnimation(AnimationUtils.loadAnimation(requireContext(), R.anim.pulse))
            }
            ConversationState.PROCESSING -> {
                voiceStatus.text = "समझ रहा हूँ..."
                recognizedText.visibility = View.VISIBLE
                confirmCard.visibility = View.GONE
                confirmButtons.visibility = View.GONE
                holdHint.visibility = View.GONE
                progress.visibility = View.VISIBLE
            }
            ConversationState.CONFIRMING -> {
                voiceStatus.text = getString(R.string.voice_confirm_question)
                recognizedText.visibility = View.VISIBLE
                confirmCard.visibility = View.VISIBLE
                confirmButtons.visibility = View.VISIBLE
                holdHint.visibility = View.VISIBLE
                progress.visibility = View.GONE
                micPulse.setBackgroundResource(0)
                setupHoldListening()
            }
            ConversationState.SAVING -> {
                voiceStatus.text = "सेव हो रहा है..."
                recognizedText.visibility = View.VISIBLE
                confirmCard.visibility = View.VISIBLE
                confirmButtons.visibility = View.GONE
                holdHint.visibility = View.GONE
                progress.visibility = View.VISIBLE
            }
            ConversationState.DONE -> {
                voiceStatus.text = "✅ सेव हो गया"
                recognizedText.visibility = View.VISIBLE
                confirmCard.visibility = View.VISIBLE
                confirmButtons.visibility = View.GONE
                holdHint.visibility = View.GONE
                progress.visibility = View.GONE
            }
            ConversationState.ERROR -> {
                voiceStatus.text = "समझ नहीं आया, फिर से बोलिए"
                recognizedText.visibility = View.VISIBLE
                confirmCard.visibility = View.GONE
                confirmButtons.visibility = View.GONE
                holdHint.visibility = View.GONE
                progress.visibility = View.GONE
            }
        }
    }

    private fun setupMicButton() {
        val micBtn = view?.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.voiceMicButton)
        micBtn?.setOnClickListener {
            when (state) {
                ConversationState.LISTENING -> stopListening()
                ConversationState.CONFIRMING -> startListening()
                ConversationState.ERROR -> {
                    setState(ConversationState.LISTENING)
                    startListening()
                }
                else -> {}
            }
        }
        view?.findViewById<MaterialButton>(R.id.btnConfirmYes)?.setOnClickListener { onConfirmYes() }
        view?.findViewById<MaterialButton>(R.id.btnConfirmNo)?.setOnClickListener { onConfirmNo() }
    }

    private fun startListening() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bolein")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
            }
        }
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                setState(ConversationState.LISTENING)
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                setState(ConversationState.PROCESSING)
            }
            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "नहीं सुन पाया"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "कुछ नहीं बोला"
                    else -> "गड़बड़ी: $error"
                }
                updateRecognizedText(msg)
                if (state == ConversationState.CONFIRMING) {
                    setState(ConversationState.CONFIRMING)
                    return
                }
                setState(ConversationState.ERROR)
                delayThen { setState(ConversationState.LISTENING); startListening() }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    transcription = matches[0]
                    updateRecognizedText(transcription)
                    if (ambiguousPatients != null) {
                        handleDisambiguation(transcription)
                    } else if (state == ConversationState.CONFIRMING) {
                        handleConfirmationVoice(transcription)
                    } else {
                        processVoiceInput(transcription)
                    }
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    updateRecognizedText(matches[0])
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
    }

    private fun processVoiceInput(text: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val parsed = VoiceIntentParser.parse(text, allVillagesList)
            currentIntent = parsed
            when (parsed.intent) {
                IntentType.CONFIRM_YES -> handleConfirmationVoice(text)
                IntentType.CONFIRM_NO -> onConfirmNo()
                IntentType.SEARCH_BALANCE -> handleBalanceQuery(parsed, text)
                IntentType.MEDICINE -> handleMedicineEntry(parsed)
                IntentType.PAYMENT -> handlePaymentEntry(parsed)
                IntentType.MEDICINE_AND_PAYMENT -> handleMedicineAndPayment(parsed)
                IntentType.NEW_PATIENT -> handleNewPatient(parsed)
                IntentType.CORRECTION -> handleCorrection()
                IntentType.UNKNOWN -> {
                    withContext(Dispatchers.Main) {
                        setState(ConversationState.ERROR)
                        ttsManager?.speak("समझ नहीं आया, फिर से बोलिए") {
                            delayThen { setState(ConversationState.LISTENING); startListening() }
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleBalanceQuery(parsed: ParsedVoiceIntent, rawText: String) {
        val name = parsed.patientName ?: VoiceIntentParser.extractPatientName(rawText)
        val patient = if (name != null) findPatient(name) else null
        withContext(Dispatchers.Main) {
            if (patient != null) {
                matchedPatient = patient
                showBalanceCard(patient)
            } else {
                setState(ConversationState.ERROR)
                val msg = if (name != null) "$name नहीं मिले" else "कौन सा रोगी?"
                ttsManager?.speak(msg) {
                    delayThen { setState(ConversationState.LISTENING); startListening() }
                }
            }
        }
    }

    private suspend fun handleMedicineEntry(parsed: ParsedVoiceIntent) {
        val name = parsed.patientName
        val patient = if (name != null) findPatient(name) else null
        withContext(Dispatchers.Main) {
            if (patient == null) {
                setState(ConversationState.ERROR)
                val msg = if (name != null) "$name नहीं मिले" else "कौन सा रोगी?"
                ttsManager?.speak(msg) {
                    delayThen { setState(ConversationState.LISTENING); startListening() }
                }
                return@withContext
            }
            matchedPatient = patient
            val amount = parsed.medicineAmount
            if (amount == null || amount <= 0) {
                setState(ConversationState.LISTENING)
                ttsManager?.speak("${patient.name} के लिए कितने की दवा?") {
                    startListening()
                }
                return@withContext
            }
            pendingTransaction = Transaction(
                patientId = patient.id, type = "medicine", amount = amount, createdAt = Date()
            )
            val newBalance = patient.currentBalance + amount
            showConfirmationCard(
                title = "${patient.name} — दवा",
                detail = "दवा: ₹${amount.toLong()}, पिछला बकाया: ₹${patient.currentBalance.toLong()}",
                balance = "नया बकाया: ₹${newBalance.toLong()}",
                spoken = getString(R.string.voice_medicine_confirmation,
                    patient.name, amount.toLong().toString(), newBalance.toLong().toString())
            )
        }
    }

    private suspend fun handlePaymentEntry(parsed: ParsedVoiceIntent) {
        val name = parsed.patientName
        val patient = if (name != null) findPatient(name) else null
        withContext(Dispatchers.Main) {
            if (patient == null) {
                setState(ConversationState.ERROR)
                val msg = if (name != null) "$name नहीं मिले" else "कौन सा रोगी?"
                ttsManager?.speak(msg) {
                    delayThen { setState(ConversationState.LISTENING); startListening() }
                }
                return@withContext
            }
            matchedPatient = patient
            val amount = parsed.paymentAmount ?: parsed.medicineAmount
            if (amount == null || amount <= 0) {
                setState(ConversationState.LISTENING)
                ttsManager?.speak("${patient.name} से कितने पैसे मिले?") {
                    startListening()
                }
                return@withContext
            }
            pendingTransaction = Transaction(
                patientId = patient.id, type = "payment", amount = amount, createdAt = Date()
            )
            val newBalance = patient.currentBalance - amount
            showConfirmationCard(
                title = "${patient.name} — भुगतान",
                detail = "जमा: ₹${amount.toLong()}, पिछला बकाया: ₹${patient.currentBalance.toLong()}",
                balance = "नया बकाया: ₹${maxOf(0.0, newBalance).toLong()}",
                spoken = getString(R.string.voice_payment_confirmation,
                    patient.name, amount.toLong().toString(), maxOf(0.0, newBalance).toLong().toString())
            )
        }
    }

    private suspend fun handleMedicineAndPayment(parsed: ParsedVoiceIntent) {
        val name = parsed.patientName
        val patient = if (name != null) findPatient(name) else null
        withContext(Dispatchers.Main) {
            if (patient == null) {
                setState(ConversationState.ERROR)
                val msg = if (name != null) "$name नहीं मिले" else "कौन सा रोगी?"
                ttsManager?.speak(msg) { delayThen { startListening() } }
                return@withContext
            }
            matchedPatient = patient
            val medAmount = parsed.medicineAmount ?: 0.0
            val payAmount = parsed.paymentAmount ?: 0.0
            if (medAmount <= 0 && payAmount <= 0) {
                setState(ConversationState.LISTENING)
                ttsManager?.speak("${patient.name} के लिए कितने की दवा और कितने पैसे दिए?") { startListening() }
                return@withContext
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
                spoken = getString(R.string.voice_medicine_payment_confirmation,
                    patient.name, medAmount.toLong().toString(), payAmount.toLong().toString(),
                    maxOf(0.0, newBalance).toLong().toString())
            )
        }
    }

    private suspend fun handleNewPatient(parsed: ParsedVoiceIntent) {
        val name = parsed.patientName ?: run {
            withContext(Dispatchers.Main) {
                setState(ConversationState.ERROR)
                ttsManager?.speak("नए रोगी का नाम नहीं सुन पाया") { delayThen { startListening() } }
            }
            return
        }
        val existingPatient = findPatient(name)
        if (existingPatient != null) {
            withContext(Dispatchers.Main) {
                matchedPatient = existingPatient
                showBalanceCard(existingPatient)
            }
            return
        }
        val village = parsed.villageName ?: "Siras"
        val amount = parsed.medicineAmount
        withContext(Dispatchers.Main) {
            val spoken = getString(R.string.voice_new_patient_confirm, name, village)
            showConfirmationCard(
                title = "नया रोगी: $name",
                detail = "गाँव: $village${if (amount != null) ", दवा: ₹${amount.toLong()}" else ""}",
                balance = if (amount != null) "बकाया: ₹${amount.toLong()}" else "कोई दवा नहीं",
                spoken = spoken
            )
        }
    }

    private suspend fun handleCorrection() {
        val lastTx = repository.getLastTransaction()
        if (lastTx == null) {
            withContext(Dispatchers.Main) {
                setState(ConversationState.ERROR)
                ttsManager?.speak("कोई पिछली एंट्री नहीं मिली") {
                    delayThen { setState(ConversationState.LISTENING); startListening() }
                }
            }
            return
        }
        val patient = repository.getPatientByIdSync(lastTx.patientId)
        if (patient == null) {
            withContext(Dispatchers.Main) {
                setState(ConversationState.ERROR)
                ttsManager?.speak("पिछली एंट्री का रोगी नहीं मिला") {
                    delayThen { setState(ConversationState.LISTENING); startListening() }
                }
            }
            return
        }
        
        withContext(Dispatchers.Main) {
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
            val titleStr = "${patient.name} — सुधार (Correction)"
            val detailStr = "रद्द (Cancel): $typeText ₹${lastTx.amount.toLong()}"
            val balanceStr = "बकाया पुनः स्थापित: ₹${maxOf(0.0, newBalance).toLong()}"
            
            val spokenText = "पिछली एंट्री ${patient.name} की $typeText ${lastTx.amount.toLong()} रुपये रद्द कर रहा हूँ। नया बकाया ${newBalance.toLong()} रुपये। क्या यह सही है?"
            
            showConfirmationCard(
                title = titleStr,
                detail = detailStr,
                balance = balanceStr,
                spoken = spokenText
            )
        }
    }

    private fun handleConfirmationVoice(text: String) {
        val parsed = VoiceIntentParser.parse(text)
        when (parsed.intent) {
            IntentType.CONFIRM_YES -> onConfirmYes()
            IntentType.CONFIRM_NO -> onConfirmNo()
            else -> {
                updateRecognizedText("$text — समझ नहीं आया")
                ttsManager?.speak("हाँ या नहीं बोलिए") {
                    delayThen { startListening() }
                }
            }
        }
    }

    private fun onConfirmYes() {
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) { setState(ConversationState.SAVING) }
            try {
                val intent = currentIntent
                val patient = matchedPatient
                when (intent?.intent) {
                    IntentType.MEDICINE -> {
                        if (pendingTransaction != null) {
                            repository.insertTransaction(pendingTransaction!!)
                            withContext(Dispatchers.Main) { onSaveComplete() }
                        }
                    }
                    IntentType.PAYMENT -> {
                        if (pendingTransaction != null) {
                            repository.insertTransaction(pendingTransaction!!)
                            withContext(Dispatchers.Main) { onSaveComplete() }
                        }
                    }
                    IntentType.MEDICINE_AND_PAYMENT -> {
                        val med = intent.medicineAmount
                        val pay = intent.paymentAmount
                        if (patient != null) {
                            if (med != null && med > 0) repository.insertTransaction(
                                Transaction(patientId = patient.id, type = "medicine", amount = med, createdAt = Date())
                            )
                            if (pay != null && pay > 0) repository.insertTransaction(
                                Transaction(patientId = patient.id, type = "payment", amount = pay, createdAt = Date())
                            )
                            withContext(Dispatchers.Main) { onSaveComplete() }
                        }
                    }
                    IntentType.NEW_PATIENT -> {
                        val name = intent.patientName ?: return@launch
                        val villageName = intent.villageName ?: "Siras"
                        val villageId = villagesMap.entries.firstOrNull { it.value == villageName }?.key
                            ?: repository.insertVillage(
                                com.villageclinicledger.data.models.Village(name = villageName)
                            )
                        val newPatientId = repository.insertPatient(
                            Patient(name = name, villageId = villageId)
                        )
                        val medAmt = intent.medicineAmount
                        if (medAmt != null && medAmt > 0) {
                            repository.insertTransaction(
                                Transaction(patientId = newPatientId, type = "medicine", amount = medAmt, createdAt = Date())
                            )
                        }
                        matchedPatient = repository.getPatientByName(name)
                        withContext(Dispatchers.Main) { onSaveComplete() }
                    }
                    IntentType.CORRECTION -> {
                        if (pendingTransaction != null) {
                            repository.insertTransaction(pendingTransaction!!)
                            withContext(Dispatchers.Main) { onSaveComplete() }
                        } else {
                            withContext(Dispatchers.Main) { onSaveComplete() }
                        }
                    }
                    IntentType.SEARCH_BALANCE -> {
                        if (patient != null) {
                            withContext(Dispatchers.Main) {
                                navigateToPatient(patient.id)
                            }
                        }
                    }
                    else -> withContext(Dispatchers.Main) { onSaveComplete() }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setState(ConversationState.ERROR)
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    ttsManager?.speak("गड़बड़ी हो गई") { delayThen { startListening() } }
                }
            }
        }
    }

    private fun onConfirmNo() {
        currentIntent = null
        pendingTransaction = null
        lifecycleScope.launch(Dispatchers.Main) {
            setState(ConversationState.LISTENING)
            ttsManager?.speak(getString(R.string.voice_try_again)) {
                startListening()
            }
        }
    }

    private fun onSaveComplete() {
        setState(ConversationState.DONE)
        ttsManager?.speak(getString(R.string.voice_saved)) {
            delayThen { dismiss() }
        }
    }

    private fun showBalanceCard(patient: Patient) {
        setState(ConversationState.CONFIRMING)
        val balanceHindi = HindiNumberConverter.convertShort(patient.currentBalance)
        updateConfirmationCard(
            title = patient.name,
            detail = "गाँव: ${getVillageName(patient.villageId)}",
            balanceAmount = "बकाया: ₹${patient.currentBalance.toLong()}"
        )
        val spoken = "${patient.name} पर $balanceHindi रुपये बकाया हैं। " +
                getString(R.string.voice_confirm_question)
        ttsManager?.speak(spoken)
        view?.findViewById<MaterialButton>(R.id.btnConfirmYes)?.text = "खोलें"
        view?.findViewById<MaterialButton>(R.id.btnConfirmYes)?.setOnClickListener {
            navigateToPatient(patient.id)
        }
        view?.findViewById<MaterialButton>(R.id.btnConfirmNo)?.text = "वापस"
        view?.findViewById<MaterialButton>(R.id.btnConfirmNo)?.setOnClickListener { dismiss() }
    }

    private fun showConfirmationCard(title: String, detail: String, balance: String, spoken: String) {
        setState(ConversationState.CONFIRMING)
        updateConfirmationCard(title, detail, balance)
        ttsManager?.speak(spoken)
    }

    private fun updateConfirmationCard(title: String, detail: String, balanceAmount: String) {
        val view = view ?: return
        view.findViewById<TextView>(R.id.confirmationTitle).text = title
        view.findViewById<TextView>(R.id.confirmationDetail).text = detail
        view.findViewById<TextView>(R.id.confirmationBalance).text = balanceAmount
    }

    private fun updateRecognizedText(text: String) {
        view?.findViewById<TextView>(R.id.recognizedSpeech)?.text = text
    }

    private fun setupHoldListening() {
        val btnYes = view?.findViewById<MaterialButton>(R.id.btnConfirmYes)
        val btnNo = view?.findViewById<MaterialButton>(R.id.btnConfirmNo)
        btnYes?.text = "हाँ (Sahi Hai)"
        btnYes?.setOnClickListener { onConfirmYes() }
        btnNo?.text = "नहीं (Badlo)"
        btnNo?.setOnClickListener { onConfirmNo() }
    }

    private suspend fun findPatient(name: String): Patient? {
        val matches = repository.findPatientByVoice(name)
        if (matches.size > 1) {
            ambiguousPatients = matches
            withContext(Dispatchers.Main) {
                showDisambiguationCard(matches)
            }
            return null
        }
        return matches.firstOrNull()
    }

    private fun showDisambiguationCard(patients: List<Patient>) {
        setState(ConversationState.CONFIRMING)
        val textBuilder = StringBuilder()
        val spokenBuilder = StringBuilder()
        spokenBuilder.append("दो रोगी मिले। ")
        for ((index, p) in patients.withIndex()) {
            val villageName = getVillageName(p.villageId)
            textBuilder.append("${index + 1}. ${p.name} (${villageName})\n")
            spokenBuilder.append("${p.name} ${villageName} से, ")
        }
        spokenBuilder.append("कौन सा?")
        
        updateConfirmationCard(
            title = "कौन सा रोगी?",
            detail = textBuilder.toString(),
            balanceAmount = ""
        )
        ttsManager?.speak(spokenBuilder.toString()) {
            delayThen { setState(ConversationState.LISTENING); startListening() }
        }
    }

    private fun handleDisambiguation(text: String) {
        val patients = ambiguousPatients ?: return
        val clean = text.lowercase()
        var selected: Patient? = null
        for (p in patients) {
            val lastName = p.name.split(" ").lastOrNull()?.lowercase() ?: ""
            val firstName = p.name.split(" ").firstOrNull()?.lowercase() ?: ""
            val village = getVillageName(p.villageId).lowercase()
            if (clean.contains(lastName) || clean.contains(village) || clean.contains(firstName)) {
                selected = p
                break
            }
        }
        if (clean.contains("pehla") || clean.contains("first") || clean.contains("ek") || clean.contains("1") || clean.contains("एक")) {
            selected = patients.getOrNull(0)
        } else if (clean.contains("doosra") || clean.contains("second") || clean.contains("do") || clean.contains("2") || clean.contains("दो")) {
            selected = patients.getOrNull(1)
        }

        if (selected != null) {
            ambiguousPatients = null
            matchedPatient = selected
            val originalIntent = currentIntent
            if (originalIntent != null) {
                lifecycleScope.launch(Dispatchers.IO) {
                    when (originalIntent.intent) {
                        IntentType.SEARCH_BALANCE -> handleBalanceQuery(originalIntent, transcription)
                        IntentType.MEDICINE -> handleMedicineEntry(originalIntent)
                        IntentType.PAYMENT -> handlePaymentEntry(originalIntent)
                        IntentType.MEDICINE_AND_PAYMENT -> handleMedicineAndPayment(originalIntent)
                        else -> {
                            withContext(Dispatchers.Main) {
                                showBalanceCard(selected)
                            }
                        }
                    }
                }
            } else {
                showBalanceCard(selected)
            }
        } else {
            ttsManager?.speak("मुझे समझ नहीं आया। पहला या दूसरा?") {
                startListening()
            }
        }
    }

    private fun getVillageName(villageId: Long): String {
        return villagesMap[villageId] ?: ""
    }

    private fun navigateToPatient(patientId: Long) {
        val fragment = PatientDetailFragment.newInstance(patientId)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
        dismiss()
    }

    private fun delayThen(action: () -> Unit) {
        view?.postDelayed(action, 1200)
    }

    override fun onDestroyView() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        ttsManager?.shutdown()
        ttsManager = null
        super.onDestroyView()
    }
}
