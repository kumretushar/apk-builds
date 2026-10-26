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
}
