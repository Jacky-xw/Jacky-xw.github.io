package com.ruru.practice.feature.meditation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * AlarmClock callback. Starts the foreground service end path immediately.
 * Must stay lightweight — no MediaPlayer here.
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
        // Allow COMPLETED + end_alarm to re-fire after process death for long seats.
        val longSeat = timerPrefs.getInt("minutes", 0) >= 60
        val allow = active && (
            phase == "RUNNING" || phase == "PAUSED" ||
                (longSeat && phase == "COMPLETED" && timerPrefs.getBoolean("end_alarm_active", false))
            )
        if (!allow && phase != "RUNNING" && phase != "PAUSED") {
            // Walking may also share scheduler in some builds; still try service if walking active.
            val walk = app.getSharedPreferences("ruru_walking_timer", Context.MODE_PRIVATE)
            val walkOk = walk.getBoolean("active", false) &&
                (walk.getString("phase", "") == "RUNNING" || walk.getString("phase", "") == "PAUSED")
            if (!walkOk) return
            MeditationKeepAliveService.notifyEnd(app, longSeat = false)
            return
        }
        if (!allow) return

        MeditationKeepAliveService.notifyEnd(app, longSeat = longSeat)
        MeditationTimerBridge.publishNaturalEndDue()
    }

    companion object {
        const val ACTION_END = "com.ruru.practice.meditation.ACTION_END_ALARM"
    }
}
