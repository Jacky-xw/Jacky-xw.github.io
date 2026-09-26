package com.ruru.practice.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.ruru.practice.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BellSoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var player: MediaPlayer? = null

    @Synchronized
    fun play() {
        stopInternal()
        val created = MediaPlayer.create(context, R.raw.soft_bell) ?: return
        player = created
        runCatching {
            created.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            created.setVolume(0.52f, 0.52f)
            created.setOnCompletionListener { completed ->
                synchronized(this) {
                    if (player === completed) player = null
                }
                runCatching { completed.release() }
            }
            created.start()
        }.onFailure {
            if (player === created) player = null
            runCatching { created.release() }
        }
    }

    @Synchronized
    fun stop() {
        stopInternal()
    }

    private fun stopInternal() {
        val current = player ?: return
        player = null
        runCatching { current.stop() }
        runCatching { current.release() }
    }
}
