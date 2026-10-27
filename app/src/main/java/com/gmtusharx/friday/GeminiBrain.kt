package com.gmtusharx.friday.logic

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.gmtusharx.friday.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gemini-powered brain — fallback jab rule-based router samajh na paaye.
 * Key yahan nahi hai — BuildConfig se aati hai (local.properties se).
 */
class GeminiBrain {

    private val history = mutableListOf<String>()

    private val model = GenerativeModel(
        modelName = "gemini-2.0-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        systemInstruction = content {
            text(
                """
                Tum Friday ho — GM TUSHARX ki banayi hui friendly AI assistant (female tone).
                User Hindi / Hinglish / English mein baat karega. Usi language mein jawab do.
                Chhota, natural, helpful jawab do. Zyada lamba mat likho.
                
                Tumhare paas phone ke ye tools hain. Jab user kahe to SIRF is format mein reply karo:
                TOOL:PLAY_YOUTUBE:<query>
                TOOL:WEB_SEARCH:<query>
                TOOL:OPEN_APP:<app name>
                TOOL:CALL:<contact name>
                TOOL:WHATSAPP:<contact name>
                TOOL:SMS:<contact name>|<message>
                TOOL:SET_ALARM:<time jaise 7:30 AM>
                TOOL:OPEN_CAMERA
                TOOL:OPEN_PHOTOS
                TOOL:STATUS
                
                Baaki har sawaal ka normal smart jawab do (knowledge, maths, jokes, advice, recipes, weather, etc.)
                Agar tool chahiye to SIRF "TOOL:..." likho, koi extra baat nahi.
                """.trimIndent()
            )
        }
    )

    suspend fun ask(userInput: String): String = withContext(Dispatchers.IO) {
        try {
            val contextInput = if (history.isEmpty()) userInput
                else "Pichli baatein:\n${history.takeLast(6).joinToString("\n")}\n\nAb user: $userInput"

            val response = model.generateContent(contextInput)
            val text = response.text?.trim() ?: ""

            history.add("User: $userInput")
            history.add("Friday: $text")
            while (history.size > 16) history.removeAt(0)

            text
        } catch (e: Exception) {
            "Sorry, abhi network ya AI mein dikkat hai. Thodi der baad try kijiye."
        }
    }

    fun clear() = history.clear()
}