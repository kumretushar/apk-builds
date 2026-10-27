package com.gmtusharx.friday.logic

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Command sealed interface alag file (Command.kt) mein hai — import ki zaroorat nahi
// kyunki same package mein hai.

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
        "टाइम" to "time", "बजे" to "baje", "कितने" to "kitne",
        "कॉल" to "call", "मैसेज" to "message", "अलार्म" to "alarm",
        "कैमरा" to "camera", "फोटो" to "photo", "तस्वीर" to "photo"
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

class CommandRouter {

    private val wake = Regex("""^(?:(?:hey|hi|hello|ok|okay)\s+)?friday\b\s*""")

    private val trailing = Regex(
        """\s+(?:chalao|chala do|chala de|chalana|chala|lagao|laga do|laga de|bajao|baja do|baja de|""" +
            """play karo|play kar do|play kar de|dikhao|dikha do|please|plz)$"""
    )

    private val ytPatterns = listOf(
        Regex("""^(?:play|search|watch)\s+(.+?)\s+(?:on|in)\s+youtube$"""),
        Regex("""^youtube\s+(?:par|pe|per|pr|mein|me|se)\s+(.+)$"""),
        Regex("""^(.+?)\s+youtube\s+(?:par|pe|per|pr|mein|me)(?:\s+.*)?$""")
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

    private val callPattern = Regex("""^(.+?)\s+(?:ko\s+)?(?:call|phone)\s+(?:karo|kar do|kar|lagao|lag do)$""")
    private val waPattern = Regex("""^(.+?)\s+(?:ko\s+)?whatsapp\s+(?:karo|kar do|kar|bhejo|bhej do|kholo|khol)$""")
    private val smsPattern = Regex("""^(.+?)\s+(?:ko\s+)?(?:sms|message)\s+(?:bhejo|bhej do|karo|kar)\s*(.*)$""")
    private val alarmPattern = Regex("""^(.+?)\s+(?:ka\s+)?alarm\s+(?:lagao|lag do|set karo|set kar do)$""")
    private val cameraRegex = Regex("""^(?:camera|kamera)\s+(?:kholo|khol|open karo|open kar)$""")
    private val photosRegex = Regex("""^(?:photo|photos|gallery|tasveer)\s+(?:kholo|khol|open karo|open kar|dikhao)$""")

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
            return Command.Reply(
                if (hadWake) "Ji, boliye! Main sun rahi hoon."
                else "Mujhe kuch sunai nahi diya. Dobara bolein?"
            )
        }

        val yt = stripTrailing(n)

        if (yt == "youtube" || yt == "youtube par") return Command.OpenApp("youtube")
        for (p in ytPatterns) {
            val q = p.find(yt)?.groupValues?.get(1)?.trim().orEmpty()
            if (q.isNotEmpty()) return Command.PlayYouTube(q)
        }

        if (DeveloperCheck.isAsking(n)) return Command.Reply(DeveloperCheck.answer(raw))

        callPattern.find(n)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { return Command.Call(it) }

        waPattern.find(n)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { return Command.WhatsApp(it) }

        smsPattern.find(n)?.let {
            val name = it.groupValues[1].trim()
            val msg = it.groupValues[2].trim()
            if (name.isNotEmpty()) return Command.Sms(name, msg)
        }

        alarmPattern.find(n)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { return Command.SetAlarm(it) }

        if (cameraRegex.containsMatchIn(n)) return Command.OpenCamera
        if (photosRegex.containsMatchIn(n)) return Command.OpenPhotos

        for (p in openPatterns) {
            val name = p.find(n)?.groupValues?.get(1)?.trim().orEmpty()
            if (name.isNotEmpty()) return Command.OpenApp(name)
        }

        for (p in searchPatterns) {
            val q = p.find(n)?.groupValues?.get(1)?.trim().orEmpty()
            if (q.isNotEmpty()) return Command.WebSearch(q)
        }

        playGeneric.find(yt)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { return Command.PlayYouTube(it) }

        if (Regex("""\b(status|battery|system check)\b""").containsMatchIn(n)) return Command.Status

        return smallTalk(n, raw)
    }

    private fun smallTalk(n: String, raw: String): Command {
        val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateFmt = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault())

        return when {
            Regex("""\b(thank|thanks|shukriya|dhanyavad|dhanyawad)\b""").containsMatchIn(n) ->
                Command.Reply("Aapka swagat hai! Aur kuch chahiye toh bataiye.")

            Regex("""(what time|time kya|kitne baje|samay kya|current time)""").containsMatchIn(n) ->
                Command.Reply("Abhi ${timeFmt.format(Date())} ho rahe hain.")

            Regex("""(what date|aaj ki tarikh|today's date|date kya|aaj kaun sa din)""").containsMatchIn(n) ->
                Command.Reply("Aaj ${dateFmt.format(Date())} hai.")

            Regex("""(who are you|tum kaun ho|aap kaun ho|tu kaun hai|your name|tumhara naam|aapka naam|apna naam)""").containsMatchIn(n) ->
                Command.Reply("Main Friday hoon, aapki personal AI assistant. Mujhe ${DeveloperCheck.DEVELOPER} ne banaya hai.")

            Regex("""(how are you|how r u|kaise ho|kaisi ho|kya haal|kya hal)""").containsMatchIn(n) ->
                Command.Reply("Main bilkul theek hoon, shukriya! Aap sunaiye, kaisa chal raha hai?")

            Regex("""^(hi+|hello|hey|namaste|namaskar|salaam|good (morning|afternoon|evening|night))\b""").containsMatchIn(n) ->
                Command.Reply("Namaste! Boliye, main aapki kya madad kar sakti hoon?")

            else -> Command.Gemini(raw)
        }
    }
}