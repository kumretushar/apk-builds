package com.gmtusharx.friday.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/** Thin wrapper over Android SpeechRecognizer. Must be created and used on the main thread. */
class VoiceRecognizer(private val context: Context, private val cb: Callback) {

    interface Callback {
        fun onRms(level: Float)                       // 0..1, drives the orb
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(message: String, silent: Boolean)  // silent = no-speech / no-match
    }

    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun start(languageTag: String) {
        stop()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(listener)
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
        recognizer?.startListening(intent)
    }

    fun stop() {
        recognizer?.run {
            runCatching { cancel() }
            runCatching { destroy() }
        }
        recognizer = null
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}
        override fun onEvent(eventType: Int, params: Bundle?) {}

        override fun onRmsChanged(rmsdB: Float) {
            cb.onRms(((rmsdB + 2f) / 12f).coerceIn(0f, 1f))
        }

        override fun onPartialResults(partialResults: Bundle?) {
            partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()?.let(cb::onPartial)
        }

        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            if (text.isBlank()) cb.onError("", true) else cb.onFinal(text)
        }

        override fun onError(error: Int) {
            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> cb.onError("", true)
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    cb.onError("Mic permission chahiye. Please permission allow kijiye.", false)
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    cb.onError("Network problem hai. Internet check kijiye ya type karke try kijiye.", false)
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> cb.onError("Recognizer busy hai, ek second baad try kijiye.", false)
                else -> cb.onError("Voice recognition mein dikkat aayi (code $error).", false)
            }
        }
    }
}
