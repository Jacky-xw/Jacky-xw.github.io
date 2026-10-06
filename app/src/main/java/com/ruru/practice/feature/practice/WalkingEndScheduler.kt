package com.ruru.practice.feature.practice

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.ruru.practice.MainActivity

object WalkingEndScheduler {
    private const val REQ_ALARM = 33021
    private const val REQ_SHOW = 33022
    const val PREFS = "ruru_walking_end_alarm"
    const val KEY_END_AT = "end_at"

    fun schedule(context: Context, endAtMillis: Long) {
        val app = context.applicationContext
        if (endAtMillis <= System.currentTimeMillis() + 500L) {
            WalkingEndAlarmReceiver.fireNow(app)
            return
        }
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_END_AT, endAtMillis).apply()
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val op = PendingIntent.getBroadcast(
            app, REQ_ALARM,
            Intent(app, WalkingEndAlarmReceiver::class.java).setAction(WalkingEndAlarmReceiver.ACTION_END),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val show = PendingIntent.getActivity(
            app, REQ_SHOW,
            Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(endAtMillis, show), op)
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_END_AT).apply()
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val op = PendingIntent.getBroadcast(
            app, REQ_ALARM,
            Intent(app, WalkingEndAlarmReceiver::class.java).setAction(WalkingEndAlarmReceiver.ACTION_END),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.cancel(op)
    }
}
