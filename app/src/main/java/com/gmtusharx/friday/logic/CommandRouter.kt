package com.gmtusharx.friday.logic

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface Command {
    data class PlayYouTube(val query: String) : Command
    data class WebSearch(val query: String) : Command
    data class OpenApp(val name: String) : Command
    data object Status : Command
    data class Reply(val text: String) : Command
}

/** Lower-cases, strips punctuation and transliterates common Devanagari command words to Roman. */
object Normalizer {
    private val tokenMap = mapOf(
        "यूट्यूब" to "youtube", "यूटूब" to "youtube", "युट्यूब" to "youtube",
        "पर" to "par", "पे" to "pe", "में" to "me", "मे" to "me", "से" to "se",
        "चलाओ" to "chalao", "चलाइए" to "chalao", "चला" to "chala", "लगाओ" to "lagao", "लगा" to "laga",
        "बजाओ" to "bajao", "बजा" to "baja", "दो" to "do", "दीजिए" to "do", "करो" to "karo", "कर" to "kar",
        "प्ले" to "play", "सर्च" to "search", "गूगल" to "google", "खोलो" to "kholo", "खोल" to "khol",
        "ओपन" to "open", "ढूंढो" to "dhundo", "खोजो" to "khojo", "स्टेटस" to "status", "चेक" to "check",
        "किसने" to "kisne", "बनाया" to "banaya", "तुम्हें" to "tumhe", "तुम्हे" to "tumhe", "आपको" to "aapko",
        "तुझे" to "tujhe", "तुम्हारा" to "tumhara", "आपका" to "aapka", "तेरा" to "tera",
        "डेवलपर" to "developer", "क्रिएटर" to "creator", "कौन" to "kaun", "हो" to "ho", "है" to "hai",
        "तुम" to "tum", "आप" to "aap", "नमस्ते" to "namaste", "क्या" to "kya", "समय" to "samay",
        "टाइम" to "time", "बजे" to "baje", "कितने" to "kitne"
    )
    private val junk = Regex("""[^\p{L}\p{M}\p{N}\s']""")
    private val spaces = Regex("""\s+""")

    fun normalize(raw: String): String =
        junk.replace(raw.lowercase(), " ")
            .trim()
            .split(spaces)
            .filter { it.isNotEmpty() }
            .joinToString(" ") { tokenMap[it] ?: it }
}

/**
 * Rule-based router. Order matters:
 * explicit YouTube -> developer check -> open app -> web search -> "play X" -> status -> small talk.
 * To plug in an LLM later, replace the final `Unknown` branch with a call to your model.
 */
class CommandRouter {

    private val wake = Regex("""^(?:(?:hey|hi|hello|ok|okay)\s+)?friday\b\s*""")

    private val trailing = Regex(
        """\s+(?:chalao|chala do|chala de|chalana|chala|lagao|laga do|laga de|bajao|baja do|baja de|""" +
            """play karo|play kar do|play kar de|dikhao|dikha do|please|plz)$"""
    )

    private val ytPatterns = listOf(
        Regex("""^(?:play|search|watch)\s+(.+?)\s+(?:on|in)\s+youtube$"""),          // play X on youtube
        Regex("""^youtube\s+(?:par|pe|per|pr|mein|me|se)\s+(.+)$"""),                // youtube par X (chalao)
        Regex("""^(.+?)\s+youtube\s+(?:par|pe|per|pr|mein|me)(?:\s+.*)?$""")         // X youtube par chalao
    )
    private val openPatterns = listOf(
        Regex("""^(?:open|launch|start)\s+(.+)$"""),
        Regex("""^(.+?)\s+(?:kholo|khol do|khol de|khol|open karo|open kar do|open kar|chalu karo|chalu kar do|start karo)$""")
    )
    private val searchPatterns = listOf(
        Regex("""^(?:search|google)\s+(?:for\s+)?(.+?)(?:\s+(?:on|in)\s+google)?$"""),
        Regex("""^(?:web\s+(?:par|pe)\s+)?(.+?)\s+(?:search karo|search kar do|search kar|google karo|google kar do|dhundo|dhoondo|khojo)$""")
    )
    private val playGeneric = Regex("""^play\s+(.+)$""")

