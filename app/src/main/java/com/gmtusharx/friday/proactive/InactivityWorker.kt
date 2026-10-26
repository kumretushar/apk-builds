package com.gmtusharx.friday.proactive

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.gmtusharx.friday.FridayApp
import com.gmtusharx.friday.MainActivity
import com.gmtusharx.friday.data.FridayPrefs
import com.gmtusharx.friday.voice.Speaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class InactivityWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        if (!FridayPrefs(applicationContext).proactiveEnabled) return Result.success()
        val text = ProactivePrompts.random()
        notify(text)
        speakAloud(text)
        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun notify(text: String) {
        val nm = NotificationManagerCompat.from(applicationContext)
        if (!nm.areNotificationsEnabled()) return
        val open = PendingIntent.getActivity(
            applicationContext, 0,
            Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(applicationContext, FridayApp.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("Friday")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        runCatching { nm.notify(1001, n) }
    }

    private suspend fun speakAloud(text: String) = withContext(Dispatchers.Main) {
        withTimeoutOrNull(25_000) {
            suspendCancellableCoroutine<Unit> { cont ->
                val speaker = Speaker(applicationContext)
                cont.invokeOnCancellation { speaker.shutdown() }
                speaker.speak(text) {
                    speaker.shutdown()
                    if (cont.isActive) cont.resume(Unit)
                }
            }
        }
    }
}
