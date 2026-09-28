package com.ruru.practice.core.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import com.ruru.practice.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BellSoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("ruru_bell_sound", Context.MODE_PRIVATE)
    private var player: MediaPlayer? = null

    val customSoundName: String?
        get() = prefs.getString("name", null)

    @Synchronized
    fun setCustomSound(uri: Uri, displayName: String?) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        prefs.edit().putString("uri", uri.toString()).putString("name", displayName ?: "自定义音频").apply()
    }

    @Synchronized
    fun useDefaultSound() {
        prefs.edit().clear().apply()
        stopInternal()
    }

    @Synchronized
    fun play() {
        stopInternal()
        val customUri = prefs.getString("uri", null)?.let(Uri::parse)
        val created = if (customUri != null) createFromUri(customUri) ?: createDefault() else createDefault()
        if (created == null) return
        player = created
        runCatching {
            created.setVolume(0.52f, 0.52f)
            created.setOnCompletionListener { completed ->
                synchronized(this) { if (player === completed) player = null }
                runCatching { completed.release() }
            }
            created.start()
        }.onFailure {
            if (player === created) player = null
            runCatching { created.release() }
        }
    }

    private fun createDefault(): MediaPlayer? = MediaPlayer.create(context, R.raw.soft_bell)

    private fun createFromUri(uri: Uri): MediaPlayer? = runCatching {
        MediaPlayer().apply {
            setAudioAttributes(audioAttributes())
            setDataSource(context, uri)
            prepare()
        }
    }.getOrNull()

    private fun audioAttributes() = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    @Synchronized fun stop() = stopInternal()
    private fun stopInternal() {
        val current = player ?: return
        player = null
        runCatching { current.stop() }
        runCatching { current.release() }
    }
}
