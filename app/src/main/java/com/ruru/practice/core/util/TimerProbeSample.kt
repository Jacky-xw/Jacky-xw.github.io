package com.ruru.practice.core.util

/** Differences are evidence, not a definitive OEM-freezer detector. */
data class TimerProbeSample(val elapsed: Long, val uptime: Long) {
    fun elapsedGap(previous: TimerProbeSample) = (elapsed - previous.elapsed).coerceAtLeast(0L)
    fun uptimeGap(previous: TimerProbeSample) = (uptime - previous.uptime).coerceAtLeast(0L)
    fun sleepGap(previous: TimerProbeSample) = (elapsedGap(previous) - uptimeGap(previous)).coerceAtLeast(0L)
    fun deltaDescription(previous: TimerProbeSample): String =
        "elapsedGapMs=${elapsedGap(previous)} uptimeGapMs=${uptimeGap(previous)} sleepGapMs=${sleepGap(previous)}"
}
