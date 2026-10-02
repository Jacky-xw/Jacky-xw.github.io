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

/**
 * Foreground keep-alive for anapanasati seats of 60 minutes or longer.
 * Reduces the chance the process is killed while the timer is running or
 * while the mandatory end alarm is sounding.
 */
class MeditationKeepAliveService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_ALARM -> {
                startAsForeground(alarm = true)
                return START_STICKY
            }
            else -> {
                startAsForeground(alarm = false)
                return START_STICKY
            }
        }
    }

    private fun startAsForeground(alarm: Boolean) {
        ensureChannel()
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (alarm) "安般念已到时" else "安般念进行中")
            .setContentText(
                if (alarm) "请打开应用确认出定，引磬将持续提醒"
                else "长时座计时保护中，请勿强制结束应用"
            )
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setPriority(NotificationCompat.PRIORITY_LOW)
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
        val channel = NotificationChannel(
            CHANNEL_ID,
            "长时安般念",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "用于一小时及以上安般念的后台保活与出定提醒"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "meditation_keep_alive"
        const val NOTIFICATION_ID = 33001
        const val ACTION_START = "com.ruru.practice.meditation.KEEP_ALIVE_START"
        const val ACTION_ALARM = "com.ruru.practice.meditation.KEEP_ALIVE_ALARM"
        const val ACTION_STOP = "com.ruru.practice.meditation.KEEP_ALIVE_STOP"

        fun start(context: Context) {
            val intent = Intent(context, MeditationKeepAliveService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun notifyAlarm(context: Context) {
            val intent = Intent(context, MeditationKeepAliveService::class.java).setAction(ACTION_ALARM)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, MeditationKeepAliveService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
