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
 * Market-style end alert: the *system* plays the channel sound and vibration
 * when the notification is posted. This works even when the app process was
 * frozen and does not depend on an in-process MediaPlayer.
 *
 * In-process MediaPlayer / Vibrator are optional secondary reinforcement.
 */
object SessionEndNotifier {
    const val CHANNEL_ID = "session_end_alarm_v3"
    const val NOTIFICATION_ID = 33010

    private val vibratePattern = longArrayOf(0, 450, 400, 450, 400, 700, 900)

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        // Sound/vibration on a channel are fixed at creation time.
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val soundUri = bellUri(context)
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val channel = NotificationChannel(
            CHANNEL_ID,
            "修习到时闹钟",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "安般念/经行到时由系统播放引磬并震动"
            setSound(soundUri, attrs)
            enableVibration(true)
            vibrationPattern = vibratePattern
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    fun postEndAlert(context: Context, longSeat: Boolean) {
        val app = context.applicationContext
        ensureChannel(app)

        val open = PendingIntent.getActivity(
            app,
            0,
            Intent(app, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // Full-screen intent helps on lock screen (same pattern as clock apps).
        val fullScreen = PendingIntent.getActivity(
            app,
            1,
            Intent(app, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (longSeat) "安般念已到时（请确认出定）" else "修习时间已到"
        val text = if (longSeat) {
            "请打开应用确认出定；引磬将持续提醒"
        } else {
            "安般念/经行计时结束"
        }

        val soundUri = bellUri(app)
        val builder = NotificationCompat.Builder(app, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setFullScreenIntent(fullScreen, true)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(soundUri)
            .setVibrate(vibratePattern)
            .setDefaults(0) // explicit sound + vibrate above

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
