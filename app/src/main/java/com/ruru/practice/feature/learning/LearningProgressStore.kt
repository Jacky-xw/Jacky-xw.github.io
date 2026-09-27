package com.ruru.practice.feature.learning

import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.*

object LearningProgressStore {
    fun markCompleted(prefs: SharedPreferences, title: String, tab: Int) {
        val key = lessonKeyFor(title)
        val already = prefs.getBoolean(key, false)
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val editor = prefs.edit()
            .putBoolean(key, true)
            .putString("last_lesson", title)
            .putString("last_article_id", articleIdFor(title))
            .putString("last_section_id", sectionIdFor(title))
            .putLong("last_time", System.currentTimeMillis())
            .putInt("last_tab", tab)
        if (!already) editor.putInt("daily_$day", prefs.getInt("daily_$day", 0) + 1)
        editor.apply()
    }
}

fun articleIdFor(title: String): String = "article_" + title.hashCode().toUInt().toString(16)
fun sectionIdFor(title: String): String = "section_" + title.hashCode().toUInt().toString(16)
fun lessonKeyFor(title: String): String = "read_" + articleIdFor(title)
