package com.ruru.practice.feature.meditation

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import com.ruru.practice.core.util.TimerProbeSample
import java.util.concurrent.atomic.AtomicBoolean

/** Observation only: does not run a second timer, acquire a wake lock or play alerts. */
class TimerExecutionProbe(context: Context, private val label: String) {
    private val app = context.applicationContext
    private val walking = label == "walking"
    @Volatile private var postedAt = 0L
    @Volatile private var lastAckLatency = 0L
    private val thread = HandlerThread("timer-probe-$label")
    private val main = Handler(Looper.getMainLooper())
    private lateinit var worker: Handler
    private val stopped = AtomicBoolean(false)
    private val ackPending = AtomicBoolean(false)
    @Volatile private var lastMainAck = SystemClock.uptimeMillis()
    private var previous = TimerProbeSample(SystemClock.elapsedRealtime(), SystemClock.uptimeMillis())
    private var lastStack = 0L
    private val ack = Runnable {
        lastMainAck = SystemClock.uptimeMillis()
        lastAckLatency = (lastMainAck - postedAt).coerceAtLeast(0L)
        ackPending.set(false)
    }
    private val sample = object : Runnable {
        override fun run() {
            if (stopped.get()) return
            try {
                val now = TimerProbeSample(SystemClock.elapsedRealtime(), SystemClock.uptimeMillis())
                val mainLag = (now.uptime - lastMainAck).coerceAtLeast(0L)
                TimerDiagnostics.record(app, "probe label=$label ${now.deltaDescription(previous)} mainAckAgeMs=$mainLag ackPending=${ackPending.get()} lastAckLatencyMs=$lastAckLatency", walking)
                // At most one outstanding main-thread callback and one bounded stack / 5 min.
                if (ackPending.get() && mainLag >= 90_000L && now.uptime - lastStack >= 300_000L) {
                    lastStack = now.uptime
                    val stack = Looper.getMainLooper().thread.stackTrace.take(16).joinToString(" <- ")
                    TimerDiagnostics.record(app, "probe_main_stack label=$label $stack", walking)
                }
                previous = now
                TimerDiagnostics.capture(app, "probe_snapshot", walking)
                if (ackPending.compareAndSet(false, true)) { postedAt = now.uptime; main.post(ack) }
            } catch (error: Exception) {
                TimerDiagnostics.record(app, "probe_failed ${error.javaClass.simpleName}", walking)
            } finally {
                if (!stopped.get()) worker.postDelayed(this, 30_000L)
            }
        }
    }

    fun start() {
        thread.start()
        worker = Handler(thread.looper)
        worker.post(sample)
    }

    fun stop() {
        if (!stopped.compareAndSet(false, true)) return
        worker.removeCallbacks(sample)
        main.removeCallbacks(ack)
        // Capture the final gap even if the UI completed before the probe woke up.
        worker.post {
            val now = TimerProbeSample(SystemClock.elapsedRealtime(), SystemClock.uptimeMillis())
            TimerDiagnostics.record(app, "probe_final label=$label ${now.deltaDescription(previous)} mainAckAgeMs=${now.uptime - lastMainAck}", walking)
            main.removeCallbacks(ack)
            thread.quitSafely()
        }
    }
}
