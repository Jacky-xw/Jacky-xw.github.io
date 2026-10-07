package com.ruru.practice.core.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ruru.practice.feature.meditation.MeditationEndScheduler
import com.ruru.practice.feature.practice.WalkingEndScheduler

/**
 * After reboot, re-arm wall-clock end alarms for any still-active timers.
 */
class TimerBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return
        val app = context.applicationContext
        com.ruru.practice.feature.meditation.TimerDiagnostics.record(app, "boot_receiver action=${intent?.action}")

        // Meditation
        val med = app.getSharedPreferences("ruru_meditation_timer", Context.MODE_PRIVATE)
        if (med.getBoolean("active", false)) {
            val phase = med.getString("phase", "") ?: ""
            if (phase == "RUNNING") {
                val minutes = med.getInt("minutes", 20).coerceIn(1, 360)
                val paused = med.getInt("paused_elapsed", 0).coerceAtLeast(0)
                val started = med.getLong("started_at", 0L)
                val elapsed = if (started > 0L) {
                    paused + ((System.currentTimeMillis() - started) / 1000L).toInt()
                } else paused
                val remaining = (minutes * 60 - elapsed).coerceAtLeast(0)
                if (remaining > 0) {
                    MeditationEndScheduler.schedule(app, System.currentTimeMillis() + remaining * 1000L, longSeat = minutes >= 60)
                } else {
                    MeditationEndScheduler.schedule(app, System.currentTimeMillis(), longSeat = minutes >= 60)
                }
            }
        }

        // Walking
        val walk = app.getSharedPreferences("ruru_walking_timer", Context.MODE_PRIVATE)
        if (walk.getBoolean("active", false)) {
            val phase = walk.getString("phase", "") ?: ""
            if (phase == "RUNNING") {
                val target = walk.getInt("target", 3600).coerceAtLeast(1)
                val storedElapsed = walk.getInt("elapsed", 0).coerceAtLeast(0)
                val started = walk.getLong("started_at", 0L)
                val elapsed = if (started > 0L) {
                    storedElapsed + ((System.currentTimeMillis() - started) / 1000L).toInt()
                } else storedElapsed
                val remaining = (target - elapsed).coerceAtLeast(0)
                if (remaining > 0) {
                    WalkingEndScheduler.schedule(app, System.currentTimeMillis() + remaining * 1000L)
                } else {
                    WalkingEndScheduler.schedule(app, System.currentTimeMillis())
                }
            }
        }
    }
}
