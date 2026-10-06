package com.ruru.practice.feature.meditation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.ruru.practice.MainActivity

/** One system wakeup alarm -> explicit receiver -> foreground alert service. */
object MeditationEndScheduler {
    private const val REQ_ALARM = 33011
    private const val REQ_SHOW = 33012
    const val PREFS = "ruru_meditation_end_alarm"
    const val KEY_END_AT = "end_at"

    fun schedule(context: Context, endAtMillis: Long, longSeat: Boolean = false) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val am = requireNotNull(app.getSystemService(AlarmManager::class.java))
        // Clean up pre-1.41.1 direct-service alarm when upgrading.
        cancelLegacy(app, am)
        prefs.edit().putLong(KEY_END_AT, endAtMillis).apply()
        val op = PendingIntent.getBroadcast(
            app, REQ_ALARM, alarmIntent(app)
                .putExtra(MeditationKeepAliveService.EXTRA_END_AT, endAtMillis)
                .putExtra(MeditationKeepAliveService.EXTRA_LONG_SEAT, longSeat),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val show = PendingIntent.getActivity(
            app, REQ_SHOW, Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Even an already due deadline goes through the system delivery path.
        val trigger = maxOf(endAtMillis, System.currentTimeMillis() + 1L)
        runCatching {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(trigger, show), op)
            TimerDiagnostics.record(app, "alarm_scheduled deadline=$endAtMillis long=$longSeat")
        }.onFailure { first ->
            TimerDiagnostics.record(app, "alarm_clock_failed ${first.javaClass.simpleName}")
            runCatching {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, op)
                TimerDiagnostics.record(app, "alarm_fallback_scheduled deadline=$endAtMillis")
            }.onFailure { error ->
                TimerDiagnostics.record(app, "ALARM_SCHEDULE_FAILED ${error.javaClass.simpleName}: ${error.message}")
            }
        }
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_END_AT).apply()
        val am = requireNotNull(app.getSystemService(AlarmManager::class.java))
        val op = PendingIntent.getBroadcast(
            app, REQ_ALARM, alarmIntent(app), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (op != null) am.cancel(op)
        cancelLegacy(app, am)
        TimerDiagnostics.record(app, "alarm_cancelled")
    }

    private fun alarmIntent(context: Context) = Intent(context, MeditationEndAlarmReceiver::class.java)
        .setAction(MeditationEndAlarmReceiver.ACTION_END)

    private fun cancelLegacy(context: Context, am: AlarmManager) {
        val old = PendingIntent.getForegroundService(
            context, REQ_ALARM,
            Intent(context, MeditationKeepAliveService::class.java).setAction(MeditationKeepAliveService.ACTION_ALARM),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (old != null) { am.cancel(old); old.cancel() }
    }
}
