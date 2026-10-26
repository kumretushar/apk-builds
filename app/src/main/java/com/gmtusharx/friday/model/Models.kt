package com.gmtusharx.friday.model

enum class OrbState { Idle, Listening, Processing, Speaking }

data class ChatMessage(val id: Long, val text: String, val fromUser: Boolean)

data class UiState(
    val orb: OrbState = OrbState.Idle,
    val level: Float = 0f,
    val partial: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val inactivityMinutes: Int = 3,
    val proactiveEnabled: Boolean = true,
    val language: String = "hi-IN"
)
