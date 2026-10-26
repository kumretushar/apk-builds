package com.gmtusharx.friday.proactive

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Background inactivity check-in. PeriodicWork has a 15-minute minimum, so we use a delayed
 * one-time job that is re-armed whenever the app goes to background and cancelled when it returns.
 * (While the app is on screen, FridayViewModel runs a precise coroutine timer instead.)
 */
object ProactiveScheduler {
    private const val WORK_NAME = "friday_inactivity"

    fun schedule(context: Context, minutes: Int) {
        val request = OneTimeWorkRequestBuilder<InactivityWorker>()
            .setInitialDelay(minutes.toLong(), TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}

object ProactivePrompts {
    private val prompts = listOf(
        "Aap kaafi der se chup hain, sab theek hai na?",
        "Kuch chahiye toh bol dijiye, main yahin hoon.",
        "Arre, kahan kho gaye? Chai pi li ya nahi?",
        "Thak gaye ho toh thoda break le lijiye. Main koi gaana laga doon?"
    )
    fun random(): String = prompts.random()
}
