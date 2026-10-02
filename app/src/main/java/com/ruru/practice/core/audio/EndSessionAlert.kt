package com.ruru.practice.core.audio

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session-end haptics with a distinctive pattern:
 * buzz ~ pause ~ buzz ~ pause ~ buzz
 * so it is unlikely to be confused with a single notification pulse.
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

    fun vibrateEndPattern() {
        if (!vibrationEnabled) return
        val vibrator = vibrator() ?: return
        // 0 delay, 450ms buzz, 400ms silence, 450ms buzz, 400ms silence, 700ms buzz
        val timings = longArrayOf(0, 450, 400, 450, 400, 700)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }

    private fun vibrator(): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    companion object {
        private const val KEY_VIBRATE = "end_vibrate_enabled"
    }
}
