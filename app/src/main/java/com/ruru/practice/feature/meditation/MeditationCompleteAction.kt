package com.ruru.practice.feature.meditation

data class MeditationCompleteAction(
    val durationSeconds: Int,
    val observation: String,
    val afterState: String
)
