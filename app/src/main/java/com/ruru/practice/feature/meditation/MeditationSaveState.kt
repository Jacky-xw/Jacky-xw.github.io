package com.ruru.practice.feature.meditation

data class MeditationSaveState(
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val message: String? = null,
    val error: String? = null
)
