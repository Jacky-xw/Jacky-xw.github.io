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
 * Foreground countdown + end alert.
 *
 * Handler ticks are best-effort while the CPU is awake. Lock-screen / Doze
 * delivery relies on [MeditationEndScheduler] (AlarmClock) which is re-armed
 * from this service on every schedule and when the screen turns off.
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
            if (endFired || endAtMillis <= 0L) return
            val now = System.currentTimeMillis()
            if (now >= endAtMillis) {
                fireEndAlert()
            } else {
                val remain = endAtMillis - now
                val delay = when {
                    remain > 60_000L -> 15_000L
                    remain > 5_000L -> 1_000L
                    else -> 250L
                }
                // elapsedRealtime delays are more stable across deep sleep resumes.
                handler.postAtTime(this, SystemClock.elapsedRealtime() + delay)
                updateKeepAliveNotification(alarm = false)
            }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Screen off: Handler may stall; ensure AlarmClock is armed.
                    if (!endFired && endAtMillis > System.currentTimeMillis()) {
                        MeditationEndScheduler.schedule(this@MeditationKeepAliveService, endAtMillis)
                        acquireSoftWakeLock()
                    } else if (!endFired && endAtMillis > 0L &&
                        endAtMillis <= System.currentTimeMillis()
                    ) {
                        fireEndAlert()
                    }
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    if (!endFired && endAtMillis > 0L) {
                        if (System.currentTimeMillis() >= endAtMillis) {
                            fireEndAlert()
                        } else {
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
            ACTION_STOP -> {
                stopEverything()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_ALARM -> {
                if (intent.hasExtra(EXTRA_LONG_SEAT)) {
                    longSeat = intent.getBooleanExtra(EXTRA_LONG_SEAT, longSeat)
                } else if (endAtMillis <= 0L) {
                    // AlarmClock path may not pass extras; read from timer prefs.
                    val minutes = getSharedPreferences("ruru_meditation_timer", MODE_PRIVATE)
                        .getInt("minutes", 0)
                    longSeat = minutes >= 60
                }
                startAsForeground(alarm = true)
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
                    startAsForeground(alarm = false)
                    acquireSoftWakeLock()
                    // Critical for lock-screen: system AlarmClock wakes the device.
                    MeditationEndScheduler.schedule(this, endAtMillis)
                    handler.post(tickRunnable)
                } else {
                    startAsForeground(alarm = false)
                }
                return START_STICKY
            }
            else -> {
                startAsForeground(alarm = false)
                return START_STICKY
            }
        }
    }

    private fun fireEndAlert() {
        if (endFired) return
        endFired = true
        handler.removeCallbacks(tickRunnable)
        MeditationEndScheduler.cancel(this)
        startAsForeground(alarm = true)
        acquireSoftWakeLock()

        SessionEndNotifier.postEndAlert(this, longSeat = longSeat, withSound = false)
        // Prefer lock-screen activity (system clock pattern). It plays on ALARM
        // stream while the screen is off / device is locked.
        runCatching { SessionEndActivity.launch(this, longSeat = longSeat) }
            .onFailure {
                // Fallback if activity cannot start: in-process playback.
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
        SessionEndNotifier.cancel(this)
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

    private fun startAsForeground(alarm: Boolean) {
        ensureQuietKeepAliveChannel()
        val notification = buildKeepNotification(alarm)
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

    private fun updateKeepAliveNotification(alarm: Boolean) {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(KEEP_NOTIFICATION_ID, buildKeepNotification(alarm))
    }

    private fun buildKeepNotification(alarm: Boolean): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val remainSec = ((endAtMillis - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
        val remainText = if (!alarm && endAtMillis > 0L) {
            val m = remainSec / 60
            val s = remainSec % 60
            "剩余约 ${m}分${s}秒"
        } else if (alarm && longSeat) {
            "已到时，请打开应用确认出定"
        } else if (alarm) {
            "计时结束"
        } else {
            "计时保护中"
        }
        val title = when {
            alarm && longSeat -> "安般念已到时"
            alarm -> "修习时间已到"
            else -> "安般念进行中"
        }
        return NotificationCompat.Builder(this, KEEP_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(remainText)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(if (alarm) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
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
            description = "计时保活通知（静音）"
            setSound(null, null)
            enableVibration(false)
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
        const val KEEP_CHANNEL_ID = "meditation_keep_alive_quiet"
        const val KEEP_NOTIFICATION_ID = 33001
        const val ACTION_START = "com.ruru.practice.meditation.KEEP_ALIVE_START"
        const val ACTION_SCHEDULE = "com.ruru.practice.meditation.KEEP_ALIVE_SCHEDULE"
        const val ACTION_ALARM = "com.ruru.practice.meditation.KEEP_ALIVE_ALARM"
        const val ACTION_STOP = "com.ruru.practice.meditation.KEEP_ALIVE_STOP"
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
            SessionEndNotifier.cancel(context)
            val intent = Intent(context, MeditationKeepAliveService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
