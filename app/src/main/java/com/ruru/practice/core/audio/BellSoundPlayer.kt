package com.ruru.practice.core.audio

import android.content.Context
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

    fun play() {
        stop()
        player = MediaPlayer.create(context, R.raw.soft_bell)?.apply {
            setVolume(0.42f, 0.42f)
            setOnCompletionListener { completed ->
                completed.release()
                if (player === completed) player = null
            }
            start()
        }
    }

    fun stop() {
        player?.let { runCatching { it.stop() }; it.release() }
        player = null
    }
}
