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

    private fun cacheDir(context: Context): File =
        File(context.filesDir, "learning_cache").apply { mkdirs() }

    suspend fun preload(context: Context) = withContext(Dispatchers.IO) {
        if (loaded) return@withContext
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val resources = listOf(
            com.ruru.practice.R.raw.bodhisattva_overview,
            com.ruru.practice.R.raw.buddha_image,
            com.ruru.practice.R.raw.guyinfujing_upper,
            com.ruru.practice.R.raw.guyinfujing_lower
        )

        val dir = cacheDir(context)
        val success = runCatching {
            resources.forEach { id ->
                val file = File(dir, "$id.txt")
                val text = if (prefs.getBoolean(KEY_READY, false) && file.exists()) {
                    file.readText(Charsets.UTF_8)
                } else {
                    context.resources.openRawResource(id).bufferedReader(Charsets.UTF_8).use {
                        it.readText()
                    }.also {
                        file.writeText(it, Charsets.UTF_8)
                    }
                }            }
        }.isSuccess

        if (success) {
            prefs.edit().putBoolean(KEY_READY, true).apply()        }
    }

    fun get(context: Context, resourceId: Int): String? {
        val file = File(cacheDir(context), "$resourceId.txt")
        return file.takeIf { it.exists() }?.readText(Charsets.UTF_8)
    }
}
