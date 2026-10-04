package com.ruru.practice.core.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ruru.practice.domain.usecase.WeeklyRolloverEvents
import com.ruru.practice.domain.usecase.WeeklyRolloverUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Background settlement for the Monday–Sunday practice cycle.
 * The use case is idempotent: if this week was already settled, it is a no-op.
 * After each run the scheduler enqueues the next Monday one-shot.
 */
@HiltWorker
class WeeklyRolloverWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val weeklyRollover: WeeklyRolloverUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            weeklyRollover()
            WeeklyRolloverEvents.publish()
            // Always re-arm the next Monday shot so a successful (or no-op)
            // run does not leave the chain broken after process death.
            WeeklyRolloverScheduler.scheduleNextMonday(applicationContext)
            Result.success()
        } catch (_: Exception) {
            // Retry with backoff; do not reschedule next Monday until success
            // so a transient DB failure is not silently skipped for a week.
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_MONDAY_WORK = "weekly_rollover_monday"
        const val UNIQUE_DAILY_GUARD = "weekly_rollover_daily_guard"
    }
}
