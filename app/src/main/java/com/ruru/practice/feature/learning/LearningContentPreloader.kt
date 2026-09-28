package com.ruru.practice.feature.learning

import android.content.Context
import com.ruru.practice.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Owns the small on-disk cache used by the learning reader.
 *
 * Important rules:
 * 1. All disk I/O happens on Dispatchers.IO.
 * 2. Cache writes are atomic (temporary file -> rename).
 * 3. A failed cache build never prevents the reader from opening; the reader can
 *    still fall back to packaged raw resources on its own IO coroutine.
 */
object LearningContentPreloader {
    private const val PREF = "learning_content_cache"
    private const val KEY_READY = "ready"
    private const val KEY_VERSION = "cache_version"
    private const val CACHE_VERSION = 5

    private val preloadMutex = Mutex()
    @Volatile private var loaded = false

    private data class Entry(val resourceId: Int, val fileName: String)

    private val entries = listOf(
        Entry(R.raw.bodhisattva_overview, "bodhisattva_overview.cache"),
        Entry(R.raw.buddha_image, "buddha_image.cache"),
        Entry(R.raw.guyinfujing_upper, "yinfu_upper.cache"),
        Entry(R.raw.guyinfujing_lower, "yinfu_lower.cache")
    )

    private fun cacheDir(context: Context): File =
        File(context.applicationContext.filesDir, "learning_cache").apply { mkdirs() }

    private fun valid(file: File): Boolean = file.isFile && file.length() > 0L

    suspend fun preload(context: Context): Boolean = withContext(Dispatchers.IO) {
        preloadMutex.withLock {
            if (loaded) return@withLock true

            val appContext = context.applicationContext
            val prefs = appContext.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            val dir = cacheDir(appContext)

            if (prefs.getInt(KEY_VERSION, 0) != CACHE_VERSION) {
                // Delete only files owned by this cache. A failed deletion is not fatal:
                // atomicWrite below replaces that entry when possible.
                entries.forEach { File(dir, it.fileName).delete() }
                dir.listFiles { file -> file.name.endsWith(".tmp") }?.forEach(File::delete)
                prefs.edit().remove(KEY_READY).remove(KEY_VERSION).commit()
            }

            val success = runCatching {
                entries.forEach { entry ->
                    val target = File(dir, entry.fileName)
                    if (!valid(target)) {
                        val text = appContext.resources.openRawResource(entry.resourceId)
                            .bufferedReader(Charsets.UTF_8)
                            .use { it.readText() }
                        atomicWrite(target, text)
                    }
                    check(valid(target)) { "Invalid learning cache: ${entry.fileName}" }
                }
            }.isSuccess

            if (success) {
                loaded = true
                prefs.edit()
                    .putBoolean(KEY_READY, true)
                    .putInt(KEY_VERSION, CACHE_VERSION)
                    .commit()
            }
            success
        }
    }

    private fun atomicWrite(target: File, text: String) {
        val temp = File(target.parentFile, "${target.name}.tmp")
        temp.writeText(text, Charsets.UTF_8)
        if (!temp.renameTo(target)) {
            // renameTo may fail when replacing an existing file on some devices.
            target.delete()
            check(temp.renameTo(target)) { "Unable to publish ${target.name}" }
        }
    }

    fun get(context: Context, resourceId: Int): String? {
        val entry = entries.firstOrNull { it.resourceId == resourceId } ?: return null
        return readCache(context.applicationContext, entry.fileName)
    }

    fun getYinfuUpper(context: Context): String? = readCache(context.applicationContext, "yinfu_upper.cache")
    fun getYinfuLower(context: Context): String? = readCache(context.applicationContext, "yinfu_lower.cache")

    /** Must be called from an IO coroutine when cache fallback may read raw resources. */
    fun readWithRawFallback(context: Context, resourceId: Int): String {
        get(context, resourceId)?.let { return it }
        return context.applicationContext.resources.openRawResource(resourceId)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }
    }

    private fun readCache(context: Context, name: String): String? {
        val file = File(cacheDir(context), name)
        return runCatching {
            if (valid(file)) file.readText(Charsets.UTF_8) else null
        }.getOrNull()
    }
}
