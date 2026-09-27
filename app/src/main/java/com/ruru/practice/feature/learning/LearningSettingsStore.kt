package com.ruru.practice.feature.learning

import android.content.Context

/** Persistent reader preferences. Legacy V1.27 keys are migrated without deleting old data. */
class LearningSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("learning_reader_settings", Context.MODE_PRIVATE)
    private val legacy = context.getSharedPreferences("learning_progress", Context.MODE_PRIVATE)

    init {
        if (!prefs.getBoolean("migration_v128_done", false)) {
            prefs.edit()
                .putBoolean("reading_mode", prefs.getBoolean("reading_mode", legacy.getBoolean("reading_mode", false)))
                .putFloat("text_scale", prefs.getFloat("text_scale", legacy.getFloat("text_scale", 1f)))
                .putBoolean("migration_v128_done", true)
                .apply()
        }
    }

    var fontSize: Int
        get() = prefs.getInt("font_size", 17)
        set(value) { prefs.edit().putInt("font_size", value).apply() }
    var lineHeight: Int
        get() = prefs.getInt("line_height", 28)
        set(value) { prefs.edit().putInt("line_height", value).apply() }
    var pageWidth: Int
        get() = prefs.getInt("page_width", 94)
        set(value) { prefs.edit().putInt("page_width", value).apply() }
    var nightMode: Boolean
        get() = prefs.getBoolean("night_mode", false)
        set(value) { prefs.edit().putBoolean("night_mode", value).apply() }
    var readingMode: Boolean
        get() = prefs.getBoolean("reading_mode", false)
        set(value) { prefs.edit().putBoolean("reading_mode", value).apply() }
    var textScale: Float
        get() = prefs.getFloat("text_scale", 1f)
        set(value) { prefs.edit().putFloat("text_scale", value).apply() }
    var pagedMode: Boolean
        get() = prefs.getBoolean("paged_mode", false)
        set(value) { prefs.edit().putBoolean("paged_mode", value).apply() }
}
