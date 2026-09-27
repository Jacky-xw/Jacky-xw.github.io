package com.ruru.practice.feature.meditation

data class MeditationFlowState(
    val selectedMinutes: Int = 10,
    val isRunning: Boolean = false,
    val elapsedSeconds: Int = 0,
    val completed: Boolean = false,
    val note: String = ""
)
