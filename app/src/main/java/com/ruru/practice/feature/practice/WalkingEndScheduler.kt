package com.ruru.practice.feature.practice

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/** Wall-clock end alarm for walking timer (background-safe). */
object WalkingEndScheduler {
    private const val REQ = 33021
    const val PREFS = "ruru_walking_end_alarm"
    const val KEY_END_AT = "end_at"

    fun schedule(context: Context, endAtMillis: Long) {
        val app = context.applicationContext
        if (endAtMillis <= System.currentTimeMillis()) {
            WalkingTimerBridge.publishNaturalEndDue()
            return
        }
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
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, pi)
        }
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_END_AT).apply()
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pendingIntent(app))
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, WalkingEndAlarmReceiver::class.java).apply {
            action = WalkingEndAlarmReceiver.ACTION_END
        }
        return PendingIntent.getBroadcast(
            context, REQ, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
