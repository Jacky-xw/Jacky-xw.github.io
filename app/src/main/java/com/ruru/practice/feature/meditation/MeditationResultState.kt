package com.ruru.practice.feature.meditation

data class MeditationResultState(
    val durationMinutes:Int = 0,
    val completed:Boolean = false,
    val observation:String = ""
)
