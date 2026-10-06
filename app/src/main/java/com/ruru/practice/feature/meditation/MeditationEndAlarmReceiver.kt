package com.ruru.practice.feature.meditation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ruru.practice.core.util.TimerDeadline

class MeditationEndAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_END) return
        val app = context.applicationContext
        val deadline = intent.getLongExtra(MeditationKeepAliveService.EXTRA_END_AT, 0L)
        val expected = app.getSharedPreferences(MeditationEndScheduler.PREFS, Context.MODE_PRIVATE)
            .getLong(MeditationEndScheduler.KEY_END_AT, 0L)
        TimerDiagnostics.record(app, "alarm_received deadline=$deadline expected=$expected lateMs=${System.currentTimeMillis() - deadline}")
        if (!TimerDeadline.matchesAlarm(deadline, expected)) {
            TimerDiagnostics.record(app, "alarm_ignored_stale")
            return
        }
        // Keep CPU awake AFTER onReceive returns, until the service takes ownership.
        // Never publish completion here: the service has not played anything yet.
        runCatching {
            TimerAlarmHandoff.acquire(app)
            MeditationKeepAliveService.notifyEnd(
                app,
                longSeat = intent.getBooleanExtra(MeditationKeepAliveService.EXTRA_LONG_SEAT, false),
                alarmDeadline = deadline
            )
            TimerDiagnostics.record(app, "alarm_service_requested")
        }.onFailure {
            TimerDiagnostics.record(app, "ALARM_SERVICE_FAILED ${it.javaClass.simpleName}: ${it.message}")
            TimerAlarmHandoff.release()
        }
    }

    companion object {
        const val ACTION_END = "com.ruru.practice.meditation.ACTION_END_ALARM"
    }
}
