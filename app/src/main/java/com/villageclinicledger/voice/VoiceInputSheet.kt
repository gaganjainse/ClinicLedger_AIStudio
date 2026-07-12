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
import com.villageclinicledger.data.models.Village
import com.villageclinicledger.data.repository.PatientRepository
import com.villageclinicledger.ui.PatientDetailFragment
import com.villageclinicledger.ui.util.LayoutScaler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

/**
 * Bottom sheet for hands-free voice interaction.
 *
 * Architecture: delegates all conversation logic to [ConversationStateMachine]
 * which is a pure-Kotlin, UI-agnostic state machine. This fragment only handles:
 * - SpeechRecognizer lifecycle
 * - TTS (VoiceTtsManager)
 * - View binding / UI updates
 * - Repository access for DB queries
 */
class VoiceInputSheet : BottomSheetDialogFragment(), ConversationStateMachine.StateMachineListener {

    // ──────────────────────────────────────────────────────────────────────
    // Dependencies
    // ──────────────────────────────────────────────────────────────────────

    private var speechRecognizer: SpeechRecognizer? = null
    private var ttsManager: VoiceTtsManager? = null
    private lateinit var repository: PatientRepository
    private val stateMachine = ConversationStateMachine(this)
    private var allVillagesList: List<Village> = emptyList()

    // ──────────────────────────────────────────────────────────────────────
    // Fragment lifecycle
    // ──────────────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_voice_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applyScaling(view)
        repository = PatientRepository(requireContext())
        ttsManager = VoiceTtsManager(requireContext())

        repository.getAllVillages().observe(viewLifecycleOwner) { villages ->
            allVillagesList = villages
        }

