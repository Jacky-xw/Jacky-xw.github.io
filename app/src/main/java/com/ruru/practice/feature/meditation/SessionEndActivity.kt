package com.ruru.practice.feature.meditation

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ruru.practice.R
import com.ruru.practice.core.ui.RuruPracticeTheme

/**
 * Shown over the lock screen when a seat ends. Plays the end bell on the
 * ALARM stream (system clock pattern).
 */
class SessionEndActivity : ComponentActivity() {
    private var player: MediaPlayer? = null
    private var longSeat: Boolean = false
    private var titleText by mutableStateOf("修习时间已到")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        longSeat = intent.getBooleanExtra(EXTRA_LONG_SEAT, false)
        titleText = if (longSeat) "安般念已到时，请确认出定" else "修习时间已到"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val kg = getSystemService(KeyguardManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            kg?.requestDismissKeyguard(this, null)
        }

        playBell(loop = longSeat)
        vibrate(loop = longSeat)
        MeditationTimerBridge.publishNaturalEndDue()

        setContent {
            RuruPracticeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (longSeat) {
                                "引磬将持续提醒，确认出定后停止。"
                            } else {
                                "计时已结束。"
                            },
                            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = { stopAndFinish() }) {
                            Text(if (longSeat) "已出定，停止提醒" else "知道了")
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        longSeat = intent.getBooleanExtra(EXTRA_LONG_SEAT, longSeat)
        titleText = if (longSeat) "安般念已到时，请确认出定" else "修习时间已到"
        if (player == null) {
            playBell(loop = longSeat)
            vibrate(loop = longSeat)
        }
    }

    private fun playBell(loop: Boolean) {
        stopPlayer()
        val uri = customBellUri()
            ?: Uri.parse("android.resource://$packageName/${R.raw.soft_bell}")
        runCatching {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@SessionEndActivity, uri)
                isLooping = loop
                if (!loop) {
                    setOnCompletionListener { stopPlayer() }
                }
                prepare()
                start()
            }
        }.onFailure {
            runCatching {
                val rtUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                val rt = RingtoneManager.getRingtone(this, rtUri)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    rt.isLooping = loop
                }
                rt.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                rt.play()
            }
        }
    }

    private fun customBellUri(): Uri? {
        val file = java.io.File(filesDir, "bell/custom_sound")
        return if (file.isFile && file.length() > 0L) Uri.fromFile(file) else null
    }

    private fun vibrate(loop: Boolean) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        } ?: return
        val timings = longArrayOf(0, 450, 400, 450, 400, 700, 900)
        val repeat = if (loop) 0 else -1
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(timings, repeat),
                    android.os.VibrationAttributes.Builder()
                        .setUsage(android.os.VibrationAttributes.USAGE_ALARM)
                        .build()
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                vibrator.vibrate(
                    VibrationEffect.createWaveform(timings, repeat),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, repeat)
            }
        }
    }

    private fun stopPlayer() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
    }

    private fun stopAndFinish() {
        stopPlayer()
        runCatching {
            val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Vibrator::class.java)
            }
            v?.cancel()
        }
        MeditationKeepAliveService.stop(applicationContext)
        MeditationTimerBridge.publishNaturalEndDue()
        finish()
    }

    override fun onDestroy() {
        if (longSeat && player != null) {
            stopPlayer()
            MeditationKeepAliveService.notifyEnd(applicationContext, longSeat = true)
        } else if (!longSeat) {
            stopPlayer()
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_LONG_SEAT = "long_seat"

        fun launch(context: Context, longSeat: Boolean) {
            val intent = Intent(context, SessionEndActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra(EXTRA_LONG_SEAT, longSeat)
            }
            context.startActivity(intent)
        }
    }
}
