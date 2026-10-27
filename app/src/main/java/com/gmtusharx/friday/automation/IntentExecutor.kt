package com.gmtusharx.friday.automation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Executes device actions through plain Android Intents (no accessibility hacks, no root). */
class IntentExecutor(private val context: Context) {

    private companion object {
        const val YOUTUBE = "com.google.android.youtube"
    }

    private fun start(intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    // ---------- Existing ----------

    fun playYouTube(query: String): Boolean {
        val url = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
        val inApp = Intent(Intent.ACTION_VIEW, url).setPackage(YOUTUBE)
        return start(inApp) || start(Intent(Intent.ACTION_VIEW, url))
    }

    fun webSearch(query: String): Boolean {
        val url = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
        return start(Intent(Intent.ACTION_VIEW, url))
    }

    fun openApp(spokenName: String): Boolean {
        val name = spokenName.trim().lowercase()
        if (name.isEmpty()) return false
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, 0)
            .filter { it.activityInfo.packageName != context.packageName }
        fun label(i: android.content.pm.ResolveInfo) = i.loadLabel(pm).toString().lowercase()
        val match = apps.firstOrNull { label(it) == name }
            ?: apps.firstOrNull { label(it).contains(name) }
            ?: apps.firstOrNull { name.contains(label(it)) && label(it).length >= 4 }
        val pkg = match?.activityInfo?.packageName ?: return false
        val intent = pm.getLaunchIntentForPackage(pkg) ?: return false
        return start(intent)
    }

    fun statusReport(): String {
        val bi = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = bi?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = bi?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level >= 0 && scale > 0) level * 100 / scale else -1
        val st = bi?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL

        val cm = context.getSystemService(ConnectivityManager::class.java)
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        val online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val time = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        val battery = if (pct >= 0) "Battery $pct% hai${if (charging) " (charging)" else ""}" else "Battery ka status nahi mila"
        val net = if (online) "internet connected hai" else "internet connected nahi hai"
        return "Sab systems theek hain. $battery, $net, aur abhi $time ho rahe hain."
    }

    // ---------- NEW: Contact-based actions ----------

    private fun findNumber(name: String): String? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val proj = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val sel = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        context.contentResolver.query(uri, proj, sel, arrayOf("%$name%"), null)?.use { c ->
            if (c.moveToFirst()) {
                return c.getString(0).replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
            }
        }
        return null
    }

    fun call(contact: String): Boolean {
        val number = findNumber(contact) ?: return false
        return start(Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")))
    }

    fun whatsapp(contact: String): Boolean {
        val number = findNumber(contact) ?: return false
        val clean = number.removePrefix("+").removePrefix("91")
        return start(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91$clean")))
    }

    fun sms(contact: String, message: String): Boolean {
        val number = findNumber(contact) ?: return false
        return start(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number"))
            .putExtra("sms_body", message))
    }

    // ---------- NEW: Alarm / Camera / Photos ----------

    fun setAlarm(time: String): Boolean {
        val cleaned = time.uppercase().replace(" ", "")
        val isPm = cleaned.contains("PM")
        val parts = cleaned.replace("AM", "").replace("PM", "").split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return false
        val min = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val finalHour = if (isPm && hour < 12) hour + 12 else if (!isPm && hour == 12) 0 else hour
        return start(Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, finalHour)
            .putExtra(AlarmClock.EXTRA_MINUTES, min)
            .putExtra(AlarmClock.EXTRA_MESSAGE, "Friday Alarm"))
    }

    fun openCamera(): Boolean = start(Intent(MediaStore.ACTION_IMAGE_CAPTURE))

    fun openPhotos(): Boolean = start(Intent(Intent.ACTION_VIEW,
        Uri.parse("content://media/internal/images/media")))
}