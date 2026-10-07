package com.ruru.practice.feature.practice

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object WalkingTimerBridge {
    private val _naturalEndDue = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val naturalEndDue = _naturalEndDue.asSharedFlow()
    fun publishNaturalEndDue() { _naturalEndDue.tryEmit(Unit) }
}
