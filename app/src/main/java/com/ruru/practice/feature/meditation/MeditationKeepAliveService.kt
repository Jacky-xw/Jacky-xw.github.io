package com.ruru.practice.feature.meditation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ruru.practice.MainActivity
import com.ruru.practice.R
import com.ruru.practice.core.audio.BellSoundPlayer
import com.ruru.practice.core.audio.EndSessionAlert
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Foreground keep-alive. On end:
 * 1) Post a system ALARM notification (system plays sound + vibration) — primary.
 * 2) Reinforce with in-process MediaPlayer / Vibrator when possible — secondary.
 * 3) Long seats loop MediaPlayer until the user acknowledges.
 */
class MeditationKeepAliveService : Service() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun endSessionAlert(): EndSessionAlert
        fun bellSoundPlayer(): BellSoundPlayer
    }

    private fun deps(): Deps =
        EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                runCatching {
                    val d = deps()
                    d.bellSoundPlayer().stop()
                    d.endSessionAlert().stopVibration()
                }
                SessionEndNotifier.cancel(this)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_ALARM -> {
                val longSeat = intent.getBooleanExtra(EXTRA_LONG_SEAT, true)
                startAsForeground(alarm = true, longSeat = longSeat)
                // Primary: system notification sound + vibration.
                SessionEndNotifier.postEndAlert(this, longSeat = longSeat)
                // Secondary: in-process playback (works when FGS is alive).
                runCatching {
                    val d = deps()
                    if (longSeat) {
                        d.bellSoundPlayer().playLooping()
                        d.endSessionAlert().startMandatoryLoopVibration()
                    } else {
                        d.bellSoundPlayer().play(alarm = true)
                        d.endSessionAlert().vibrateEndPattern()
                    }
                }
                return START_STICKY
            }
            else -> {
                startAsForeground(alarm = false, longSeat = false)
                return START_STICKY
            }
        }
    }

    private fun startAsForeground(alarm: Boolean, longSeat: Boolean) {
        ensureQuietKeepAliveChannel()
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = when {
            alarm && longSeat -> "安般念已到时（请确认出定）"
            alarm -> "修习时间已到"
            else -> "安般念进行中"
        }
        val text = when {
            alarm && longSeat -> "请打开应用确认出定"
            alarm -> "计时结束"
            else -> "计时保护中"
        }
        // Keep-alive notification stays quiet; the end alert uses SessionEndNotifier.
        val notification: Notification = NotificationCompat.Builder(this, KEEP_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(if (alarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                KEEP_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(KEEP_NOTIFICATION_ID, notification)
        }
    }

    private fun ensureQuietKeepAliveChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(KEEP_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            KEEP_CHANNEL_ID,
            "修习进行中",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "长时/后台计时保活（静音）"
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val KEEP_CHANNEL_ID = "meditation_keep_alive_quiet"
        const val KEEP_NOTIFICATION_ID = 33001
        const val ACTION_START = "com.ruru.practice.meditation.KEEP_ALIVE_START"
        const val ACTION_ALARM = "com.ruru.practice.meditation.KEEP_ALIVE_ALARM"
        const val ACTION_STOP = "com.ruru.practice.meditation.KEEP_ALIVE_STOP"
        const val EXTRA_LONG_SEAT = "long_seat"

        fun start(context: Context) {
            val intent = Intent(context, MeditationKeepAliveService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun notifyEnd(context: Context, longSeat: Boolean) {
            // Always post system notification first (works even if FGS start is delayed).
            SessionEndNotifier.postEndAlert(context, longSeat)
            val intent = Intent(context, MeditationKeepAliveService::class.java)
                .setAction(ACTION_ALARM)
                .putExtra(EXTRA_LONG_SEAT, longSeat)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun notifyAlarm(context: Context) = notifyEnd(context, longSeat = true)

        fun stop(context: Context) {
            SessionEndNotifier.cancel(context)
            val intent = Intent(context, MeditationKeepAliveService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
