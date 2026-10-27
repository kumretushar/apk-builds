package com.gmtusharx.friday

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gmtusharx.friday.automation.IntentExecutor
import com.gmtusharx.friday.data.FridayPrefs
import com.gmtusharx.friday.logic.Command
import com.gmtusharx.friday.logic.CommandRouter
import com.gmtusharx.friday.logic.GeminiBrain
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
    private val speaker = Speaker(app, prefs.speechRate)
    private val executor = IntentExecutor(app)
    private val router = CommandRouter()
    private val gemini = GeminiBrain()

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
            OrbState.Listening -> {
                recognizer.stop()
                _ui.update { it.copy(orb = OrbState.Idle, level = 0f, partial = "") }
            }
            OrbState.Speaking -> {
                speaker.stop()
                startListening()
            }
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
            delay(450)
            val cmd: Command = router.route(input)
            val reply: String = when (cmd) {
                is Command.Reply -> cmd.text
                is Command.PlayYouTube -> {
                    executor.playYouTube(cmd.query)
                    "YouTube par \"${cmd.query}\" chala rahi hoon."
                }
                is Command.WebSearch -> {
                    executor.webSearch(cmd.query)
                    "Google par \"${cmd.query}\" search kar rahi hoon."
                }
                is Command.OpenApp -> {
                    if (executor.openApp(cmd.name)) "${cmd.name} khol rahi hoon."
                    else "Mujhe \"${cmd.name}\" naam ka app nahi mila."
                }
                is Command.Call -> {
                    if (executor.call(cmd.contact)) "${cmd.contact} ko call kar rahi hoon."
                    else "\"${cmd.contact}\" ka number nahi mila."
                }
                is Command.WhatsApp -> {
                    if (executor.whatsapp(cmd.contact)) "${cmd.contact} ko WhatsApp khol rahi hoon."
                    else "\"${cmd.contact}\" ka number nahi mila."
                }
                is Command.Sms -> {
                    if (executor.sms(cmd.contact, cmd.message)) "${cmd.contact} ko SMS khol rahi hoon."
                    else "\"${cmd.contact}\" ka number nahi mila."
                }
                is Command.SetAlarm -> {
                    if (executor.setAlarm(cmd.time)) "Alarm set kar rahi hoon."
                    else "Alarm set nahi kar payi."
                }
                Command.OpenCamera -> {
                    executor.openCamera()
                    "Camera khol rahi hoon."
                }
                Command.OpenPhotos -> {
                    executor.openPhotos()
                    "Photos khol rahi hoon."
                }
                Command.Status -> executor.statusReport()
                is Command.Gemini -> {
                    _ui.update { it.copy(orb = OrbState.Processing) }
                    parseGeminiTool(gemini.ask(cmd.input))
                }
            }
            say(reply)
        }
    }

    private fun parseGeminiTool(text: String): String = when {
        text.contains("TOOL:PLAY_YOUTUBE:") -> {
            val q = text.substringAfter("TOOL:PLAY_YOUTUBE:").trim()
            executor.playYouTube(q)
            "YouTube par \"$q\" chala rahi hoon."
        }
        text.contains("TOOL:WEB_SEARCH:") -> {
            val q = text.substringAfter("TOOL:WEB_SEARCH:").trim()
            executor.webSearch(q)
            "Google par \"$q\" search kar rahi hoon."
        }
        text.contains("TOOL:OPEN_APP:") -> {
            val n = text.substringAfter("TOOL:OPEN_APP:").trim()
            if (executor.openApp(n)) "$n khol rahi hoon." else "\"$n\" nahi mila."
        }
        text.contains("TOOL:CALL:") -> {
            val c = text.substringAfter("TOOL:CALL:").trim()
            if (executor.call(c)) "$c ko call kar rahi hoon." else "\"$c\" ka number nahi mila."
        }
        text.contains("TOOL:WHATSAPP:") -> {
            val c = text.substringAfter("TOOL:WHATSAPP:").trim()
            if (executor.whatsapp(c)) "$c ko WhatsApp khol rahi hoon." else "number nahi mila."
        }
        text.contains("TOOL:SMS:") -> {
            val parts = text.substringAfter("TOOL:SMS:").split("|")
            val name = parts.getOrNull(0)?.trim() ?: ""
            val msg = parts.getOrNull(1)?.trim() ?: ""
            if (executor.sms(name, msg)) "$name ko SMS khol rahi hoon." else "number nahi mila."
        }
        text.contains("TOOL:SET_ALARM:") -> {
            val t = text.substringAfter("TOOL:SET_ALARM:").trim()
            if (executor.setAlarm(t)) "Alarm set kar rahi hoon." else "Alarm set nahi hua."
        }
        text.contains("TOOL:OPEN_CAMERA") -> {
            executor.openCamera()
            "Camera khol rahi hoon."
        }
        text.contains("TOOL:OPEN_PHOTOS") -> {
            executor.openPhotos()
            "Photos khol rahi hoon."
        }
        text.contains("TOOL:STATUS") -> executor.statusReport()
        text.isNotBlank() -> text
        else -> "Samjhi nahi. Thoda aur clearly boliye."
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

    // ---------- Proactive companion ----------

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
        if (_ui.value.orb != OrbState.Idle) {
            startTimer()
            return
        }
        if (unansweredPrompts >= 3) return
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
        _ui.update {
            if (it.orb == OrbState.Listening) it.copy(orb = OrbState.Idle, level = 0f, partial = "")
            else it
        }
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

    fun setSpeechRate(rate: Float) {
        prefs.speechRate = rate
        speaker.setSpeechRate(rate)
    }

    override fun onCleared() {
        inactivityJob?.cancel()
        recognizer.stop()
        speaker.shutdown()
        gemini.clear()
    }
}