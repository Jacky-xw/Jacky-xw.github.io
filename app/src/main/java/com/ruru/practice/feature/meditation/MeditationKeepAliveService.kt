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
 * Foreground keep-alive + end-of-seat alert delivery.
 * All end bells/vibration are owned here so they work with the screen off
 * and the UI process frozen.
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
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_ALARM -> {
                val longSeat = intent.getBooleanExtra(EXTRA_LONG_SEAT, true)
                startAsForeground(alarm = true, longSeat = longSeat)
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
        ensureChannel()
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = when {
            alarm && longSeat -> "安般念已到时（请确认出定）"
            alarm -> "安般念已到时"
            else -> "安般念进行中"
        }
        val text = when {
            alarm && longSeat -> "引磬与震动将持续提醒，请打开应用点击确认停止"
            alarm -> "修习时间已到，请打开应用查看"
            else -> "计时保护中，请勿强制结束应用"
        }
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(!alarm)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(if (alarm) NotificationCompat.DEFAULT_ALL else 0)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        // Recreate with high importance so OEM builds don't keep a silent channel.
        runCatching { manager.deleteNotificationChannel(CHANNEL_ID) }
        val channel = NotificationChannel(
            CHANNEL_ID,
            "修习到时提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "安般念到时响铃与震动"
            enableVibration(true)
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "meditation_end_alert_v2"
        const val NOTIFICATION_ID = 33001
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
            val intent = Intent(context, MeditationKeepAliveService::class.java)
                .setAction(ACTION_ALARM)
                .putExtra(EXTRA_LONG_SEAT, longSeat)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** @deprecated use notifyEnd */
        fun notifyAlarm(context: Context) = notifyEnd(context, longSeat = true)

        fun stop(context: Context) {
            val intent = Intent(context, MeditationKeepAliveService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
