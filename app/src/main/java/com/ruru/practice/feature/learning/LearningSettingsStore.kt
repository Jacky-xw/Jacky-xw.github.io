package com.ruru.practice.feature.learning

import android.content.Context

/** Persistent settings for the lightweight reader. */
class LearningSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("learning_reader_settings", Context.MODE_PRIVATE)

    var fontSize: Int
        get() = prefs.getInt("font_size", 17)
        set(value) { prefs.edit().putInt("font_size", value).apply() }
    var pageWidth: Int
        get() = prefs.getInt("page_width", 94)
        set(value) { prefs.edit().putInt("page_width", value).apply() }
    var lineSpacing: Int
        get() = prefs.getInt("line_spacing_hundredths", 170)
        set(value) { prefs.edit().putInt("line_spacing_hundredths", value).apply() }
    var nightMode: Boolean
        get() = prefs.getBoolean("night_mode", false)
        set(value) { prefs.edit().putBoolean("night_mode", value).apply() }
}
