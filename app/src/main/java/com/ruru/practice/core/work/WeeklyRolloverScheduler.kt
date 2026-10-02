package com.ruru.practice.core.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Schedules background weekly settlement.
 *
 * - One-shot work aimed at the next Monday 00:05 local time (primary).
 * - Daily periodic guard (secondary) so Doze/OEM delays cannot skip a week;
 *   [WeeklyRolloverUseCase] is idempotent so extra runs are safe.
 */
object WeeklyRolloverScheduler {

    fun schedule(context: Context) {
        scheduleNextMonday(context)
        scheduleDailyGuard(context)
    }

    fun scheduleNextMonday(context: Context) {
        val delayMs = millisUntilNextMondaySettlement().coerceAtLeast(TimeUnit.MINUTES.toMillis(1))
        val request = OneTimeWorkRequestBuilder<WeeklyRolloverWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .addTag(WeeklyRolloverWorker.UNIQUE_MONDAY_WORK)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WeeklyRolloverWorker.UNIQUE_MONDAY_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun scheduleDailyGuard(context: Context) {
        // Minimum periodic interval is 15 minutes; 1 day is enough as a safety net.
        val request = PeriodicWorkRequestBuilder<WeeklyRolloverWorker>(1, TimeUnit.DAYS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .addTag(WeeklyRolloverWorker.UNIQUE_DAILY_GUARD)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WeeklyRolloverWorker.UNIQUE_DAILY_GUARD,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Delay until the next Monday 00:05 local time.
     * If we are already on Monday before 00:05, target today; otherwise next Monday.
     * 00:05 (not 00:00) gives a small buffer past midnight for clock skew.
     */
    fun millisUntilNextMondaySettlement(): Long {
        val now = Calendar.getInstance()
        val target = (now.clone() as Calendar).apply {
            val daysSinceMonday = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // This week's Monday 00:05 is in the past → jump to next Monday.
            if (!after(now)) {
                add(Calendar.DAY_OF_YEAR, 7)
            }
        }
        return target.timeInMillis - now.timeInMillis
    }
}
