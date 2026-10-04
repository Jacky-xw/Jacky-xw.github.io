package com.ruru.practice.feature.meditation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ruru.practice.MainActivity
import com.ruru.practice.R

/**
 * Heads-up end notice. Sound is intentionally off by default: playback uses
 * the app foreground service on the ALARM stream so users can leave
 * notification volume low without losing the end bell.
 */
object SessionEndNotifier {
    const val CHANNEL_SOUND_ID = "session_end_alarm_sound_v4"
    const val CHANNEL_SILENT_ID = "session_end_alarm_silent_v4"
    const val NOTIFICATION_ID = 33010

    private val vibratePattern = longArrayOf(0, 450, 400, 450, 400, 700, 900)

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_SOUND_ID) == null) {
            val soundUri = bellUri(context)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SOUND_ID,
                    "修习到时闹钟",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "短时到时：系统播放引磬并震动"
                    setSound(soundUri, attrs)
                    enableVibration(true)
                    vibrationPattern = vibratePattern
                    setBypassDnd(true)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
        }
        if (manager.getNotificationChannel(CHANNEL_SILENT_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SILENT_ID,
                    "修习到时提示（无声）",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "长时到时：仅展示通知，声音由应用循环播放"
                    setSound(null, null)
                    enableVibration(false)
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
            )
        }
    }

    /**
     * @param withSound true = system plays bell + vibrate once (short seats).
     *                  false = silent heads-up only (long seats; service loops audio).
     */
    fun postEndAlert(context: Context, longSeat: Boolean, withSound: Boolean = !longSeat) {
        val app = context.applicationContext
        ensureChannels(app)

        val open = PendingIntent.getActivity(
            app, 0,
            Intent(app, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val fullScreen = PendingIntent.getActivity(
            app, 1,
            Intent(app, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (longSeat) "安般念已到时（请确认出定）" else "修习时间已到"
        val text = if (longSeat) "请打开应用确认出定；引磬将持续提醒" else "安般念/经行计时结束"
        val channelId = if (withSound) CHANNEL_SOUND_ID else CHANNEL_SILENT_ID

        val builder = NotificationCompat.Builder(app, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setFullScreenIntent(fullScreen, true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (withSound) {
            builder.setSound(bellUri(app))
            builder.setVibrate(vibratePattern)
        } else {
            builder.setSilent(true)
        }

        runCatching {
            NotificationManagerCompat.from(app).notify(NOTIFICATION_ID, builder.build())
        }
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context.applicationContext).cancel(NOTIFICATION_ID)
    }

    private fun bellUri(context: Context): Uri =
        Uri.parse("android.resource://${context.packageName}/${R.raw.soft_bell}")
}
