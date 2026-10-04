package com.ruru.practice.feature.meditation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

/**
 * Backup AlarmClock / exact-alarm path. Do NOT start activities from here
 * (Android 10+ background activity start restrictions). FGS + full-screen
 * notification is the supported pattern.
 */
class MeditationEndAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_END) return
        val pending = goAsync()
        val app = context.applicationContext
        val wl = runCatching {
            val pm = app.getSystemService(PowerManager::class.java)
            pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ruru:alarm_receiver")
                ?.apply { setReferenceCounted(false); acquire(30_000L) }
        }.getOrNull()
        try {
            app.getSharedPreferences(MeditationEndScheduler.PREFS, Context.MODE_PRIVATE)
                .edit().remove(MeditationEndScheduler.KEY_END_AT).apply()

            val timerPrefs = app.getSharedPreferences("ruru_meditation_timer", Context.MODE_PRIVATE)
            val walkPrefs = app.getSharedPreferences("ruru_walking_timer", Context.MODE_PRIVATE)
            val medPhase = timerPrefs.getString("phase", "") ?: ""
            val medActive = timerPrefs.getBoolean("active", false)
            val longSeat = timerPrefs.getInt("minutes", 0) >= 60
            val medOk = medActive && (medPhase == "RUNNING" || medPhase == "PAUSED")
            val walkOk = walkPrefs.getBoolean("active", false) &&
                (walkPrefs.getString("phase", "") == "RUNNING" ||
                    walkPrefs.getString("phase", "") == "PAUSED")
            if (!medOk && !walkOk) return

            MeditationKeepAliveService.notifyEnd(app, longSeat = medOk && longSeat)
            if (medOk) MeditationTimerBridge.publishNaturalEndDue()
            if (walkOk) {
                runCatching {
                    com.ruru.practice.feature.practice.WalkingTimerBridge.publishNaturalEndDue()
                }
            }
        } finally {
            runCatching { if (wl?.isHeld == true) wl.release() }
            pending.finish()
        }
    }

    companion object {
        const val ACTION_END = "com.ruru.practice.meditation.ACTION_END_ALARM"
    }
}
