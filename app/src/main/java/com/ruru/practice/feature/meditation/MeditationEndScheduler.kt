package com.ruru.practice.feature.meditation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ruru.practice.MainActivity

/**
 * Schedules a user-visible wall-clock end using [AlarmManager.setAlarmClock].
 * Unlike setExactAndAllowWhileIdle, AlarmClock is treated as a real alarm by OEMs
 * and reliably wakes the app from Doze / background freeze.
 */
object MeditationEndScheduler {
    private const val REQ_ALARM = 33011
    private const val REQ_SHOW = 33012
    const val PREFS = "ruru_meditation_end_alarm"
    const val KEY_END_AT = "end_at"

    fun schedule(context: Context, endAtMillis: Long) {
        val app = context.applicationContext
        if (endAtMillis <= System.currentTimeMillis() + 500L) {
            MeditationKeepAliveService.notifyEnd(app, longSeat = isLongSeat(app))
            MeditationTimerBridge.publishNaturalEndDue()
            return
        }
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_END_AT, endAtMillis).apply()

        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val op = alarmPendingIntent(app)
        val show = showPendingIntent(app)
        // setAlarmClock is the most reliable background trigger on modern Android.
        am.setAlarmClock(AlarmManager.AlarmClockInfo(endAtMillis, show), op)
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_END_AT).apply()
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(alarmPendingIntent(app))
    }

    fun scheduledEndAt(context: Context): Long =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_END_AT, 0L)

    private fun isLongSeat(context: Context): Boolean {
        val minutes = context.getSharedPreferences("ruru_meditation_timer", Context.MODE_PRIVATE)
            .getInt("minutes", 0)
        return minutes >= 60
    }

    private fun alarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MeditationEndAlarmReceiver::class.java).apply {
            action = MeditationEndAlarmReceiver.ACTION_END
        }
        return PendingIntent.getBroadcast(
            context, REQ_ALARM, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        return PendingIntent.getActivity(
            context, REQ_SHOW, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
