package com.gmtusharx.friday

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gmtusharx.friday.automation.IntentExecutor
import com.gmtusharx.friday.data.FridayPrefs
import com.gmtusharx.friday.logic.Command
import com.gmtusharx.friday.logic.CommandRouter
import com.gmtusharx.friday.model.ChatMessage
import com.gmtusharx.friday.model.OrbState
import com.gmtusharx.friday.model.UiState
import com.gmtusharx.friday.proactive.ProactivePrompts
import com.gmtusharx.friday.voice.Speaker
import com.gmtusharx.friday.voice.VoiceRecognizer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FridayViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = FridayPrefs(app)
    private val speaker = Speaker(app)
    private val executor = IntentExecutor(app)
    private val router = CommandRouter()

    private var nextId = 0L
    private var inactivityJob: Job? = null
    private var unansweredPrompts = 0
    private var foreground = false

    private val _ui = MutableStateFlow(
        UiState(
            messages = listOf(
                ChatMessage(
                    nextId++,
                    "Namaste! Main Friday hoon — GM TUSHARX ki banayi hui aapki AI assistant. Mic dabaiye ya kuch type kijiye.",
                    false
                )
            ),
            inactivityMinutes = prefs.inactivityMinutes,
            proactiveEnabled = prefs.proactiveEnabled,
            language = prefs.language
        )
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private val recognizer = VoiceRecognizer(app, object : VoiceRecognizer.Callback {
        override fun onRms(level: Float) {
            if (_ui.value.orb == OrbState.Listening) _ui.update { it.copy(level = level) }
        }
        override fun onPartial(text: String) = _ui.update { it.copy(partial = text) }
        override fun onFinal(text: String) = submit(text)
        override fun onError(message: String, silent: Boolean) {
            _ui.update { it.copy(orb = OrbState.Idle, level = 0f, partial = "") }
            if (!silent && message.isNotBlank()) addMessage(message, fromUser = false)
        }
    })

    // ---------- Input ----------

    fun toggleListening() {
        markActive()
        when (_ui.value.orb) {
            OrbState.Listening -> { recognizer.stop(); _ui.update { it.copy(orb = OrbState.Idle, level = 0f, partial = "") } }
            OrbState.Speaking -> { speaker.stop(); startListening() }
            OrbState.Processing -> Unit
            OrbState.Idle -> startListening()
        }
    }

    private fun startListening() {
        if (!recognizer.isAvailable()) {
            addMessage("Is device par speech recognition available nahi hai. Type karke try kijiye.", false)
            return
        }
        speaker.stop()
        recognizer.start(prefs.language)
        _ui.update { it.copy(orb = OrbState.Listening, partial = "", level = 0f) }
    }

    fun showPermissionNeeded() =
        addMessage("Voice commands ke liye mic permission chahiye. Tab tak aap type kar sakte hain.", false)

    fun submit(text: String) {
        val input = text.trim()
        if (input.isEmpty()) return
        markActive()
        addMessage(input, fromUser = true)
        _ui.update { it.copy(orb = OrbState.Processing, partial = "", level = 0f) }
        viewModelScope.launch {
            delay(450) // lets the "thinking" animation breathe
            val reply = when (val cmd = router.route(input)) {
                is Command.Reply -> cmd.text
                is Command.PlayYouTube -> {
                    executor.playYouTube(cmd.query)
                    "YouTube par \"${cmd.query}\" chala rahi hoon."
                }
                is Command.WebSearch -> {
                    executor.webSearch(cmd.query)
                    "Google par \"${cmd.query}\" search kar rahi hoon."
                }
                is Command.OpenApp ->
                    if (executor.openApp(cmd.name)) "${cmd.name} khol rahi hoon."
                    else "Mujhe \"${cmd.name}\" naam ka koi app nahi mila."
                Command.Status -> executor.statusReport()
            }
            say(reply)
        }
    }

    // ---------- Output ----------

    private fun addMessage(text: String, fromUser: Boolean) {
        val id = nextId++
        _ui.update { it.copy(messages = it.messages + ChatMessage(id, text, fromUser)) }
    }

    private fun say(text: String) {
        addMessage(text, fromUser = false)
        _ui.update { it.copy(orb = OrbState.Speaking) }
        speaker.speak(text) {
            _ui.update { s -> if (s.orb == OrbState.Speaking) s.copy(orb = OrbState.Idle) else s }
        }
    }

    // ---------- Proactive companion (in-app timer) ----------

    private fun markActive() {
        unansweredPrompts = 0
        restartTimer()
    }

    private fun restartTimer() {
        inactivityJob?.cancel()
        if (prefs.proactiveEnabled && foreground) startTimer()
    }

    private fun startTimer() {
        inactivityJob = viewModelScope.launch {
            delay(prefs.inactivityMinutes * 60_000L)
            onInactive()
        }
    }

    private fun onInactive() {
        if (_ui.value.orb != OrbState.Idle) { startTimer(); return }  // busy; try again later
        if (unansweredPrompts >= 3) return                             // don't nag forever
        unansweredPrompts++
        say(ProactivePrompts.random())
        startTimer()
    }

    fun onAppForeground() {
        foreground = true
        unansweredPrompts = 0
        restartTimer()
    }

    fun onAppBackground() {
        foreground = false
        inactivityJob?.cancel()
        recognizer.stop()
        _ui.update { if (it.orb == OrbState.Listening) it.copy(orb = OrbState.Idle, level = 0f, partial = "") else it }
        // WorkManager (scheduled by MainActivity.onStop) takes over from here.
    }

    // ---------- Settings ----------

    fun setInactivityMinutes(min: Int) {
        prefs.inactivityMinutes = min
        _ui.update { it.copy(inactivityMinutes = prefs.inactivityMinutes) }
        restartTimer()
    }

    fun setProactiveEnabled(enabled: Boolean) {
        prefs.proactiveEnabled = enabled
        _ui.update { it.copy(proactiveEnabled = enabled) }
        restartTimer()
    }

    fun setLanguage(tag: String) {
        prefs.language = tag
        _ui.update { it.copy(language = tag) }
    }

    override fun onCleared() {
        inactivityJob?.cancel()
        recognizer.stop()
        speaker.shutdown()
    }
}
