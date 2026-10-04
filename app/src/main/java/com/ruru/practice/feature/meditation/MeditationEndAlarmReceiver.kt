package com.ruru.practice.feature.meditation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ruru.practice.core.audio.BellSoundPlayer
import com.ruru.practice.core.audio.EndSessionAlert
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Fired by [MeditationEndScheduler] at the wall-clock end of a seat.
 * Delivers sound/vibration immediately (works while the UI is backgrounded),
 * then notifies the ViewModel to settle session state.
 */
class MeditationEndAlarmReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun endSessionAlert(): EndSessionAlert
        fun bellSoundPlayer(): BellSoundPlayer
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_END) return
        val app = context.applicationContext
        val endPrefs = app.getSharedPreferences(MeditationEndScheduler.PREFS, Context.MODE_PRIVATE)
        endPrefs.edit().remove(MeditationEndScheduler.KEY_END_AT).apply()

        val timerPrefs = app.getSharedPreferences("ruru_meditation_timer", Context.MODE_PRIVATE)
        val active = timerPrefs.getBoolean("active", false)
        val phase = timerPrefs.getString("phase", "") ?: ""
        // Skip if user already finished/reset, or another path already completed the seat.
        if (!active || (phase != "RUNNING" && phase != "PAUSED")) {
            return
        }

        val minutes = timerPrefs.getInt("minutes", 0)
        val longSeat = minutes >= 60

        if (longSeat) {
            MeditationKeepAliveService.notifyAlarm(app)
        } else {
            runCatching {
                val deps = EntryPointAccessors.fromApplication(app, Deps::class.java)
                deps.bellSoundPlayer().play(alarm = true)
                deps.endSessionAlert().vibrateEndPattern()
            }
        }
        MeditationTimerBridge.publishNaturalEndDue()
    }

    companion object {
        const val ACTION_END = "com.ruru.practice.meditation.ACTION_END_ALARM"
    }
}
