package com.ruru.practice.feature.meditation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * AlarmClock callback for lock-screen / Doze delivery.
 * Starts [SessionEndActivity] which plays the ALARM-stream bell over the lock.
 */
class MeditationEndAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_END) return
        val app = context.applicationContext
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

        val long = medOk && longSeat
        SessionEndActivity.launch(app, longSeat = long)
        // Keep FGS alive for priority; endFired path updates state without requiring UI.
        MeditationKeepAliveService.notifyEnd(app, longSeat = long)
        if (medOk) MeditationTimerBridge.publishNaturalEndDue()
        if (walkOk) {
            runCatching {
                com.ruru.practice.feature.practice.WalkingTimerBridge.publishNaturalEndDue()
            }
        }
    }

    companion object {
        const val ACTION_END = "com.ruru.practice.meditation.ACTION_END_ALARM"
    }
}
