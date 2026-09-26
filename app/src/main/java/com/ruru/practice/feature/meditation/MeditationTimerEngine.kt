package com.ruru.practice.feature.meditation

import kotlinx.coroutines.*

class MeditationTimerEngine {
    private var job: Job? = null

    fun start(onTick:(Int)->Unit) {
        var elapsed = 0
        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(1000)
                elapsed++
                onTick(elapsed)
            }
        }
    }

    fun stop() {
        job?.cancel()
    }
}
