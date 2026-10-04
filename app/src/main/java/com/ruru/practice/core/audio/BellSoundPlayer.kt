package com.ruru.practice.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioFocusRequest
import android.media.MediaPlayer
import android.net.Uri
import com.ruru.practice.R
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BellSoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("ruru_bell_sound", Context.MODE_PRIVATE)
    private val customSoundFile = File(context.filesDir, "bell/custom_sound")
    private var player: MediaPlayer? = null

    val customSoundName: String?
        get() = if (customSoundFile.isFile) {
            prefs.getString("name", null)?.takeIf { it.isNotBlank() }
        } else {
            null
        }

    /**
     * Copy the selected document into app-private storage immediately. This avoids depending on
     * a document provider's long-term URI permission and makes preview/end-of-timer playback use
     * the exact same local file.
     */
    @Synchronized
    fun setCustomSound(uri: Uri, displayName: String?): Result<Unit> {
        // Do not keep an old preview alive while its backing file is being replaced.
        stopInternal()
        val parent = customSoundFile.parentFile
            ?: return Result.failure(IllegalStateException("无法创建音频存储目录"))
        val temp = File(parent, "${customSoundFile.name}.tmp")
        return runCatching {
            check(parent.exists() || parent.mkdirs()) { "无法创建音频存储目录" }
            if (temp.exists()) check(temp.delete()) { "无法清理旧音频临时文件" }
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().buffered().use { output -> input.copyTo(output) }
                } ?: error("无法读取所选音频")
                check(temp.isFile && temp.length() > 0L) { "所选音频为空" }

                // Prepare the temporary file first so corrupt or unsupported audio never
                // replaces a working selection.
                val probe = createFromFile(temp)
                    ?: error("所选音频无法播放，请选择常见音频格式")
                runCatching { probe.release() }

                val previousName = prefs.getString("name", null)
                val backup = replaceCustomSound(temp)
                try {
                    check(
                        prefs.edit()
                            .putString("name", displayName?.takeIf { it.isNotBlank() } ?: "本地音频")
                            .commit()
                    ) { "无法保存音频设置" }
                    if (backup?.exists() == true) check(backup.delete()) { "无法清理旧音频备份" }
                } catch (error: Throwable) {
                    if (customSoundFile.exists()) customSoundFile.delete()
                    if (backup?.exists() == true) backup.renameTo(customSoundFile)
                    val restore = prefs.edit()
                    if (previousName == null) restore.remove("name") else restore.putString("name", previousName)
                    restore.apply()
                    throw error
                }
            } finally {
                if (temp.exists()) temp.delete()
            }
        }.map { Unit }
    }

    @Synchronized
    fun useDefaultSound(): Result<Unit> {
        stopInternal()
        return runCatching {
            if (customSoundFile.exists()) check(customSoundFile.delete()) { "无法删除本地音频" }
            check(prefs.edit().clear().commit()) { "无法保存默认提示音设置" }
        }.map { Unit }
    }

    /** Plays the selected sound, falling back to the bundled bell when needed. */
    @Synchronized
    fun play(alarm: Boolean = false): Boolean = playInternal(loop = false, alarm = alarm)

    /**
     * Continuous end-of-long-session alert. Stops only via [stop].
     * Used when a seat is >= 60 minutes so the practitioner is not left unaware.
     */
    @Synchronized
    fun playLooping(): Boolean = playInternal(loop = true, alarm = true)

    @Synchronized
    fun stop() = stopInternal()

    private fun playInternal(loop: Boolean, alarm: Boolean = loop): Boolean {
        stopInternal()
        requestAlarmFocusIfNeeded(alarm)
        val hasCustomSound = customSoundFile.isFile
        val custom = if (hasCustomSound) createFromFile(customSoundFile, alarm = alarm) else null
        if (custom != null && startPlayer(custom, loop)) return true
        custom?.let { runCatching { it.release() } }
        if (hasCustomSound) clearStoredCustomSound()
        val fallback = createDefault(alarm = alarm)
        return fallback != null && startPlayer(fallback, loop)
    }

    private fun startPlayer(candidate: MediaPlayer, loop: Boolean): Boolean {
        player = candidate
        return runCatching {
            candidate.setVolume(1f, 1f)
            candidate.isLooping = loop
            if (loop) {
                // Some containers ignore isLooping; restart from completion as a fallback.
                candidate.setOnCompletionListener { completed ->
                    synchronized(this) {
                        if (player !== completed) return@setOnCompletionListener
                        runCatching {
                            completed.seekTo(0)
                            completed.start()
                        }.onFailure {
                            player = null
                            runCatching { completed.release() }
                        }
                    }
                }
            } else {
                candidate.setOnCompletionListener { completed ->
                    synchronized(this) { if (player === completed) player = null }
                    runCatching { completed.release() }
                }
            }
            candidate.start()
            true
        }.getOrElse {
            if (player === candidate) player = null
            runCatching { candidate.release() }
            false
        }
    }

    /** Replaces the local file while keeping the previous one available for rollback. */
    private fun replaceCustomSound(temp: File): File? {
        val parent = customSoundFile.parentFile ?: error("无法创建音频存储目录")
        val backup = File(parent, "${customSoundFile.name}.bak")
        if (backup.exists()) check(backup.delete()) { "无法清理旧音频备份" }
        val hadOldSound = customSoundFile.isFile
        if (hadOldSound) check(customSoundFile.renameTo(backup)) { "无法替换现有提示音" }
        try {
            if (!temp.renameTo(customSoundFile)) {
                temp.copyTo(customSoundFile, overwrite = false)
                check(temp.delete()) { "无法清理临时音频文件" }
            }
            check(customSoundFile.isFile && customSoundFile.length() > 0L) { "无法保存所选音频" }
            return backup.takeIf { it.exists() }
        } catch (error: Throwable) {
            if (customSoundFile.exists()) customSoundFile.delete()
            if (backup.exists()) backup.renameTo(customSoundFile)
            throw error
        }
    }

    private fun clearStoredCustomSound() {
        runCatching { customSoundFile.delete() }
        prefs.edit().clear().apply()
    }

    private fun createDefault(alarm: Boolean = false): MediaPlayer? = runCatching {
        context.resources.openRawResourceFd(R.raw.soft_bell).use { afd ->
            MediaPlayer().apply {
                setAudioAttributes(audioAttributes(alarm))
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                prepare()
            }
        }
    }.getOrNull()

    private fun createFromFile(file: File, alarm: Boolean = false): MediaPlayer? = runCatching {
        MediaPlayer().apply {
            setAudioAttributes(audioAttributes(alarm))
            setDataSource(file.absolutePath)
            prepare()
        }
    }.getOrNull()

    private fun requestAlarmFocusIfNeeded(alarm: Boolean) {
        if (!alarm) return
        runCatching {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttributes(alarm = true))
                    .setAcceptsDelayedFocusGain(true)
                    .build()
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        }
    }

    private fun audioAttributes(alarm: Boolean = false) = AudioAttributes.Builder()
        .setUsage(if (alarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA)
        .setContentType(
            if (alarm) AudioAttributes.CONTENT_TYPE_SONIFICATION
            else AudioAttributes.CONTENT_TYPE_MUSIC
        )
        .build()


    private fun stopInternal() {
        val current = player ?: return
        player = null
        runCatching { current.stop() }
        runCatching { current.release() }
    }
}
