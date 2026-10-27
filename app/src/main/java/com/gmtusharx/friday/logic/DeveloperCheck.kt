package com.gmtusharx.friday.logic

/**
 * Answers "Who made you?" in English, Hindi (Devanagari) and Hinglish.
 * Input must already be normalized (see Normalizer) so Devanagari keywords are transliterated.
 */
object DeveloperCheck {
    const val DEVELOPER = "GM TUSHARX"

    private val patterns = listOf(
        "who made you", "who made u", "who created you", "who created u", "who built you",
        "who developed you", "who developed u", "who programmed you", "who is your developer",
        "who is your creator", "who is your maker", "who's your developer", "whos your developer",
        "who's your creator", "who is ur developer", "your developer", "your creator", "your maker",
        "ur developer", "ur creator",
        "kisne banaya", "kisne bnaya", "kisne banayi", "kis ne banaya", "kisne create",
        "kisne tumhe", "kisne aapko", "kisne tujhe",
        "developer kaun", "developer kon", "creator kaun", "creator kon",
        "tumhara developer", "tumhara creator", "aapka developer", "aapka creator",
        "tera developer", "tera creator", "tumhara maker", "aapka maker"
    )

    private val hinglishMarkers = Regex("""\b(kisne|kis ne|kaun|kon|tumhe|tumhara|aapko|aapka|tujhe|tera|banaya|bnaya|banayi)\b""")
    private val devanagari = Regex("""[\u0900-\u097F]""")

    fun isAsking(normalized: String): Boolean = patterns.any { normalized.contains(it) }

    /** [raw] is the original text, used only to pick the reply language. */
    fun answer(raw: String): String = when {
        devanagari.containsMatchIn(raw) ->
            "मुझे $DEVELOPER ने बनाया है। वही मेरे क्रिएटर और डेवलपर हैं, और मुझे उन पर बहुत गर्व है!"
        hinglishMarkers.containsMatchIn(raw.lowercase()) ->
            "Mujhe $DEVELOPER ne banaya hai. Wahi mere creator aur developer hain, aur mujhe un par bahut garv hai!"
        else ->
            "I was created and developed by $DEVELOPER. He's my creator, and I'm proud to carry his work."
    }
}