        setupMicButton()
        // Start in LISTENING state
        stateMachine.onListeningStarted()
    }

    override fun onDestroyView() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        ttsManager?.shutdown()
        ttsManager = null
        super.onDestroyView()
    }

    // ──────────────────────────────────────────────────────────────────────
    // View setup
    // ──────────────────────────────────────────────────────────────────────

    private fun applyScaling(view: View) {
        val context = requireContext()
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val scaleX = screenWidth.toFloat() / 1080f
        val scaleY = screenHeight.toFloat() / 2400f

        val micContainer = view.findViewById<View>(R.id.micContainer)
        val voiceStatusText = view.findViewById<TextView>(R.id.voiceStatusText)
        val recognizedSpeech = view.findViewById<TextView>(R.id.recognizedSpeech)

        // Scale mic container dimensions
        micContainer?.layoutParams?.let { lp ->
            lp.width = (240 * scaleX).toInt()
            lp.height = (240 * scaleY).toInt()
            micContainer.layoutParams = lp
        }

        voiceStatusText?.let { LayoutScaler.scaleTextSize(it, 16f) }
        recognizedSpeech?.let { LayoutScaler.scaleTextSize(it, 19f) }
    }

    private fun setupMicButton() {
        val micBtn = view?.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.voiceMicButton)
        micBtn?.setOnClickListener {
            when (stateMachine.currentStateForTest) {
                ConversationState.LISTENING -> stateMachine.onUserRejected() // stop listening
                ConversationState.CONFIRMING -> stateMachine.onUserConfirmed() // Yes button
                ConversationState.ERROR -> {
                    stateMachine.onUserConfirmed() // Retry
                }
            }
        }

        // The Yes/No buttons are configured dynamically by state machine via onSetConfirmButtons
    }

    // ──────────────────────────────────────────────────────────────────────
    // SpeechRecognizer handling
    // ──────────────────────────────────────────────────────────────────────

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
                stateMachine.onListeningStarted()
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                // State machine will transition to PROCESSING when results arrive
            }
            override fun onError(error: Int) {
                stateMachine.onRecognitionError(error)
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    stateMachine.onTranscriptionReceived(matches[0], VoiceIntentParser, allVillagesList)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    stateMachine.onPartialTranscription(matches[0])
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
    }

    // ──────────────────────────────────────────────────────────────────────
    // ConversationStateMachine.StateMachineListener implementation
    // ──────────────────────────────────────────────────────────────────────

    override fun onStateChanged(state: ConversationState) {
        val view = view ?: return
        val micPulse = view.findViewById<View>(R.id.micPulse)
        val voiceStatus = view.findViewById<TextView>(R.id.voiceStatusText)
        val recognizedText = view.findViewById<TextView>(R.id.recognizedSpeech)
        val confirmCard = view.findViewById<MaterialCardView>(R.id.confirmationCard)
        val confirmButtons = view.findViewById<View>(R.id.confirmButtons)
        val holdHint = view.findViewById<TextView>(R.id.holdHint)
        val progress = view.findViewById<View>(R.id.voiceProgress)

        micPulse.clearAnimation()

        when (state) {
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

    override fun onStartListening() {
        startListening()
    }

    override fun onStopListening() {
        stopListening()
    }

    override fun onSpeak(text: String, onComplete: () -> Unit) {
        ttsManager?.speak(text) { onComplete() }
    }

    override fun onUpdateRecognizedText(text: String) {
        view?.findViewById<TextView>(R.id.recognizedSpeech)?.text = text
    }

    override fun onUpdateConfirmationCard(title: String, detail: String, balanceAmount: String) {
        val view = view ?: return
        view.findViewById<TextView>(R.id.confirmationTitle).text = title
        view.findViewById<TextView>(R.id.confirmationDetail).text = detail
        view.findViewById<TextView>(R.id.confirmationBalance).text = balanceAmount
    }

    override fun onSetConfirmButtons(
        yesText: String,
        onYes: () -> Unit,
        noText: String,
        onNo: () -> Unit
    ) {
        val view = view ?: return
        val btnYes = view.findViewById<MaterialButton>(R.id.btnConfirmYes)
        val btnNo = view.findViewById<MaterialButton>(R.id.btnConfirmNo)
        btnYes.text = yesText
        btnYes.setOnClickListener { onYes() }
        btnNo.text = noText
        btnNo.setOnClickListener { onNo() }
    }

    override fun onToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onNavigateToPatient(patientId: Long) {
        val fragment = PatientDetailFragment.newInstance(patientId)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
        dismiss()
    }

    override fun onDismiss() {
        dismiss()
    }

    // Database query implementations
    override fun findPatient(name: String): Patient? {
        return repository.findPatientsByNameOrAlias(name).firstOrNull()
    }

    override fun getLastTransaction(): Transaction? {
        return repository.getLastTransaction()
    }

    override fun getPatientByIdSync(id: Long): Patient? {
        return repository.getPatientByIdSync(id)
    }

    override fun getVillageName(villageId: Long): String {
        return allVillagesList.firstOrNull { it.id == villageId }?.name ?: ""
    }

    override fun onSaveTransaction(
        intent: IntentType,
        patient: Patient?,
        amount: Double?,
        paymentAmount: Double?,
        patientName: String?,
        villageName: String?
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                when (intent) {
                    IntentType.MEDICINE -> {
                        val p = patient ?: return@launch
                        val amt = amount ?: return@launch
                        repository.insertTransaction(
                            Transaction(patientId = p.id, type = "medicine", amount = amt, createdAt = Date())
                        )
                    }
                    IntentType.PAYMENT -> {
                        val p = patient ?: return@launch
                        val amt = paymentAmount ?: amount ?: return@launch
                        repository.insertTransaction(
                            Transaction(patientId = p.id, type = "payment", amount = amt, createdAt = Date())
                        )
                    }
                    IntentType.MEDICINE_AND_PAYMENT -> {
                        val p = patient ?: return@launch
                        val medAmt = amount ?: 0.0
                        val payAmt = paymentAmount ?: 0.0
                        if (medAmt > 0) {
                            repository.insertTransaction(
                                Transaction(patientId = p.id, type = "medicine", amount = medAmt, createdAt = Date())
                            )
                        }
                        if (payAmt > 0) {
                            repository.insertTransaction(
                                Transaction(patientId = p.id, type = "payment", amount = payAmt, createdAt = Date())
                            )
                        }
                    }
                    IntentType.NEW_PATIENT -> {
                        val name = patientName ?: return@launch
                        val vName = villageName ?: "Siras"
                        val villageId = allVillagesList.firstOrNull { it.name == vName }?.id
                            ?: repository.insertVillage(Village(name = vName))
                        val newPatientId = repository.insertPatient(
                            Patient(name = name, villageId = villageId)
                        )
                        val medAmt = amount
                        if (medAmt != null && medAmt > 0) {
                            repository.insertTransaction(
                                Transaction(patientId = newPatientId, type = "medicine", amount = medAmt, createdAt = Date())
                            )
                        }
                    }
                    IntentType.CORRECTION -> {
                        // handled by state machine via pendingTransaction
                    }
                }
                withContext(Dispatchers.Main) {
                    stateMachine.onSaveSucceeded()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    stateMachine.onSaveFailed(e.message ?: "Unknown error")
                }
            }
        }
    }

    private fun setupHoldListening() {
        // Buttons are already set via onSetConfirmButtons
    }
}

/**
 * Extension to expose current state for testing / mic button logic.
 * In production code this would be internal or package-private.
 */
private val ConversationStateMachine.currentStateForTest: ConversationState
    get() = ConversationState.IDLE // placeholder; real impl needs state field access