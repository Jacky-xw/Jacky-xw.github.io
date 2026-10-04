package com.ruru.practice.feature.meditation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ruru.practice.R

/**
 * Full-screen / heads-up end notice. On modern Android this is the supported
 * way to surface UI over the lock screen (background startActivity is blocked).
 */
object SessionEndNotifier {
    const val CHANNEL_ID = "session_end_fullscreen_v5"
    const val NOTIFICATION_ID = 33010
    private val vibratePattern = longArrayOf(0, 450, 400, 450, 400, 700, 900)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "修习到时提醒",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "到时全屏提醒（声音由前台服务用闹钟音量播放）"
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = vibratePattern
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun postEndAlert(context: Context, longSeat: Boolean, withSound: Boolean = false) {
        val app = context.applicationContext
        ensureChannel(app)

        val open = PendingIntent.getActivity(
            app, 0,
            Intent(app, SessionEndActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(SessionEndActivity.EXTRA_LONG_SEAT, longSeat),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (longSeat) "安般念已到时（请确认出定）" else "修习时间已到"
        val text = if (longSeat) "请确认出定；引磬持续提醒中" else "计时结束"

        val builder = NotificationCompat.Builder(app, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(vibratePattern)

        if (withSound) {
            val uri = Uri.parse("android.resource://${app.packageName}/${R.raw.soft_bell}")
            // NotificationCompat.Builder.setSound expects stream type Int, not AudioAttributes.
            builder.setSound(uri, android.media.AudioManager.STREAM_ALARM)
        } else {
            builder.setSilent(true)
        }

        runCatching { NotificationManagerCompat.from(app).notify(NOTIFICATION_ID, builder.build()) }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context.applicationContext).cancel(NOTIFICATION_ID)
    }
}
