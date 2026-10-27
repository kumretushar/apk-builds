package com.gmtusharx.friday.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

/** Text-to-speech wrapper. Callbacks are delivered on the main thread. */
class Speaker(context: Context) {
    private val main = Handler(Looper.getMainLooper())
    private val callbacks = mutableMapOf<String, () -> Unit>()
    private var tts: TextToSpeech? = null
    private var ready = false
    private var queued: Pair<String, () -> Unit>? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            main.post {
                ready = status == TextToSpeech.SUCCESS
                val q = queued
                queued = null
                if (ready) {
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) = finish(utteranceId, run = true)
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) = finish(utteranceId, run = true)
                        override fun onStop(utteranceId: String?, interrupted: Boolean) = finish(utteranceId, run = false)
                    })
                    q?.let { speak(it.first, it.second) }
                } else {
                    q?.second?.invoke()
                }
            }
        }
    }

    private fun finish(id: String?, run: Boolean) {
        main.post {
            val cb = id?.let { callbacks.remove(it) }
            if (run) cb?.invoke()
        }
    }

    fun speak(text: String, onDone: () -> Unit = {}) {
        if (!ready) {
            if (tts == null) onDone() else queued = text to onDone
            return
        }
        val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
        val locale = if (hasDevanagari) Locale("hi", "IN") else Locale("en", "IN")
        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            tts?.setLanguage(Locale.getDefault())
        }
        val id = UUID.randomUUID().toString()
        callbacks[id] = onDone
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        callbacks.clear()
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
