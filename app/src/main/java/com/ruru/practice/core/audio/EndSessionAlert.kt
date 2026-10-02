package com.ruru.practice.core.audio

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
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
 */
@Singleton
class EndSessionAlert @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("ruru_end_alert", Context.MODE_PRIVATE)

    var vibrationEnabled: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE, true)
        set(value) {
            prefs.edit().putBoolean(KEY_VIBRATE, value).apply()
        }

    fun mediaVolumeRatio(): Float {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val current = am.getStreamVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(0)
        return current.toFloat() / max.toFloat()
    }

    fun isMediaMutedOrVeryLow(): Boolean = mediaVolumeRatio() <= 0.05f

    fun mediaVolumePercent(): Int = (mediaVolumeRatio() * 100f).toInt().coerceIn(0, 100)

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
        vibratePattern(repeat = true)
    }

    fun stopVibration() {
        vibrator()?.cancel()
    }

    private fun vibratePattern(repeat: Boolean) {
        val vibrator = vibrator() ?: return
        // 0, buzz, gap, buzz, gap, buzz, longer gap before next cycle when repeating
        val timings = longArrayOf(0, 450, 400, 450, 400, 700, 900)
        val repeatIndex = if (repeat) 0 else -1
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, repeatIndex))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, repeatIndex)
        }
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
