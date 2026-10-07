package com.ruru.practice.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerProbeSampleTest {
    private val before = TimerProbeSample(100_000L, 80_000L)

    @Test fun normalThirtySecondSample() {
        val now = TimerProbeSample(130_000L, 110_000L)
        assertEquals(30_000L, now.elapsedGap(before))
        assertEquals(30_000L, now.uptimeGap(before))
        assertEquals(0L, now.sleepGap(before))
    }
    @Test fun deepSleepAdvancesElapsedButNotUptime() {
        val now = TimerProbeSample(700_000L, 110_000L)
        assertEquals(570_000L, now.sleepGap(before))
    }
    @Test fun longSchedulingGapIsNotAutomaticallyDeepSleep() {
        val now = TimerProbeSample(700_000L, 680_000L)
        assertEquals(600_000L, now.elapsedGap(before))
        assertEquals(0L, now.sleepGap(before))
    }
    @Test fun resetClockCannotProduceNegativeGaps() {
        val now = TimerProbeSample(1L, 1L)
        assertEquals(0L, now.elapsedGap(before))
        assertEquals(0L, now.sleepGap(before))
    }
}
