package com.ruru.practice.feature.meditation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.ruru.practice.MainActivity

/**
 * Industry pattern for timer/alarm apps:
 * - setAlarmClock (strongest Doze exemption, status-bar clock)
 * - plus setExactAndAllowWhileIdle as second line
 * - operation targets the foreground service (preferred over pure broadcast on OEM ROMs)
 */
object MeditationEndScheduler {
    private const val REQ_ALARM_SERVICE = 33011
    private const val REQ_ALARM_BROADCAST = 33013
    private const val REQ_SHOW = 33012
    const val PREFS = "ruru_meditation_end_alarm"
    const val KEY_END_AT = "end_at"

    fun schedule(context: Context, endAtMillis: Long, longSeat: Boolean = false) {
        val app = context.applicationContext
        if (endAtMillis <= System.currentTimeMillis() + 500L) {
            MeditationKeepAliveService.notifyEnd(app, longSeat = longSeat)
            MeditationTimerBridge.publishNaturalEndDue()
            return
        }
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_END_AT, endAtMillis).apply()

        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val servicePi = servicePendingIntent(app, longSeat)
        val broadcastPi = broadcastPendingIntent(app)
        val showPi = showPendingIntent(app)

        // 1) User-visible alarm clock (best Doze / OEM behavior)
        runCatching {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(endAtMillis, showPi), servicePi)
        }.onFailure {
            // 2) Fallback exact idle alarm
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, servicePi)
                } else {
                    @Suppress("DEPRECATION")
                    am.setExact(AlarmManager.RTC_WAKEUP, endAtMillis, servicePi)
                }
            }
        }

        // 3) Secondary broadcast path (some OEMs deliver one better than the other)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endAtMillis, broadcastPi)
            }
        }
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_END_AT).apply()
        val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(servicePendingIntent(app, longSeat = false))
        am.cancel(broadcastPendingIntent(app))
    }

    private fun servicePendingIntent(context: Context, longSeat: Boolean): PendingIntent {
        val intent = Intent(context, MeditationKeepAliveService::class.java).apply {
            action = MeditationKeepAliveService.ACTION_ALARM
            putExtra(MeditationKeepAliveService.EXTRA_LONG_SEAT, longSeat)
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(
                context, REQ_ALARM_SERVICE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            PendingIntent.getService(
                context, REQ_ALARM_SERVICE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    private fun broadcastPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MeditationEndAlarmReceiver::class.java).apply {
            action = MeditationEndAlarmReceiver.ACTION_END
        }
        return PendingIntent.getBroadcast(
            context, REQ_ALARM_BROADCAST, intent,
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
