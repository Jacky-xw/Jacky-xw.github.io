package com.ruru.practice.feature.meditation

data class MeditationSessionSummary(
    val durationSeconds: Int,
    val observation: String,
    val afterState: String
)
