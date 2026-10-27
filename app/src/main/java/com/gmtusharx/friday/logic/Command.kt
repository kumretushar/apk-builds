package com.gmtusharx.friday.logic

/**
 * All commands Friday can execute.
 * Separate file so the ViewModel can import it cleanly.
 */
sealed interface Command {
    data class PlayYouTube(val query: String) : Command
    data class WebSearch(val query: String) : Command
    data class OpenApp(val name: String) : Command
    data class Call(val contact: String) : Command
    data class WhatsApp(val contact: String) : Command
    data class Sms(val contact: String, val message: String) : Command
    data class SetAlarm(val time: String) : Command
    data object OpenCamera : Command
    data object OpenPhotos : Command
    data object Status : Command
    data class Reply(val text: String) : Command
    data class Gemini(val input: String) : Command
}