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
import com.ruru.practice.core.util.TimerDeadline
import com.ruru.practice.feature.practice.WalkingEndScheduler
import com.ruru.practice.feature.practice.WalkingTimerBridge
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import com.ruru.practice.MainActivity
import com.ruru.practice.R
import com.ruru.practice.core.audio.BellSoundPlayer
import com.ruru.practice.core.audio.EndSessionAlert

/**
 * FGS + MediaSession notification (same pattern as music apps on lock screen).
 */
open class MeditationKeepAliveService : Service() {
    protected open val walking: Boolean = false
    private val notificationId get() = if (walking) 33002 else NOTIFICATION_ID
    private fun serviceIntent() = Intent(this, javaClass)
    private fun cancelEnd() { if (walking) WalkingEndScheduler.cancel(this) else MeditationEndScheduler.cancel(this) }
    private fun scheduleEnd() { if (walking) WalkingEndScheduler.schedule(this, endAtMillis) else MeditationEndScheduler.schedule(this, endAtMillis, longSeat) }

    private val handler = Handler(Looper.getMainLooper())
    private var lastHeartbeat = 0L
    private fun trace(event: String) = TimerDiagnostics.record(this, "walking=$walking $event")
    private fun attempt(name: String, action: () -> Unit) {
        runCatching(action).onFailure { trace("FAILED $name ${it.javaClass.simpleName}: ${it.message}") }
    }
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
                if (now - lastHeartbeat >= 60_000L) {
                    lastHeartbeat = now
                    trace("heartbeat remainingMs=${endAtMillis - now} wakeLock=${wakeLock?.isHeld}")
                }
                if (now >= endAtMillis) {
                    fireEndAlert()
                } else {
                    attempt("tick_notification") { publishNotification() }
                    ensureKeepAliveAudio()
                    val remain = endAtMillis - now
                    // Handler delays use uptime; AlarmManager remains the wakeup backup.
                    handler.postDelayed(this, TimerDeadline.nextCheckDelay(remain))
                }
            } else if (longSeat) {
                attempt("end_notification") { publishNotification() }
                runCatching { alert.startMandatoryLoopVibration() }
                handler.postDelayed(this, 8_000L)
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
                        scheduleEnd()
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

    // Each timer owns its playback; previews and the other timer cannot stop its alarm.
    private val bell by lazy { BellSoundPlayer(applicationContext) }
    private val alert by lazy { EndSessionAlert(applicationContext) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        trace("service_created")
        attempt("register_screen_receiver") { registerScreenReceiver() }
        attempt("create_media_session") { ensureMediaSession() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        trace("service_command action=${intent?.action} deadline=${intent?.getLongExtra(EXTRA_END_AT, 0L)}")
        when (intent?.action) {
            ACTION_STOP, ACTION_ACKNOWLEDGE -> {
                if (intent?.action == ACTION_ACKNOWLEDGE) {
                    getSharedPreferences("ruru_meditation_timer", MODE_PRIVATE).edit()
                        .putBoolean("end_alarm_active", false).apply()
                    MeditationTimerBridge.publishAcknowledged()
                }
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
                val alarmDeadline = intent.getLongExtra(EXTRA_END_AT, 0L)
                if (alarmDeadline > 0L) {
                    val prefs = getSharedPreferences(MeditationEndScheduler.PREFS, MODE_PRIVATE)
                    val expected = prefs.getLong(MeditationEndScheduler.KEY_END_AT, 0L)
                    if (!TimerDeadline.matchesAlarm(alarmDeadline, expected)) {
                        trace("service_alarm_ignored_stale expected=$expected")
                        TimerAlarmHandoff.release()
                        startAsForeground()
                        if (endAtMillis == 0L && !endFired) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf()
                            return START_NOT_STICKY
                        }
                        return START_STICKY
                    }
                    endAtMillis = alarmDeadline
                }
                if (intent.hasExtra(EXTRA_LONG_SEAT)) {
                    longSeat = intent.getBooleanExtra(EXTRA_LONG_SEAT, longSeat)
                }
                try {
                    acquireSoftWakeLock()
                    fireEndAlert()
                } finally {
                    if (!walking) TimerAlarmHandoff.release()
                }
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
                    scheduleEnd()
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
                if (!endFired && endAtMillis > 0L) {
                    acquireSoftWakeLock()
                    if (endAtMillis > System.currentTimeMillis()) {
                        ensureKeepAliveAudio()
                        scheduleEnd()
                    }
                    handler.removeCallbacks(tickRunnable)
                    handler.post(tickRunnable)
                }
                return START_STICKY
            }
        }
    }

    private fun restoreFromPrefsIfNeeded() {
        if (endAtMillis > 0L) return
        val prefs = getSharedPreferences(if (walking) "ruru_walking_timer" else "ruru_meditation_timer", MODE_PRIVATE)
        if (!prefs.getBoolean("active", false)) return
        longSeat = !walking && prefs.getInt("minutes", 0) >= 60
        if (prefs.getBoolean("end_alarm_active", false) && longSeat) {
            endAtMillis = System.currentTimeMillis()
            return
        }
        if (prefs.getString("phase", "") != "RUNNING") return
        val started = prefs.getLong("started_at", 0L)
        if (started <= 0L) return
        val total = if (walking) prefs.getInt("target", 3600) else prefs.getInt("minutes", 20) * 60
        val base = prefs.getInt(if (walking) "elapsed" else "paused_elapsed", 0)
        endAtMillis = TimerDeadline.restore(started, total, base)
    }

    private fun fireEndAlert() {
        if (endFired) { trace("end_duplicate_ignored"); return }
        trace("end_begin deadline=$endAtMillis long=$longSeat lateMs=${System.currentTimeMillis() - endAtMillis}")
        endFired = true
        handler.removeCallbacks(tickRunnable)
        attempt("cancel_end") { cancelEnd() }
        stopKeepAliveAudio()
        attempt("end_wake_lock") { acquireSoftWakeLock() }
        // Media metadata or notification failure must not prevent sound and vibration.
        attempt("end_foreground") { startAsForeground() }

        runCatching { check(if (longSeat) bell.playLooping() else bell.play(alarm = true)) { "No playable end sound" } }
            .onSuccess { trace("end_audio_started") }
            .onFailure { trace("END_AUDIO_FAILED ${it.javaClass.simpleName}: ${it.message}") }
        runCatching { if (longSeat) alert.startMandatoryLoopVibration() else alert.vibrateEndPattern() }
            .onSuccess { trace("end_vibration_requested") }
            .onFailure { trace("END_VIBRATION_FAILED ${it.javaClass.simpleName}: ${it.message}") }
        if (longSeat) handler.post(tickRunnable)
        attempt("end_notification") { publishNotification() }
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
        runCatching {
            val prefs = getSharedPreferences(if (walking) "ruru_walking_timer" else "ruru_meditation_timer", MODE_PRIVATE)
            if (prefs.getBoolean("active", false)) {
                prefs.edit()
                    .putString("phase", if (walking) "FINISHED" else "COMPLETED")
                    .putInt("elapsed", if (walking) prefs.getInt("target", 3600) else prefs.getInt("minutes", 20) * 60)
                    .putBoolean("end_alarm_active", longSeat)
                    .apply()
            }
        }
        if (walking) WalkingTimerBridge.publishNaturalEndDue() else MeditationTimerBridge.publishNaturalEndDue()
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
        val title = when {
            endFired && longSeat -> "安般念已到时"
            endFired -> "修习时间已到"
            countingDown -> if (walking) "经行进行中" else "安般念进行中"
            else -> if (walking) "经行进行中" else "安般念进行中"
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
        cancelEnd()
        stopKeepAliveAudio()
        releaseMediaSession()
        runCatching {
            bell.stop()
            alert.stopVibration()
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
        lock.acquire((endAtMillis - System.currentTimeMillis()).coerceAtLeast(0L) + 30 * 60 * 1000L)
        wakeLock = lock
    }

    private fun releaseSoftWakeLock() {
        runCatching { if (wakeLock?.isHeld == true) wakeLock?.release() }
        wakeLock = null
    }

    private fun startAsForeground() {
        ensureChannel()
        attempt("media_metadata") {
            ensureMediaSession()
            updateMediaSessionMetadata()
        }
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                notificationId, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun publishNotification() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.notify(notificationId, buildNotification())
    }

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val deletePi = PendingIntent.getService(
            this, 3,
            serviceIntent().setAction(ACTION_REPOST),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val countingDown = !endFired && endAtMillis > System.currentTimeMillis()
        val title = when {
            endFired && longSeat -> "安般念已到时"
            endFired -> "修习时间已到"
            else -> if (walking) "经行进行中" else "安般念进行中"
        }
        // Only set a subtitle when ended — empty ContentText still draws a blank row on many OEMs.
        val subtitle: String? = when {
            endFired && longSeat -> "请确认出定；引磬与震动持续中"
            endFired -> "计时结束"
            else -> null
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentIntent(open)
            .setDeleteIntent(deletePi)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        // Required on many OEMs for lock-screen presence (music-style card).
        mediaSession?.let { session ->
            builder.setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setMediaSession(session.sessionToken)
            )
        }

        if (subtitle != null) {
            builder.setContentText(subtitle)
        }

        if (countingDown) {
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
                serviceIntent().setAction(ACTION_ACKNOWLEDGE),
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
            scheduleEnd()
            runCatching {
                val restart = serviceIntent()
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
        bell.stop()
        alert.stopVibration()
        releaseMediaSession()
        unregisterScreenReceiver()
        releaseSoftWakeLock()
        trace("service_destroyed endFired=$endFired deadline=$endAtMillis")
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "meditation_media_lock_v7"
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

        fun schedule(context: Context, endAtMillis: Long, longSeat: Boolean, walking: Boolean = false) {
            val intent = Intent(context, if (walking) com.ruru.practice.feature.practice.WalkingKeepAliveService::class.java else MeditationKeepAliveService::class.java)
                .setAction(ACTION_SCHEDULE)
                .putExtra(EXTRA_END_AT, endAtMillis)
                .putExtra(EXTRA_LONG_SEAT, longSeat)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun notifyEnd(context: Context, longSeat: Boolean, walking: Boolean = false, alarmDeadline: Long = 0L) {
            val intent = Intent(context, if (walking) com.ruru.practice.feature.practice.WalkingKeepAliveService::class.java else MeditationKeepAliveService::class.java)
                .setAction(ACTION_ALARM)
                .putExtra(EXTRA_LONG_SEAT, longSeat)
                .putExtra(EXTRA_END_AT, alarmDeadline)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun notifyAlarm(context: Context) = notifyEnd(context, longSeat = true)

        fun stop(context: Context, walking: Boolean = false) {
            val intent = Intent(context, if (walking) com.ruru.practice.feature.practice.WalkingKeepAliveService::class.java else MeditationKeepAliveService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }
    }
}
