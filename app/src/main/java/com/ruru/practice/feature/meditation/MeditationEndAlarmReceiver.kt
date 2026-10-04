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
        app.getSharedPreferences(MeditationEndScheduler.PREFS, Context.MODE_PRIVATE)
            .edit().remove(MeditationEndScheduler.KEY_END_AT).apply()

        val minutes = app.getSharedPreferences("ruru_meditation_timer", Context.MODE_PRIVATE)
            .getInt("minutes", 0)
        val longSeat = minutes >= 60

        if (longSeat) {
            // Foreground service owns looping alarm audio + vibration under lock-screen.
            MeditationKeepAliveService.notifyAlarm(app)
        } else {
            runCatching {
                val deps = EntryPointAccessors.fromApplication(app, Deps::class.java)
                deps.bellSoundPlayer().play()
                deps.endSessionAlert().vibrateEndPattern()
            }
        }
        MeditationTimerBridge.publishNaturalEndDue()
    }

    companion object {
        const val ACTION_END = "com.ruru.practice.meditation.ACTION_END_ALARM"
    }
}
