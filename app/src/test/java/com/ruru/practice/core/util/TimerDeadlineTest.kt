package com.ruru.practice.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerDeadlineTest {
    @Test fun restoresOneHourWithoutRestartingTheDuration() {
        assertEquals(3_601_000L, TimerDeadline.restore(1_000L, 3600, 0))
    }
    @Test fun restoresPausedThenResumedSegment() {
        assertEquals(2_701_000L, TimerDeadline.restore(1_000L, 3600, 900))
    }
    @Test fun supportsSixHoursWithoutOverflow() {
        assertEquals(21_601_000L, TimerDeadline.restore(1_000L, 21600, 0))
    }
    @Test fun completedSessionHasNoExtraRemainingTime() {
        assertEquals(1_000L, TimerDeadline.restore(1_000L, 3600, 4000))
    }
    @Test fun lastTickDoesNotOvershootTheDeadline() {
        assertEquals(250L, TimerDeadline.nextCheckDelay(250L))
        assertEquals(0L, TimerDeadline.nextCheckDelay(-1L))
        assertEquals(20_000L, TimerDeadline.nextCheckDelay(3_600_000L))
        for (remaining in listOf(1L, 999L, 15000L, 15001L, 60000L, 60001L)) {
            assertTrue(TimerDeadline.nextCheckDelay(remaining) in 1L..remaining)
        }
    }
}
