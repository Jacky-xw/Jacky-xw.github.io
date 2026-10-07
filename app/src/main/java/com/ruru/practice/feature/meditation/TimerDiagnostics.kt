package com.ruru.practice.feature.meditation

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.usage.UsageStatsManager
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.ruru.practice.BuildConfig
import com.ruru.practice.core.util.DiagnosticStore
import com.ruru.practice.core.util.ExactAlarmPermission
import java.io.File
import java.io.OutputStream
import java.util.TimeZone
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Local, bounded diagnostics. No notes, custom audio names/paths or identifiers are collected. */
object TimerDiagnostics {
    private val writer = Executors.newSingleThreadExecutor { task -> Thread(task, "timer-log-writer") }
    private val pending = AtomicInteger()
    private val dropped = AtomicInteger()
    private fun prefs(context: Context) = context.getSharedPreferences("timer_diagnostics_v2", Context.MODE_PRIVATE)
    private fun store(context: Context) = DiagnosticStore(File(context.filesDir, "timer-diagnostics-v2"))
    private fun current(context: Context, walking: Boolean) =
        prefs(context).getString(if (walking) "walking" else "meditation", null) ?: "unassigned"

    /** New practice only; pause/resume and process recreation retain the same ID. */
    fun beginSession(context: Context, walking: Boolean, targetSeconds: Int) {
        runCatching {
            val app = context.applicationContext
            val kind = if (walking) "walking" else "meditation"
            val id = "${System.currentTimeMillis()}_${kind}_${UUID.randomUUID().toString().take(8)}"
            prefs(app).edit().putString(kind, id).putString("latest", id).apply()
            record(app, "session_begin kind=$kind targetSeconds=$targetSeconds", walking)
            capture(app, "session_begin", walking)
        }.onFailure { Log.e("PracticeTimer", "Cannot initialize diagnostics", it) }
    }

    fun record(context: Context, event: String, walking: Boolean = false, sessionId: String? = null) {
        // Diagnostics must never throw into the timer, and must not grow without bound if IO stalls.
        runCatching {
            val app = context.applicationContext
            val id = sessionId ?: current(app, walking)
            val line = "${System.currentTimeMillis()} elapsed=${SystemClock.elapsedRealtime()} uptime=${SystemClock.uptimeMillis()} thread=${Thread.currentThread().name} pid=${android.os.Process.myPid()} session=$id version=${BuildConfig.VERSION_NAME} ${event.take(6000)}\n"
            Log.i("PracticeTimer", line.trim())
            if (pending.incrementAndGet() > 2048) {
                pending.decrementAndGet()
                dropped.incrementAndGet()
                return
            }
            writer.execute {
                val lost = dropped.getAndSet(0)
                try {
                    val protected = setOf(current(app, false), current(app, true))
                    val notice = if (lost > 0) "DIAGNOSTIC_EVENTS_DROPPED count=$lost; queue or storage failure\n" else ""
                    store(app).append(id, notice + line, protected)
                } catch (error: Exception) {
                    dropped.addAndGet(lost + 1)
                    Log.e("PracticeTimer", "Diagnostic write failed", error)
                } finally { pending.decrementAndGet() }
            }
        }.onFailure { Log.e("PracticeTimer", "Diagnostic enqueue failed", it) }
    }

    fun failure(context: Context, operation: String, error: Throwable, walking: Boolean = false) {
        // Omit exception messages: document-provider exceptions may contain private audio paths.
        val chain = generateSequence(error) { it.cause }.take(3).joinToString(" causedBy ") {
            "${it.javaClass.name}: ${it.stackTrace.take(12).joinToString(" <- ")}"
        }
        record(context, "failure operation=$operation $chain", walking)
    }

    fun deviceState(context: Context): String = runCatching {
        val pm = context.getSystemService(PowerManager::class.java)
        val am = context.getSystemService(ActivityManager::class.java)
        val alarm = context.getSystemService(AlarmManager::class.java)
        val bucket = if (Build.VERSION.SDK_INT >= 28) context.getSystemService(UsageStatsManager::class.java)?.appStandbyBucket else -1
        val restricted = if (Build.VERSION.SDK_INT >= 28) am?.isBackgroundRestricted else false
        "interactive=${pm?.isInteractive} idle=${pm?.isDeviceIdleMode} powerSave=${pm?.isPowerSaveMode} restricted=$restricted standbyBucket=$bucket nextSystemAlarm=${alarm?.nextAlarmClock?.triggerTime}"
    }.getOrElse { "deviceStateUnavailable=${it.javaClass.simpleName}" }

    private fun environment(context: Context): String = buildString {
        appendLine("一分钟禅修 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT}, display=${Build.DISPLAY}")
        appendLine("timezone=${TimeZone.getDefault().id} bootCount=${Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, -1)}")
        appendLine(deviceState(context))
        val pm = context.getSystemService(PowerManager::class.java)
        appendLine("exactAlarm=${ExactAlarmPermission.canSchedule(context)} batteryExempt=${pm?.isIgnoringBatteryOptimizations(context.packageName)} notifications=${NotificationManagerCompat.from(context).areNotificationsEnabled()}")
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        appendLine("batteryLevel=${battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)} batteryScale=${battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1)} plugged=${battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)} temperatureTenthsC=${battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)}")
        val audio = context.getSystemService(AudioManager::class.java)
        appendLine("alarmVolume=${audio?.getStreamVolume(AudioManager.STREAM_ALARM)}/${audio?.getStreamMaxVolume(AudioManager.STREAM_ALARM)} mediaVolume=${audio?.getStreamVolume(AudioManager.STREAM_MUSIC)} ringerMode=${audio?.ringerMode}")
        val notifications = context.getSystemService(NotificationManager::class.java)
        appendLine("interruptionFilter=${notifications?.currentInterruptionFilter} channelImportance=${notifications?.getNotificationChannel(MeditationKeepAliveService.CHANNEL_ID)?.importance}")
        for (walking in listOf(false, true)) {
            val timer = context.getSharedPreferences(if (walking) "ruru_walking_timer" else "ruru_meditation_timer", Context.MODE_PRIVATE)
            appendLine("walking=$walking phase=${timer.getString("phase", "")} active=${timer.getBoolean("active", false)} startedAt=${timer.getLong("started_at", 0)} elapsed=${timer.getInt("elapsed", 0)} endAlarmActive=${timer.getBoolean("end_alarm_active", false)}")
        }
        appendLine("nextSystemAlarm is device-wide; not proof of ownership. OEM autostart/high-background-power/unused-app settings: UNKNOWN (must be confirmed manually).")
    }

