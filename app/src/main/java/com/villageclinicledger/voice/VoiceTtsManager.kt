package com.villageclinicledger.voice

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue

class VoiceTtsManager(context: Context) {

    private var tts: TextToSpeech? = null
    private val utteranceQueue: Queue<String> = ConcurrentLinkedQueue()
    private var isInitialized = false
    private var onDone: (() -> Unit)? = null
    private var toneGenerator: ToneGenerator? = null

    private val initListener = TextToSpeech.OnInitListener { status ->
        if (status == TextToSpeech.SUCCESS) {
            val hindiLocale = Locale("hi", "IN")
            val result = tts?.setLanguage(hindiLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            isInitialized = true
            speakQueued()
        }
    }

    init {
        tts = TextToSpeech(context.applicationContext, initListener)
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playStartListeningChime() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_SUP_PIP, 120)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playSuccessChime() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playErrorChime() {
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 250)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        this.onDone = onDone
        if (isInitialized) {
            speakNow(text)
        } else {
            utteranceQueue.add(text)
        }
    }

    private fun speakNow(text: String) {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                onDone?.invoke()
            }
            override fun onError(utteranceId: String?) {}
        })
        val utteranceId = "vcl_${System.currentTimeMillis()}"
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun speakQueued() {
        val next = utteranceQueue.poll() ?: return
        speakNow(next)
    }

    fun stop() {
        tts?.stop()
    }

    fun isSpeaking(): Boolean = tts?.isSpeaking ?: false

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
        try {
            toneGenerator?.release()
            toneGenerator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected fun finalize() {
        shutdown()
    }
}
