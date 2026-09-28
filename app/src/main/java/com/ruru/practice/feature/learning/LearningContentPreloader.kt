package com.ruru.practice.feature.learning

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 学习内容首次启动预加载。
 * 使用应用内部 filesDir 缓存，不需要任何存储权限。
 */
object LearningContentPreloader {
    private const val PREF = "learning_content_cache"
    private const val KEY_READY = "ready"
    private const val KEY_YINFU_READY = "yinfu_ready"
    private const val KEY_VERSION = "cache_version"
    private const val CACHE_VERSION = 4
    @Volatile private var loaded = false

    private fun cacheDir(context: Context): File =
        File(context.filesDir, "learning_cache").apply { mkdirs() }

    private fun valid(file: File): Boolean =
        file.exists() && file.length() > 100

    suspend fun preload(context: Context) = withContext(Dispatchers.IO) {
        if (loaded) return@withContext

        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val dir = cacheDir(context)

        if (prefs.getInt(KEY_VERSION, 0) != CACHE_VERSION) {
            dir.listFiles()?.forEach {
                if (!it.delete()) {
                    it.deleteOnExit()
                }
            }
            prefs.edit().clear().apply()
        }

        val success = runCatching {
            val normalResources = listOf(
                com.ruru.practice.R.raw.bodhisattva_overview to "bodhisattva_overview.cache",
                com.ruru.practice.R.raw.buddha_image to "buddha_image.cache"
            )

            normalResources.forEach { (id, name) ->
                val file = File(dir, name)
                if (!valid(file)) {
                    context.resources.openRawResource(id).bufferedReader(Charsets.UTF_8).use {
                        file.writeText(it.readText(), Charsets.UTF_8)
                    }
                }
            }

            prepareAncientYinfu(context, prefs)
        }.isSuccess

        if (success) {
            loaded = true
            prefs.edit()
                .putBoolean(KEY_READY, true)
                .putBoolean(KEY_YINFU_READY, true)
                .putInt(KEY_VERSION, CACHE_VERSION)
                .apply()
        }
    }

    /**
     * 阴符经在首次启动阶段创建独立缓存。
     * 不依赖 Android resourceId，避免资源重排造成缓存漂移。
     */
    private fun prepareAncientYinfu(context: Context, prefs: android.content.SharedPreferences) {
        val dir = cacheDir(context)
        val files = listOf(
            com.ruru.practice.R.raw.guyinfujing_upper to "yinfu_upper.cache",
            com.ruru.practice.R.raw.guyinfujing_lower to "yinfu_lower.cache"
        )

        files.forEach { (id, name) ->
            val file = File(dir, name)
            if (!valid(file)) {
                context.resources.openRawResource(id).bufferedReader(Charsets.UTF_8).use {
                    file.writeText(it.readText(), Charsets.UTF_8)
                }
            }
        }
    }

    /**
     * 通用学习缓存读取接口。
     * 非阴符经内容仍可通过 resourceId 映射到固定缓存文件。
     */
    fun get(context: Context, resourceId: Int): String? {
        val name = when (resourceId) {
            com.ruru.practice.R.raw.bodhisattva_overview -> "bodhisattva_overview.cache"
            com.ruru.practice.R.raw.buddha_image -> "buddha_image.cache"
            com.ruru.practice.R.raw.guyinfujing_upper -> "yinfu_upper.cache"
            com.ruru.practice.R.raw.guyinfujing_lower -> "yinfu_lower.cache"
            else -> null
        }
        return name?.let { readCache(context, it) }
    }

    fun getYinfuUpper(context: Context): String? =
        readCache(context, "yinfu_upper.cache")

    fun getYinfuLower(context: Context): String? =
        readCache(context, "yinfu_lower.cache")

    private fun readCache(context: Context, name: String): String? {
        val file = File(cacheDir(context), name)
        return runCatching {
            if (valid(file)) file.readText(Charsets.UTF_8) else null
        }.getOrNull()
    }
}
