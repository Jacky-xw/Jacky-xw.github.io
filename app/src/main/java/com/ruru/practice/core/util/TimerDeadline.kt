package com.ruru.practice.core.util

/** Pure deadline math shared by service recovery and its uptime-based Handler ticker. */
object TimerDeadline {
    fun matchesAlarm(delivered: Long, expected: Long): Boolean = delivered > 0L && delivered == expected

    fun restore(startedAtMillis: Long, totalSeconds: Int, elapsedBeforeStart: Int): Long =
        startedAtMillis + (totalSeconds - elapsedBeforeStart.coerceAtLeast(0)).coerceAtLeast(0) * 1000L

    fun nextCheckDelay(remainingMillis: Long): Long {
        if (remainingMillis <= 0L) return 0L
        val interval = when {
            remainingMillis > 60_000L -> 20_000L
            remainingMillis > 15_000L -> 5_000L
            else -> 1_000L
        }
        return minOf(interval, remainingMillis)
    }
}
