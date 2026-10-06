package com.ruru.practice.feature.meditation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ruru.practice.MainActivity
import com.ruru.practice.core.util.ExactAlarmPermission

/**
 * Single wall-clock end: setAlarmClock → ForegroundService.
 * Avoid scheduling two operations for the same instant (that caused duplicate alerts).
 */
object MeditationEndScheduler {
    private const val REQ_ALARM = 33011
    private const val REQ_SHOW = 33012
    const val PREFS = "ruru_meditation_end_alarm"
    const val KEY_END_AT = "end_at"

    fun schedule(context: Context, endAtMillis: Long, longSeat: Boolean = false) {
        val app = context.applicationContext
        if (endAtMillis <= System.currentTimeMillis() + 400L) {
            MeditationKeepAliveService.notifyEnd(app, longSeat = longSeat)
            return
        }
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_END_AT, endAtMillis).apply()

        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val op = servicePendingIntent(app, longSeat, endAtMillis)
        val show = showPendingIntent(app)

        // Cancel any previous before re-arming
        runCatching { am.cancel(op) }

        val canExact = ExactAlarmPermission.canSchedule(app)
        if (canExact) {
            runCatching {
                am.setAlarmClock(AlarmManager.AlarmClockInfo(endAtMillis, show), op)
            }.onFailure {
                setExactFallback(am, endAtMillis, op)
            }
        } else {
            setExactFallback(am, endAtMillis, op)
        }
    }

    private fun setExactFallback(am: AlarmManager, endAtMillis: Long, op: PendingIntent) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, op)
            } else {
                @Suppress("DEPRECATION")
                am.setExact(AlarmManager.RTC_WAKEUP, endAtMillis, op)
            }
        }.onFailure { android.util.Log.e("PracticeTimer", "Unable to schedule exact end alarm", it) }
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_END_AT).apply()
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Do not UPDATE_CURRENT while cancelling: a delivered PendingIntent may
        // still be in flight, and rewriting its extras would erase its deadline.
        val intent = Intent(app, MeditationKeepAliveService::class.java)
            .setAction(MeditationKeepAliveService.ACTION_ALARM)
        val op = PendingIntent.getForegroundService(
            app, REQ_ALARM, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (op != null) am.cancel(op)
    }

    private fun servicePendingIntent(context: Context, longSeat: Boolean, endAtMillis: Long = 0L): PendingIntent {
        val intent = Intent(context, MeditationKeepAliveService::class.java).apply {
            action = MeditationKeepAliveService.ACTION_ALARM
            putExtra(MeditationKeepAliveService.EXTRA_LONG_SEAT, longSeat)
            putExtra(MeditationKeepAliveService.EXTRA_END_AT, endAtMillis)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                context, REQ_ALARM, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                context, REQ_ALARM, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
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
