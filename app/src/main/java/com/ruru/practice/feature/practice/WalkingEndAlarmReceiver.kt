package com.ruru.practice.feature.practice

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ruru.practice.core.audio.BellSoundPlayer
import com.ruru.practice.core.audio.EndSessionAlert
import com.ruru.practice.feature.meditation.MeditationKeepAliveService
import com.ruru.practice.feature.meditation.SessionEndNotifier
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class WalkingEndAlarmReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun endSessionAlert(): EndSessionAlert
        fun bellSoundPlayer(): BellSoundPlayer
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_END) return
        fireNow(context.applicationContext)
    }

    companion object {
        const val ACTION_END = "com.ruru.practice.walking.ACTION_END_ALARM"

        fun fireNow(app: Context) {
            app.getSharedPreferences(WalkingEndScheduler.PREFS, Context.MODE_PRIVATE)
                .edit().remove(WalkingEndScheduler.KEY_END_AT).apply()
            val timerPrefs = app.getSharedPreferences("ruru_walking_timer", Context.MODE_PRIVATE)
            val active = timerPrefs.getBoolean("active", false)
            val phase = timerPrefs.getString("phase", "") ?: ""
            if (!active || (phase != "RUNNING" && phase != "PAUSED")) return

            MeditationKeepAliveService.notifyEnd(app, longSeat = false)
            WalkingTimerBridge.publishNaturalEndDue()
        }
    }
}
