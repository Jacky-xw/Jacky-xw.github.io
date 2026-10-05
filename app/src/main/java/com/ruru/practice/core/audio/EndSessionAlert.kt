package com.ruru.practice.core.audio

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session-end haptics and device checks.
 * Distinct pattern: buzz ~ pause ~ buzz ~ pause ~ buzz.
 * Long seats (>= 60 min) use a repeating pattern that cannot be disabled.
 *
 * Lock-screen / Doze: end vibrations use ALARM usage so the system does not
 * suppress them when the display is off (plain BACKGROUND vibration is blocked).
 */
@Singleton
class EndSessionAlert @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("ruru_end_alert", Context.MODE_PRIVATE)
    private var wakeLock: PowerManager.WakeLock? = null

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_VIBRATE, value).apply()
        }

    /**
     * End-of-seat alerts use USAGE_ALARM / STREAM_ALARM, so volume checks
     * must follow the system alarm stream (not media).
     */
    fun alarmVolumeRatio(): Float {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        val current = am.getStreamVolume(AudioManager.STREAM_ALARM).coerceAtLeast(0)
        return current.toFloat() / max.toFloat()
    }

    fun isAlarmMutedOrVeryLow(): Boolean = alarmVolumeRatio() <= 0.05f

    fun alarmVolumePercent(): Int = (alarmVolumeRatio() * 100f).toInt().coerceIn(0, 100)

    // Backward-compatible aliases used by ViewModel / UI call sites.
    fun mediaVolumeRatio(): Float = alarmVolumeRatio()
    fun isMediaMutedOrVeryLow(): Boolean = isAlarmMutedOrVeryLow()
    fun mediaVolumePercent(): Int = alarmVolumePercent()

    /** 0–100, or -1 if unknown. */
    fun batteryPercent(): Int {
        val sticky = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return -1
        val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return -1
        return ((level * 100f) / scale).toInt().coerceIn(0, 100)
    }

    fun vibrateEndPattern() {
        if (!vibrationEnabled) return
        vibratePattern(repeat = false)
    }

    /** Mandatory long-seat end alert; ignores the optional vibration toggle. */
    fun startMandatoryLoopVibration() {
        acquireEndAlarmWakeLock()
        vibratePattern(repeat = true)
    }

    fun stopVibration() {
        vibrator()?.cancel()
        releaseEndAlarmWakeLock()
    }

    private fun vibratePattern(repeat: Boolean) {
        val vibrator = vibrator() ?: return
        // 0, buzz, gap, buzz, gap, buzz, longer gap before next cycle when repeating
        val timings = longArrayOf(0, 450, 400, 450, 400, 700, 900)
        val repeatIndex = if (repeat) 0 else -1
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val effect = VibrationEffect.createWaveform(timings, repeatIndex)
                val attrs = VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_ALARM)
                    .build()
                vibrator.vibrate(effect, attrs)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = VibrationEffect.createWaveform(timings, repeatIndex)
                val audioAttrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, audioAttrs)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, repeatIndex)
            }
        } catch (_: Throwable) {
            // Some OEMs reject ALARM usage from non-system apps; fall back.
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, repeatIndex))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(timings, repeatIndex)
                }
            }
        }
    }

    private fun acquireEndAlarmWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
        @Suppress("DEPRECATION")
        val lock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "ruru:meditation_end_alarm"
        )
        lock.setReferenceCounted(false)
        // Cap at 30 minutes of continuous hold; user should acknowledge sooner.
        lock.acquire(30 * 60 * 1000L)
        wakeLock = lock
    }

    private fun releaseEndAlarmWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }
        wakeLock = null
    }

    private fun vibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    companion object {
        private const val KEY_VIBRATE = "end_vibrate_enabled"
    }
}
