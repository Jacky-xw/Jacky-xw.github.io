package com.ruru.practice.domain.usecase

import androidx.room.withTransaction
import com.ruru.practice.data.dao.*
import com.ruru.practice.data.database.RuruDatabase
import com.ruru.practice.data.entity.WeeklyReportEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

/**
 * Settles the previous Monday-Sunday practice cycle once per new week.
 * The report is written before old raw records are removed, so no source data
 * is lost before the weekly analysis has been produced.
 */
class WeeklyRolloverUseCase @Inject constructor(
    private val db: RuruDatabase,
    private val meditationDao: MeditationDao,
    private val walkingDao: WalkingSessionDao,
    private val rootDao: RootProtectionDao,
    private val fiveCoverDao: FiveCoverDao,
    private val mindfulnessDao: MindfulnessEventDao,
    private val preceptDao: PreceptDao,
    private val dailyPracticeDao: DailyPracticeDao,
    private val eightPreceptDao: EightPreceptSessionDao,
    private val reflectionDao: ReflectionDao,
    private val aggregateDao: FiveAggregateDao,
    private val originDao: DependentOriginationDao,
    private val weeklyReportDao: WeeklyReportDao
) {
    suspend operator fun invoke() = db.withTransaction {
        val now = Calendar.getInstance()
        val thisMonday = mondayStart(now)
        val lastMonday = (thisMonday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -7) }
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val start = date.format(lastMonday.time)
        val end = date.format(thisMonday.time)
        val reportEnd = date.format((thisMonday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }.time)
        val startMillis = lastMonday.timeInMillis
        val endMillis = thisMonday.timeInMillis

        val currentReport = weeklyReportDao.getCurrent()
        if (currentReport?.weekStart != start) {
            val meditationCount = meditationDao.countBetween(start, end)
            val walkingCount = walkingDao.countBetween(start, end)
            val rootCount = rootDao.countBetween(start, end)
            val fiveCoverCount = fiveCoverDao.countBetween(start, end)
            val mindfulnessCount = mindfulnessDao.countBetween(start, end)
            val preceptCount = preceptDao.countBetween(start, end)
            val dailyCount = dailyPracticeDao.countBetween(start, end)
            val eightPreceptCount = eightPreceptDao.countBetween(start, end)
            val reflectionCount = reflectionDao.countBetween(startMillis, endMillis)
            val aggregateCount = aggregateDao.countBetween(startMillis, endMillis)
            val originCount = originDao.countBetween(startMillis, endMillis)
            val seconds = meditationDao.sumSecondsBetween(start, end) + walkingDao.sumSecondsBetween(start, end)
            val formalCount = meditationCount + walkingCount + rootCount
            val observationCount = fiveCoverCount + mindfulnessCount + aggregateCount + originCount
            val totalSourceCount = formalCount + observationCount + preceptCount + dailyCount + eightPreceptCount + reflectionCount
            val hindrances = fiveCoverDao.countTypesBetween(start, end)

            // On a fresh install there is no previous-week source data. Keep the
            // report table empty so the UI can show the requested first-use text.
            if (totalSourceCount > 0) {
                val strongest = hindrances.firstOrNull()?.let { "${it.type}（${it.count}次）" } ?: "本周未记录明显五盖"
                val content = buildString {
                    append("上周共完成正式练习 ${formalCount} 次，安般念与经行累计 ${seconds / 60} 分钟。\n")
                    append("其中安般念 ${meditationCount} 次、经行 ${walkingCount} 次、护根 ${rootCount} 次；")
                    append("观察记录 ${observationCount} 次（五盖 ${fiveCoverCount}、当下事件 ${mindfulnessCount}、五蕴 ${aggregateCount}、缘起 ${originCount}）；")
                    append("戒行复盘 ${preceptCount} 次、每日修行 ${dailyCount} 次、八戒自检 ${eightPreceptCount} 次、独立复盘 ${reflectionCount} 次。\n")
                    append("上周共有 ${totalSourceCount} 条记录参与统计。\n")
                    append("五盖记录中最常出现：$strongest。")
                    if (formalCount == 0) append("下周可先恢复一个稳定、可持续的正式练习节奏。")
                    else if (reflectionCount == 0) append("下周可在练习后补一条简短复盘，让练习与日常变化更容易对应。")
                    else append("下周继续保持练习与复盘配对，重点观察重复出现的身心模式。")
                }
                weeklyReportDao.replace(WeeklyReportEntity(weekStart = start, weekEnd = reportEnd, content = content))
            } else {
                weeklyReportDao.clear()
            }
        }

        // Only the current Monday-Sunday cycle remains as raw history.
        meditationDao.deleteBefore(end)
        walkingDao.deleteBefore(end)
        rootDao.deleteBefore(end)
        fiveCoverDao.deleteBefore(end)
        mindfulnessDao.deleteBefore(end)
        preceptDao.deleteBefore(end)
        dailyPracticeDao.deleteBefore(end)
        eightPreceptDao.deleteBefore(end)
        reflectionDao.deleteBefore(endMillis)
        aggregateDao.deleteBefore(endMillis)
        originDao.deleteBefore(endMillis)
    }

    private fun mondayStart(source: Calendar): Calendar = (source.clone() as Calendar).apply {
        // Calculate the offset explicitly instead of relying on the device
        // locale's first-day-of-week setting. Sunday therefore maps back six
        // days to the preceding Monday, while Monday maps to itself.
        val daysSinceMonday = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
        add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
