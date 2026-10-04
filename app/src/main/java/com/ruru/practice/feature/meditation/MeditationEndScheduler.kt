package com.ruru.practice.feature.meditation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Wall-clock end alarm so natural completion still fires when the UI process
 * is frozen in the background. The ViewModel ticker remains a secondary path.
 */
object MeditationEndScheduler {
    private const val REQ = 33011
    const val EXTRA_END_AT = "end_at_millis"
    const val PREFS = "ruru_meditation_end_alarm"
    const val KEY_END_AT = "end_at"

    fun schedule(context: Context, endAtMillis: Long) {
        if (endAtMillis <= System.currentTimeMillis()) {
            // Already due — fire path immediately via service.
            MeditationKeepAliveService.notifyAlarm(context.applicationContext)
            MeditationTimerBridge.publishNaturalEndDue()
            return
        }
        val app = context.applicationContext
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_END_AT, endAtMillis).apply()

        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = pendingIntent(app)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pi)
            } else {
                @Suppress("DEPRECATION")
                am.setExact(AlarmManager.RTC_WAKEUP, endAtMillis, pi)
            }
        } catch (_: SecurityException) {
            // Exact-alarm permission missing: best-effort windowed alarm.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pi)
        }
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_END_AT).apply()
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(app))
    }

    fun scheduledEndAt(context: Context): Long =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_END_AT, 0L)

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MeditationEndAlarmReceiver::class.java).apply {
            action = MeditationEndAlarmReceiver.ACTION_END
        }
        return PendingIntent.getBroadcast(
            context,
            REQ,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
