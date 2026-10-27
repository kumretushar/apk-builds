package com.gmtusharx.friday.data

import android.content.Context

class FridayPrefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("friday_prefs", Context.MODE_PRIVATE)

    /** Silence threshold (minutes) before Friday checks in. */
    var inactivityMinutes: Int
        get() = sp.getInt("inactivity_min", 3)
        set(v) = sp.edit().putInt("inactivity_min", v.coerceIn(1, 120)).apply()

    var proactiveEnabled: Boolean
        get() = sp.getBoolean("proactive", true)
        set(v) = sp.edit().putBoolean("proactive", v).apply()

    /** Speech-recognition language tag: "hi-IN" (Hindi/Hinglish) or "en-IN". */
    var language: String
        get() = sp.getString("lang", "hi-IN") ?: "hi-IN"
        set(v) = sp.edit().putString("lang", v).apply()

    /** TTS speed (0.5 = slow, 1.0 = normal, 1.5 = fast). */
    var speechRate: Float
        get() = sp.getFloat("speech_rate", 0.85f)
        set(v) = sp.edit().putFloat("speech_rate", v.coerceIn(0.5f, 1.5f)).apply()
}