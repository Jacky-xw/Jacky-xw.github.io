package com.ruru.practice.feature.meditation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

class MeditationEndAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_END) return
        val pending = goAsync()
        val app = context.applicationContext
        val wl = runCatching {
            app.getSystemService(PowerManager::class.java)
                ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ruru:end_rx")
                ?.apply { setReferenceCounted(false); acquire(45_000L) }
        }.getOrNull()
        try {
            val timerPrefs = app.getSharedPreferences("ruru_meditation_timer", Context.MODE_PRIVATE)
            val longSeat = timerPrefs.getInt("minutes", 0) >= 60
            // Always hand off to FGS — phase guard lives inside service.
            MeditationKeepAliveService.notifyEnd(app, longSeat = longSeat)
            MeditationTimerBridge.publishNaturalEndDue()
        } finally {
            runCatching { if (wl?.isHeld == true) wl.release() }
            pending.finish()
        }
    }

    companion object {
        const val ACTION_END = "com.ruru.practice.meditation.ACTION_END_ALARM"
    }
}
