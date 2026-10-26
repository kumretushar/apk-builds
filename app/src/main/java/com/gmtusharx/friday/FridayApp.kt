package com.gmtusharx.friday

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class FridayApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(CHANNEL_ID, "Friday check-ins", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Friendly check-ins when you've been quiet for a while" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "friday_checkin"
    }
}