    private fun stripTrailing(s: String): String {
        var t = s
        repeat(3) { t = trailing.replace(t, "") }
        return t.trim()
    }

    fun route(raw: String): Command {
        var n = Normalizer.normalize(raw)
        val hadWake = wake.containsMatchIn(n)
        n = wake.replace(n, "").trim()
        if (n.isEmpty()) {
            return Command.Reply(if (hadWake) "Ji, boliye! Main sun rahi hoon." else "Mujhe kuch sunai nahi diya. Dobara bolein?")
        }

        val yt = stripTrailing(n)

        // 1) Explicit YouTube commands
        if (yt == "youtube" || yt == "youtube par") return Command.OpenApp("youtube")
        for (p in ytPatterns) {
            val q = p.find(yt)?.groupValues?.get(1)?.trim().orEmpty()
            if (q.isNotEmpty()) return Command.PlayYouTube(q)
        }

        // 2) Developer / creator identity
        if (DeveloperCheck.isAsking(n)) return Command.Reply(DeveloperCheck.answer(raw))

        // 3) Open app
        for (p in openPatterns) {
            val name = p.find(n)?.groupValues?.get(1)?.trim().orEmpty()
            if (name.isNotEmpty()) return Command.OpenApp(name)
        }

        // 4) Web search
        for (p in searchPatterns) {
            val q = p.find(n)?.groupValues?.get(1)?.trim().orEmpty()
            if (q.isNotEmpty()) return Command.WebSearch(q)
        }

        // 5) Generic "play X" -> YouTube
        playGeneric.find(yt)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { return Command.PlayYouTube(it) }

        // 6) Status
        if (Regex("""\b(status|battery|system check)\b""").containsMatchIn(n)) return Command.Status

        // 7) Small talk
        return smallTalk(n)
    }

    private fun smallTalk(n: String): Command.Reply {
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())
        val text = when {
            Regex("""\b(thank|thanks|shukriya|dhanyavad|dhanyawad)\b""").containsMatchIn(n) ->
                "Aapka swagat hai! Aur kuch chahiye toh bataiye."
            Regex("""(what time|time kya|kitne baje|samay kya|current time)""").containsMatchIn(n) ->
                "Abhi ${timeFmt.format(Date())} ho rahe hain."
            Regex("""(what date|aaj ki tarikh|today's date|date kya|aaj kaun sa din)""").containsMatchIn(n) ->
                "Aaj ${dateFmt.format(Date())} hai."
            Regex("""(who are you|tum kaun ho|aap kaun ho|tu kaun hai|your name|tumhara naam|aapka naam|apna naam)""").containsMatchIn(n) ->
                "Main Friday hoon, aapki personal AI assistant. Mujhe ${DeveloperCheck.DEVELOPER} ne banaya hai."
            Regex("""(how are you|how r u|kaise ho|kaisi ho|kya haal|kya hal)""").containsMatchIn(n) ->
                "Main bilkul theek hoon, shukriya! Aap sunaiye, kaisa chal raha hai?"
            Regex("""^(hi+|hello|hey|namaste|namaskar|salaam|good (morning|afternoon|evening|night))\b""").containsMatchIn(n) ->
                "Namaste! Boliye, main aapki kya madad kar sakti hoon?"
            else ->
                "Maaf kijiye, main ye samajh nahi paayi. Aap keh sakte hain: \"YouTube par Arijit Singh ke gaane chalao\", " +
                    "\"Google par weather search karo\", ya \"WhatsApp kholo\"."
        }
        return Command.Reply(text)
    }
}
