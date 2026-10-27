package com.gmtusharx.friday.logic

object DeveloperCheck {
    const val DEVELOPER = "GM TUSHARX" // Yahan aap apna naam daal sakte hain

    // Yeh check karega ki user developer ke baare mein pooch raha hai ya nahi
    fun isAsking(normalizedText: String): Boolean {
        val keywords = listOf("developer", "creator", "banaya", "kisne", "kaun")
        return keywords.any { normalizedText.contains(it) }
    }

    // Agar user pooch raha hai, toh yeh jawab dega
    fun answer(rawText: String): String {
        return "Mujhe $DEVELOPER ne banaya hai. Main unki personal AI assistant hoon."
    }
}