package com.ruru.practice.feature.meditation

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import com.ruru.practice.BuildConfig
import com.ruru.practice.core.util.ExactAlarmPermission
import java.io.File
import java.util.concurrent.Executors

/** Local technical events only: no notes, audio paths or practice content. */
object TimerDiagnostics {
    private val writer = Executors.newSingleThreadExecutor()
    fun record(context: Context, event: String) {
        val app = context.applicationContext
        val line = "${System.currentTimeMillis()} elapsed=${SystemClock.elapsedRealtime()} pid=${android.os.Process.myPid()} $event\n"
        Log.i("PracticeTimer", line.trim())
        writer.execute {
            runCatching {
                val file = File(app.filesDir, "timer-diagnostics.log")
                if (file.length() > 128 * 1024) {
                    val previous = File(app.filesDir, "timer-diagnostics.previous.log")
                    previous.delete()
                    file.renameTo(previous)
                }
                file.appendText(line)
            }.onFailure { Log.e("PracticeTimer", "Diagnostic write failed", it) }
        }
    }

    /** Call on Dispatchers.IO, never wait for the writer on the UI thread. */
    fun snapshot(context: Context): String = writer.submit<String> {
        val pm = context.getSystemService(PowerManager::class.java)
        buildString {
            appendLine("一分钟禅修 ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("${Build.MANUFACTURER} ${Build.MODEL}, Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT}")
            appendLine("exactAlarm=${ExactAlarmPermission.canSchedule(context)} batteryExempt=${pm?.isIgnoringBatteryOptimizations(context.packageName)}")
            for (name in listOf("timer-diagnostics.previous.log", "timer-diagnostics.log")) {
                val file = File(context.filesDir, name)
                if (file.isFile) append(file.readText())
            }
        }
    }.get()
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
