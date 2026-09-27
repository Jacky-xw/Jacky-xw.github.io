package com.ruru.practice.feature.meditation

data class MeditationState(
    val running:Boolean=false,
    val seconds:Int=0,
    val step:Int=1
)
