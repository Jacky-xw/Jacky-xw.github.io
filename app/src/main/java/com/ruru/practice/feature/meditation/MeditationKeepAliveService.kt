package com.ruru.practice.feature.meditation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
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
 * Single persistent FGS notification.
 * Countdown uses the **system chronometer** (shows beside the app name).
 * Do not put a second “剩余 MM:SS” in the body — app-driven text freezes in background.
 */
class MeditationKeepAliveService : Service() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Deps {
        fun endSessionAlert(): EndSessionAlert
        fun bellSoundPlayer(): BellSoundPlayer
    }

    private val handler = Handler(Looper.getMainLooper())
    private var endAtMillis: Long = 0L
    private var longSeat: Boolean = false
    private var endFired: Boolean = false
    private var wakeLock: PowerManager.WakeLock? = null
    private var screenReceiverRegistered = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (endAtMillis <= 0L && !endFired) return
            if (!endFired) {
                val now = System.currentTimeMillis()
                if (now >= endAtMillis) {
                    fireEndAlert()
                } else {
                    // System chronometer ticks by itself; only poll for end time.
                    val remain = endAtMillis - now
                    val delay = when {
                        remain > 60_000L -> 20_000L
                        remain > 15_000L -> 5_000L
                        else -> 1_000L
                    }
                    handler.postAtTime(this, SystemClock.elapsedRealtime() + delay)
                }
            } else if (longSeat) {
                publishNotification()
                runCatching { deps().endSessionAlert().startMandatoryLoopVibration() }
                handler.postAtTime(this, SystemClock.elapsedRealtime() + 8_000L)
            }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Re-assert FGS notification so lock-screen shade keeps it.
                    runCatching { startAsForeground() }
                    if (!endFired && endAtMillis > System.currentTimeMillis()) {
                        MeditationEndScheduler.schedule(
                            this@MeditationKeepAliveService, endAtMillis, longSeat
                        )
                        acquireSoftWakeLock()
                    } else if (!endFired && endAtMillis in 1..System.currentTimeMillis()) {
                        fireEndAlert()
                    }
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    if (!endFired && endAtMillis > 0L) {
                        if (System.currentTimeMillis() >= endAtMillis) fireEndAlert()
                        else {
                            handler.removeCallbacks(tickRunnable)
                            handler.post(tickRunnable)
                        }
                    }
                }
            }
        }
    }

    private fun deps(): Deps =
        EntryPointAccessors.fromApplication(applicationContext, Deps::class.java)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        registerScreenReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP, ACTION_ACKNOWLEDGE -> {
                stopEverything()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_REPOST -> {
                if (endAtMillis > 0L || endFired) {
                    startAsForeground()
                    if (!endFired) {
                        handler.removeCallbacks(tickRunnable)
                        handler.post(tickRunnable)
                    }
                }
                return START_STICKY
            }
            ACTION_ALARM -> {
                if (intent.hasExtra(EXTRA_LONG_SEAT)) {
                    longSeat = intent.getBooleanExtra(EXTRA_LONG_SEAT, longSeat)
                }
                startAsForeground()
                fireEndAlert()
                return START_STICKY
            }
            ACTION_SCHEDULE, ACTION_START -> {
                val endAt = intent.getLongExtra(EXTRA_END_AT, 0L)
                val long = intent.getBooleanExtra(EXTRA_LONG_SEAT, false)
                if (endAt > 0L) {
                    endAtMillis = endAt
                    longSeat = long
                    endFired = false
                    handler.removeCallbacks(tickRunnable)
                    startAsForeground()
                    acquireSoftWakeLock()
                    MeditationEndScheduler.schedule(this, endAtMillis, longSeat = longSeat)
                    handler.post(tickRunnable)
                } else {
                    startAsForeground()
                }
                return START_STICKY
            }
            else -> {
                startAsForeground()
                return START_STICKY
            }
        }
    }

    private fun fireEndAlert() {
        if (endFired) return
        endFired = true
        handler.removeCallbacks(tickRunnable)
        MeditationEndScheduler.cancel(this)
        acquireSoftWakeLock()
        startAsForeground()

        runCatching {
            val d = deps()
            if (longSeat) {
                d.bellSoundPlayer().playLooping()
                d.endSessionAlert().startMandatoryLoopVibration()
                handler.post(tickRunnable)
            } else {
                d.bellSoundPlayer().play(alarm = true)
                d.endSessionAlert().vibrateEndPattern()
            }
        }
        publishNotification()
        if (longSeat) {
            runCatching {
                @Suppress("DEPRECATION")
                val pm = getSystemService(PowerManager::class.java)
                val wl = pm?.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "ruru:end_screen"
                )
                wl?.setReferenceCounted(false)
                wl?.acquire(3_000L)
            }
        }
        MeditationTimerBridge.publishNaturalEndDue()
        runCatching {
            val prefs = getSharedPreferences("ruru_meditation_timer", MODE_PRIVATE)
            if (prefs.getBoolean("active", false)) {
                prefs.edit()
                    .putString("phase", "COMPLETED")
                    .putBoolean("end_alarm_active", longSeat)
                    .apply()
            }
        }
    }

    private fun stopEverything() {
        handler.removeCallbacks(tickRunnable)
        endAtMillis = 0L
        endFired = false
        MeditationEndScheduler.cancel(this)
        runCatching {
            val d = deps()
            d.bellSoundPlayer().stop()
            d.endSessionAlert().stopVibration()
        }
        releaseSoftWakeLock()
        unregisterScreenReceiver()
    }

    private fun registerScreenReceiver() {
        if (screenReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(screenReceiver, filter)
        }
        screenReceiverRegistered = true
    }

    private fun unregisterScreenReceiver() {
        if (!screenReceiverRegistered) return
        runCatching { unregisterReceiver(screenReceiver) }
        screenReceiverRegistered = false
    }

    private fun acquireSoftWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(PowerManager::class.java) ?: return
        @Suppress("DEPRECATION")
        val lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ruru:meditation_ticker")
        lock.setReferenceCounted(false)
        lock.acquire(3 * 60 * 60 * 1000L + 30 * 60 * 1000L)
        wakeLock = lock
    }

    private fun releaseSoftWakeLock() {
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
    }

    private fun startAsForeground() {
        ensureChannel()
        val notification = buildNotification()
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

    private fun publishNotification() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val deletePi = PendingIntent.getService(
            this, 3,
            Intent(this, MeditationKeepAliveService::class.java).setAction(ACTION_REPOST),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val countingDown = !endFired && endAtMillis > System.currentTimeMillis()
        val (title, text) = when {
            endFired && longSeat -> "安般念已到时" to "请确认出定；引磬与震动持续中"
            endFired -> "修习时间已到" to "计时结束"
            countingDown -> "安般念进行中" to "点击返回修习"
            else -> "安般念进行中" to "计时保护中"
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setDeleteIntent(deletePi)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(
                if (endFired) NotificationCompat.CATEGORY_ALARM
                else NotificationCompat.CATEGORY_SERVICE
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (countingDown) {
            // System chronometer beside app name — ticks without app notify.
            builder
                .setWhen(endAtMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        } else {
            builder
                .setUsesChronometer(false)
                .setShowWhen(false)
        }

        if (endFired && longSeat) {
            val ack = PendingIntent.getService(
                this, 2,
                Intent(this, MeditationKeepAliveService::class.java).setAction(ACTION_ACKNOWLEDGE),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(0, "确认出定，停止提醒", ack)
        }

        return builder.build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "修习计时（锁屏可见）",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "安般念计时常驻通知，锁屏上保持显示"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setBypassDnd(false)
        }
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tickRunnable)
        unregisterScreenReceiver()
        releaseSoftWakeLock()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "meditation_session_lock_v4"
        const val NOTIFICATION_ID = 33001
        const val ACTION_START = "com.ruru.practice.meditation.KEEP_ALIVE_START"
        const val ACTION_SCHEDULE = "com.ruru.practice.meditation.KEEP_ALIVE_SCHEDULE"
        const val ACTION_ALARM = "com.ruru.practice.meditation.KEEP_ALIVE_ALARM"
        const val ACTION_STOP = "com.ruru.practice.meditation.KEEP_ALIVE_STOP"
        const val ACTION_ACKNOWLEDGE = "com.ruru.practice.meditation.KEEP_ALIVE_ACK"
        const val ACTION_REPOST = "com.ruru.practice.meditation.KEEP_ALIVE_REPOST"
        const val EXTRA_LONG_SEAT = "long_seat"
        const val EXTRA_END_AT = "end_at"

        fun start(context: Context) {
            val intent = Intent(context, MeditationKeepAliveService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun schedule(context: Context, endAtMillis: Long, longSeat: Boolean) {
            val intent = Intent(context, MeditationKeepAliveService::class.java)
                .setAction(ACTION_SCHEDULE)
                .putExtra(EXTRA_END_AT, endAtMillis)
                .putExtra(EXTRA_LONG_SEAT, longSeat)
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

        fun notifyAlarm(context: Context) = notifyEnd(context, longSeat = true)

        fun stop(context: Context) {
            val intent = Intent(context, MeditationKeepAliveService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
