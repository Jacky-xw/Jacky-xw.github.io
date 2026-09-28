package com.ruru.practice.feature.meditation

enum class MeditationPhase {
    READY,
    RUNNING,
    PAUSED,
    COMPLETED,
    SAVED
}

data class MeditationTimerState(
    val phase: MeditationPhase = MeditationPhase.READY,
    val selectedMinutes: Int = 20,
    val practiceStep: Int = 1,
    val elapsedSeconds: Int = 0,
    val remainingSeconds: Int = 20 * 60,
    val observation: String = "",
    val afterState: String = "",
    val saveState: MeditationSaveState = MeditationSaveState()
)
