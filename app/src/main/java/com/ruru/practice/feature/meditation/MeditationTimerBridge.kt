package com.ruru.practice.feature.meditation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Cross-boundary signal: AlarmManager / Service → ViewModel. */
object MeditationTimerBridge {
    private val _naturalEndDue = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val naturalEndDue = _naturalEndDue.asSharedFlow()

    fun publishNaturalEndDue() {
        _naturalEndDue.tryEmit(Unit)
    }
}
