package com.ruru.practice.core.startup

import android.content.Context
import androidx.annotation.RawRes
import com.ruru.practice.R

/**
 * Holds learning-center raw texts after splash preload so the first open of
 * 学习 does not block on disk reads of the larger documents.
 */
object LearningTextCache {
    @Volatile
    private var yinfu: List<String>? = null

    @Volatile
    private var bodhisattva: List<String>? = null

    fun preload(context: Context) {
        if (yinfu == null) {
            yinfu = listOf(
                readRaw(context, R.raw.guyinfujing_upper),
                readRaw(context, R.raw.guyinfujing_lower)
            )
        }
        if (bodhisattva == null) {
            bodhisattva = listOf(
                readRaw(context, R.raw.bodhisattva_overview),
                readRaw(context, R.raw.buddha_image)
            )
        }
    }

    fun yinfuTexts(context: Context): List<String> {
        preload(context)
        return yinfu ?: emptyList()
    }

    fun bodhisattvaTexts(context: Context): List<String> {
        preload(context)
        return bodhisattva ?: emptyList()
    }

    private fun readRaw(context: Context, @RawRes id: Int): String =
        runCatching {
            context.resources.openRawResource(id).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrElse { error ->
            "文本读取失败：${error.message ?: error.javaClass.simpleName}"
        }
}
