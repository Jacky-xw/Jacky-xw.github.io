package com.ruru.practice.feature.learning

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

class LearningSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LearningSettingsStore(application)

    var fontSize by mutableIntStateOf(store.fontSize)
        private set
    var lineHeight by mutableIntStateOf(store.lineHeight)
        private set
    var pageWidth by mutableIntStateOf(store.pageWidth)
        private set
    var nightMode by mutableStateOf(store.nightMode)
        private set
    var readingMode by mutableStateOf(store.readingMode)
        private set
    var textScale by mutableFloatStateOf(store.textScale)
        private set
    var pagedMode by mutableStateOf(store.pagedMode)
        private set

    fun setFontSize(value: Int) { fontSize = value.coerceIn(14, 26); store.fontSize = fontSize }
    fun setLineHeight(value: Int) { lineHeight = value.coerceIn(20, 42); store.lineHeight = lineHeight }
    fun setPageWidth(value: Int) { pageWidth = value.coerceIn(70, 100); store.pageWidth = pageWidth }
    fun setNightMode(value: Boolean) { nightMode = value; store.nightMode = value }
    fun setReadingMode(value: Boolean) { readingMode = value; store.readingMode = value }
    fun setTextScale(value: Float) { textScale = value.coerceIn(0.8f, 1.4f); store.textScale = textScale }
    fun setPagedMode(value: Boolean) { pagedMode = value; store.pagedMode = value }
}
