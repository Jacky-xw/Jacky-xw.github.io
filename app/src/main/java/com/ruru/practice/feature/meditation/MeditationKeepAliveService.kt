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
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import com.ruru.practice.MainActivity
import com.ruru.practice.R
import com.ruru.practice.core.audio.BellSoundPlayer
import com.ruru.practice.core.audio.EndSessionAlert
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * FGS + MediaSession notification (same pattern as music apps on lock screen).
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
    private var keepAlivePlayer: MediaPlayer? = null
    private var mediaSession: MediaSessionCompat? = null

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (endAtMillis <= 0L && !endFired) return
            if (!endFired) {
                val now = System.currentTimeMillis()
                if (now >= endAtMillis) {
                    fireEndAlert()
                } else {
                    updateMediaSessionMetadata()
                    publishNotification()
                    ensureKeepAliveAudio()
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
                    runCatching {
                        ensureMediaSession()
                        ensureKeepAliveAudio()
                        startAsForeground()
                    }
                    if (!endFired && endAtMillis > System.currentTimeMillis()) {
                        MeditationEndScheduler.schedule(
                            this@MeditationKeepAliveService, endAtMillis, longSeat
                        )
                        acquireSoftWakeLock()
                        handler.removeCallbacks(tickRunnable)
                        handler.post(tickRunnable)
                    } else if (!endFired && endAtMillis in 1..System.currentTimeMillis()) {
                        fireEndAlert()
                    }
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    runCatching { startAsForeground() }
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
        ensureMediaSession()
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
                stopKeepAliveAudio()
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
                    ensureMediaSession()
                    startAsForeground()
                    acquireSoftWakeLock()
                    ensureKeepAliveAudio()
                    MeditationEndScheduler.schedule(this, endAtMillis, longSeat = longSeat)
                    handler.post(tickRunnable)
                } else {
                    startAsForeground()
                }
                return START_STICKY
            }
            else -> {
                restoreFromPrefsIfNeeded()
                ensureMediaSession()
                startAsForeground()
                if (!endFired && endAtMillis > System.currentTimeMillis()) {
                    ensureKeepAliveAudio()
                    handler.removeCallbacks(tickRunnable)
                    handler.post(tickRunnable)
                }
                return START_STICKY
            }
        }
    }

    private fun restoreFromPrefsIfNeeded() {
        if (endAtMillis > 0L) return
        runCatching {
            val prefs = getSharedPreferences("ruru_meditation_timer", MODE_PRIVATE)
            if (!prefs.getBoolean("active", false)) return
            val phase = prefs.getString("phase", "") ?: ""
            if (phase != "RUNNING" && phase != "PAUSED") return
            val remaining = prefs.getInt("remaining_seconds", 0)
            val minutes = prefs.getInt("minutes", 0)
            if (remaining > 0) {
                endAtMillis = System.currentTimeMillis() + remaining * 1000L
                longSeat = minutes >= 60
                endFired = false
            }
        }
    }

    private fun fireEndAlert() {
        if (endFired) return
        endFired = true
        handler.removeCallbacks(tickRunnable)
        MeditationEndScheduler.cancel(this)
        stopKeepAliveAudio()
        acquireSoftWakeLock()
        updateMediaSessionMetadata()
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

    private fun ensureMediaSession() {
        if (mediaSession != null) return
        val session = MediaSessionCompat(this, "ruru_meditation_timer")
        session.setCallback(object : MediaSessionCompat.Callback() {})
        session.isActive = true
        mediaSession = session
        updateMediaSessionMetadata()
    }

    private fun updateMediaSessionMetadata() {
        val session = mediaSession ?: return
        val countingDown = !endFired && endAtMillis > System.currentTimeMillis()
        val remainSec = if (countingDown) {
            ((endAtMillis - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
        } else 0
        val mm = remainSec / 60
        val ss = remainSec % 60
        val title = when {
            endFired && longSeat -> "安般念已到时"
            endFired -> "修习时间已到"
            countingDown -> "安般念进行中"
            else -> "安般念进行中"
        }
        session.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, "一分钟禅修")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, if (countingDown) remainSec * 1000L else 0L)
                .build()
        )
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(
                    if (endFired) PlaybackStateCompat.STATE_STOPPED
                    else PlaybackStateCompat.STATE_PLAYING,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    1.0f
                )
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PAUSE)
                .build()
        )
    }

    private fun releaseMediaSession() {
        runCatching {
            mediaSession?.isActive = false
            mediaSession?.release()
        }
        mediaSession = null
    }

    private fun ensureKeepAliveAudio() {
        if (endFired) return
        val existing = keepAlivePlayer
        if (existing != null) {
            runCatching { if (!existing.isPlaying) existing.start() }
                .onFailure { stopKeepAliveAudio() }
            if (keepAlivePlayer != null) return
        }
        runCatching {
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            val afd = resources.openRawResourceFd(R.raw.soft_bell)
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            player.isLooping = true
            player.setVolume(0f, 0f)
            player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            player.prepare()
            player.start()
            keepAlivePlayer = player
        }
    }

    private fun stopKeepAliveAudio() {
        runCatching { keepAlivePlayer?.stop() }
        runCatching { keepAlivePlayer?.release() }
        keepAlivePlayer = null
    }

    private fun stopEverything() {
        handler.removeCallbacks(tickRunnable)
        endAtMillis = 0L
        endFired = false
        MeditationEndScheduler.cancel(this)
        stopKeepAliveAudio()
        releaseMediaSession()
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
        ensureMediaSession()
        updateMediaSessionMetadata()
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
        val remainSec = ((endAtMillis - System.currentTimeMillis()) / 1000L).toInt().coerceAtLeast(0)
        val mm = remainSec / 60
        val ss = remainSec % 60
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
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        mediaSession?.let { session ->
            builder.setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setMediaSession(session.sessionToken)
            )
        }

        if (countingDown) {
            // System chronometer (header) — the only countdown UI.
            builder
                .setWhen(endAtMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
        } else {
            builder.setUsesChronometer(false).setShowWhen(false)
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
        // Always recreate policy for this channel id if missing
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "修习计时",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "安般念计时（媒体样式，便于锁屏显示）"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!endFired && endAtMillis > System.currentTimeMillis()) {
            MeditationEndScheduler.schedule(this, endAtMillis, longSeat)
            runCatching {
                val restart = Intent(this, MeditationKeepAliveService::class.java)
                    .setAction(ACTION_SCHEDULE)
                    .putExtra(EXTRA_END_AT, endAtMillis)
                    .putExtra(EXTRA_LONG_SEAT, longSeat)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(restart)
                } else {
                    startService(restart)
                }
            }
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        handler.removeCallbacks(tickRunnable)
        stopKeepAliveAudio()
        releaseMediaSession()
        unregisterScreenReceiver()
        releaseSoftWakeLock()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "meditation_media_lock_v6"
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
