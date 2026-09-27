package com.ruru.practice.feature.learning

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

class LearningSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LearningSettingsStore(application)

    var fontSize by mutableIntStateOf(store.fontSize.coerceIn(14, 26))
        private set
    var pageWidth by mutableIntStateOf(store.pageWidth.coerceIn(70, 100))
        private set
    var lineSpacing by mutableIntStateOf(store.lineSpacing.coerceIn(120, 240))
        private set
    var nightMode by mutableStateOf(store.nightMode)
        private set

    fun updateFontSize(value: Int) { fontSize = value.coerceIn(14, 26); store.fontSize = fontSize }
    fun updatePageWidth(value: Int) { pageWidth = value.coerceIn(70, 100); store.pageWidth = pageWidth }
    fun updateLineSpacing(value: Int) { lineSpacing = value.coerceIn(120, 240); store.lineSpacing = lineSpacing }
    fun updateNightMode(value: Boolean) { nightMode = value; store.nightMode = value }
}
