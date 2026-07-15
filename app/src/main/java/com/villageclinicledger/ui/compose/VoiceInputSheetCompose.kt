package com.villageclinicledger.ui.compose

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.villageclinicledger.data.models.Patient
import com.villageclinicledger.data.models.Transaction
import com.villageclinicledger.data.repository.PatientRepository
import com.villageclinicledger.ui.util.LocaleManager
import com.villageclinicledger.ui.util.LocaleManager.LocalIsHindi
import com.villageclinicledger.voice.IntentType
import com.villageclinicledger.voice.ParsedVoiceIntent
import com.villageclinicledger.voice.VoiceIntentParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VoiceInputSheetCompose(
    onDismiss: () -> Unit,
    onNavigateToPatientDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { PatientRepository(context) }
    val isHindi = LocalIsHindi.current

    var state by remember { mutableStateOf(ConversationState.IDLE) }
    var transcript by remember { mutableStateOf("") }
    var parsedIntent by remember { mutableStateOf<ParsedVoiceIntent?>(null) }
    var disambiguationPatients by remember { mutableStateOf<List<Patient>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    fun processTranscript(text: String) {
        scope.launch {
            state = ConversationState.PROCESSING
            val villages = withContext<List<com.villageclinicledger.data.models.Village>>(Dispatchers.IO) { repository.getAllVillagesSync() }
            val intent = VoiceIntentParser.parse(text, villages)
            parsedIntent = intent

            if (intent.intent == IntentType.UNKNOWN) {
                state = ConversationState.ERROR
                error = if (isHindi) "माफ़ करें, मुझे समझ नहीं आया।" else "Sorry, I didn't get that."
                return@launch
            }

            if (!intent.patientName.isNullOrBlank()) {
                val matches = withContext(Dispatchers.IO) { repository.findPatientByVoice(intent.patientName) }
                if (matches.size > 1) {
                    disambiguationPatients = matches
                    state = ConversationState.CONFIRMING
                } else if (matches.size == 1) {
                    parsedIntent = intent.copy(patientName = matches[0].name)
                    state = ConversationState.CONFIRMING
                } else {
                    state = ConversationState.CONFIRMING
                }
            } else {
                state = ConversationState.CONFIRMING
            }
        }
    }
    
    val listener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                state = ConversationState.LISTENING
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {
                state = ConversationState.PROCESSING
            }
            override fun onError(err: Int) {
                state = ConversationState.ERROR
                error = when(err) {
                    SpeechRecognizer.ERROR_NO_MATCH -> if (isHindi) "समझ नहीं आया" else "No match found"
                    SpeechRecognizer.ERROR_NETWORK -> if (isHindi) "नेटवर्क समस्या" else "Network error"
                    else -> "Error: $err"
                }
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    transcript = matches[0]
                    processTranscript(transcript)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (isHindi) "hi-IN" else "en-US")
        }
        speechRecognizer.setRecognitionListener(listener)
        speechRecognizer.startListening(intent)
    }

    fun executeAction(intent: ParsedVoiceIntent, confirmedPatientId: Long? = null) {
        scope.launch {
            state = ConversationState.SAVING
            withContext(Dispatchers.IO) {
                val targetId = confirmedPatientId ?: if (!intent.patientName.isNullOrBlank()) {
                    repository.getPatientByName(intent.patientName)?.id
                } else null

                if (targetId != null) {
                    if (intent.medicineAmount != null && intent.medicineAmount > 0) {
                        repository.insertTransaction(Transaction(patientId = targetId, type = "medicine", amount = intent.medicineAmount, notes = "Voice added"))
                    }
                    if (intent.paymentAmount != null && intent.paymentAmount > 0) {
                        repository.insertTransaction(Transaction(patientId = targetId, type = "payment", amount = intent.paymentAmount, notes = "Voice added"))
                    }
                    
                    if (intent.intent == IntentType.SEARCH_BALANCE) {
                        withContext(Dispatchers.Main) { onNavigateToPatientDetail(targetId) }
                    }
                }
            }
            state = ConversationState.DONE
            delay(1000)
            onDismiss()
        }
    }

    LaunchedEffect(Unit) {
        startListening()
    }

    DisposableEffect(Unit) {
        onDispose {
            speechRecognizer.destroy()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isHindi) "वॉयस असिस्टेंट" else "Voice Assistant",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (state) {
                        ConversationState.LISTENING -> ListeningAnim()
                        ConversationState.PROCESSING -> CircularProgressIndicator()
                        ConversationState.ERROR -> Icon(Icons.Rounded.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(64.dp))
                        ConversationState.DONE -> Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(64.dp))
                        else -> Icon(Icons.Rounded.Mic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (transcript.isNotBlank()) "\"$transcript\"" else (if (state == ConversationState.LISTENING) (if (isHindi) "सुन रहा हूँ..." else "Listening...") else ""),
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (state == ConversationState.CONFIRMING && parsedIntent != null) {
                    IntentConfirmation(
                        intent = parsedIntent!!,
                        patients = disambiguationPatients,
                        isHindi = isHindi,
                        onConfirm = { patientId -> executeAction(parsedIntent!!, patientId) },
                        onCancel = onDismiss
                    )
                }

                if (state == ConversationState.ERROR) {
                    Text(text = error ?: "", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { transcript = ""; error = null; startListening() }, modifier = Modifier.padding(top = 16.dp)) {
                        Text(if (isHindi) "फिर से कोशिश करें" else "Try Again")
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                if (state != ConversationState.SAVING && state != ConversationState.DONE) {
                    TextButton(onClick = onDismiss) {
                        Text(if (isHindi) "बंद करें" else "Close")
                    }
                }
            }
        }
    }
}

@Composable
fun ListeningAnim() {
    val infiniteTransition = rememberInfiniteTransition(label = "listening")
    val size by infiniteTransition.animateFloat(
        initialValue = 60f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "size"
    )
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    )
}

@Composable
fun IntentConfirmation(
    intent: ParsedVoiceIntent,
    patients: List<Patient>,
    isHindi: Boolean,
    onConfirm: (Long?) -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (isHindi) "क्या आप यह करना चाहते हैं?" else "Confirm Action", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            val summary = buildString {
                if (!intent.patientName.isNullOrBlank()) append("${intent.patientName}: ")
                if (intent.medicineAmount != null) append(if (isHindi) "दवाई ${LocaleManager.formatCurrency(intent.medicineAmount)}" else "Medicine ${LocaleManager.formatCurrency(intent.medicineAmount)}")
                if (intent.paymentAmount != null) {
                    if (isNotEmpty()) append(", ")
                    append(if (isHindi) "जमा ${LocaleManager.formatCurrency(intent.paymentAmount)}" else "Payment ${LocaleManager.formatCurrency(intent.paymentAmount)}")
                }
                if (intent.intent == IntentType.SEARCH_BALANCE) append(if (isHindi) "का हिसाब देखें" else "Check balance")
            }
            Text(text = summary, textAlign = TextAlign.Center)

            if (patients.size > 1) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(if (isHindi) "मरीज चुनें:" else "Select Patient:", style = MaterialTheme.typography.labelSmall)
                patients.forEach { p ->
                    Button(onClick = { onConfirm(p.id) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text(p.name)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text(if (isHindi) "नहीं" else "No")
                    }
                    Button(onClick = { onConfirm(null) }, modifier = Modifier.weight(1f)) {
                        Text(if (isHindi) "हाँ" else "Yes")
                    }
                }
            }
        }
    }
}

enum class ConversationState {
    IDLE, LISTENING, PROCESSING, CONFIRMING, SAVING, DONE, ERROR
}