    private val stateReader = Executors.newSingleThreadExecutor { task -> Thread(task, "timer-state-reader") }
    private val statePending = AtomicInteger()
    fun capture(context: Context, reason: String, walking: Boolean = false) {
        val app = context.applicationContext
        val id = current(app, walking)
        val requestedAt = System.currentTimeMillis()
        if (statePending.incrementAndGet() > 4) { statePending.decrementAndGet(); record(app, "snapshot_skipped reason=$reason", walking); return }
        stateReader.execute {
            try {
                record(app, "environment requestedAt=$requestedAt capturedAt=${System.currentTimeMillis()} reason=$reason\n${environment(app)}", walking, id)
            } catch (error: Exception) { failure(app, "environment:$reason", error, walking) }
            finally { statePending.decrementAndGet() }
        }
    }

    /** Call on IO. Export-time snapshot is labeled separately from historical events. */
    private fun reportHeader(context: Context): String = buildString {
        appendLine("Export-time snapshot (not the state at failure):")
        appendLine(runCatching { environment(context) }.getOrElse { "snapshot failed: ${it.javaClass.name}" })
        appendLine("pendingEvents=${pending.get()} droppedNotYetReported=${dropped.get()}")
        if (Build.VERSION.SDK_INT >= 30) {
            runCatching {
                context.getSystemService(ActivityManager::class.java)
                    ?.getHistoricalProcessExitReasons(context.packageName, 0, 8)?.forEach {
                        appendLine("historicalExit time=${it.timestamp} pid=${it.pid} reason=${it.reason} status=${it.status} importance=${it.importance} pssKb=${it.pss} rssKb=${it.rss}")
                    }
            }.onFailure { appendLine("historicalExitUnavailable=${it.javaClass.name}") }
        }
        appendLine("A frozen/killed app cannot record while not executing. Missing callbacks do not prove OEM cause. Audio started/vibration requested does not prove physical delivery.")
    }

    fun snapshot(context: Context): String {
        val header = reportHeader(context)
        return header + writer.submit<String> {
            val id = prefs(context).getString("latest", "unassigned") ?: "unassigned"
            "\nLatest session=$id (tail only; export ZIP for retained full logs)\n" + store(context).recent(id)
        }.get(15, TimeUnit.SECONDS)
    }

    /** Serializes log files on their writer thread; copies ZIP to user destination on caller IO. */
    fun export(context: Context, destination: OutputStream) {
        val header = reportHeader(context)
        val file = File.createTempFile("timer-diagnostics-", ".zip", context.cacheDir)
        val task = writer.submit {
            try {
                ZipOutputStream(file.outputStream().buffered()).use { zip ->
                    fun text(name: String, value: String) {
                        zip.putNextEntry(ZipEntry(name)); zip.write(value.toByteArray(Charsets.UTF_8)); zip.closeEntry()
                    }
                    text("report.txt", header)
                    text("README.txt", "Contains local technical diagnostics only. No notes or audio files. Recent 10 inactive sessions plus two latest protected timer sessions; at most 2 x 512KiB events per session. TRUNCATED.txt means older events were removed. Legacy logs are included separately and may contain older diagnostic message text. Do not post publicly without reviewing. This is not system logcat/dumpsys.\n")
                    for (session in store(context).sessions()) {
                        session.listFiles()?.filter { it.isFile }?.sortedBy { it.name }?.forEach {
                            zip.putNextEntry(ZipEntry("sessions/${session.name}/${it.name}"))
                            it.inputStream().use { input -> input.copyTo(zip) }; zip.closeEntry()
                        }
                    }
                    for (name in listOf("timer-diagnostics.previous.log", "timer-diagnostics.log")) {
                        val legacy = File(context.filesDir, name)
                        if (legacy.isFile) {
                            zip.putNextEntry(ZipEntry("legacy/$name"))
                            legacy.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
                        }
                    }
                }
            } catch (error: Exception) { file.delete(); throw error }
        }
        try {
            task.get() // No UI blocking; completes the queued writes before copying.
            file.inputStream().use { it.copyTo(destination) }
        } finally { file.delete() }
    }
}

/** Bridge CPU ownership across receiver return and asynchronous service dispatch. */
object TimerAlarmHandoff {
    private var lock: PowerManager.WakeLock? = null
    @Synchronized fun acquire(context: Context) {
        if (lock?.isHeld == true) return
        lock = context.getSystemService(PowerManager::class.java)
            ?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ruru:alarm_handoff")
            ?.apply { setReferenceCounted(false); acquire(60_000L) }
    }
    @Synchronized fun release() {
        runCatching { if (lock?.isHeld == true) lock?.release() }
        lock = null
    }
}
