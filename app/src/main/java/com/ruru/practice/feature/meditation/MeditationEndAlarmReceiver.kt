package com.ruru.practice.feature.meditation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * AlarmClock callback. Must only hand off to the foreground service quickly —
 * do not play MediaPlayer here (receiver time budget is short, and background
 * playback is often blocked).
 */
class MeditationEndAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_END) return
        val app = context.applicationContext
        app.getSharedPreferences(MeditationEndScheduler.PREFS, Context.MODE_PRIVATE)
            .edit().remove(MeditationEndScheduler.KEY_END_AT).apply()

        val timerPrefs = app.getSharedPreferences("ruru_meditation_timer", Context.MODE_PRIVATE)
        val active = timerPrefs.getBoolean("active", false)
        val phase = timerPrefs.getString("phase", "") ?: ""
        if (!active || (phase != "RUNNING" && phase != "PAUSED")) return

        val longSeat = timerPrefs.getInt("minutes", 0) >= 60
        // Start FGS under the AlarmManager exemption, then notify ViewModel.
        MeditationKeepAliveService.notifyEnd(app, longSeat = longSeat)
        MeditationTimerBridge.publishNaturalEndDue()
    }

    companion object {
        const val ACTION_END = "com.ruru.practice.meditation.ACTION_END_ALARM"
    }
}
