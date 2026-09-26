package com.ruru.practice.domain.usecase

import com.ruru.practice.data.repository.PracticeRepository
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class GetPracticeSummaryUseCase @Inject constructor(
    private val repository: PracticeRepository
) {
    suspend operator fun invoke(): PracticeSummary {
        val now = Date()
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateOnly = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = dateOnly.format(now)
        val calendar = Calendar.getInstance().apply {
            time = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -6)
        }
        val weekStart = calendar.time
        val weekStartLabel = format.format(weekStart)
        val weekEndLabel = format.format(now)

        val meditationOnlyCount = repository.getMeditationCount()
        val walkingAll = repository.getWalkingCount()
        val rootAll = repository.getRootProtectionCount()
        val meditationCount = meditationOnlyCount + walkingAll + rootAll
        val totalMinutes = ((repository.getTotalMeditationSeconds() + repository.getTotalWalkingSeconds()) / 60L).toInt()
        val recentPractices = buildList {
            repository.getRecentMeditations(12).forEach { session ->
                add(
                    RecentPracticeSummary(
                        key = "meditation-${session.id}",
                        date = session.date,
                        title = "安般念 · 第${session.practiceStep}步",
                        durationSeconds = session.durationSeconds,
                        detail = listOf(session.observation, session.afterState).filter { it.isNotBlank() }.joinToString(" · ")
                    )
                )
            }
            repository.getRecentWalking(12).forEach { session ->
                add(
                    RecentPracticeSummary(
                        key = "walking-${session.id}",
                        date = session.date,
                        title = "经行 · ${session.method}",
                        durationSeconds = session.durationSeconds,
                        detail = session.observation
                    )
                )
            }
            repository.getRecentRootProtections(12).forEach { session ->
                add(
                    RecentPracticeSummary(
                        key = "root-${session.id}",
                        date = session.date,
                        title = "护根 · ${session.sense}",
                        durationSeconds = null,
                        detail = listOf("触：${session.contact}", "受：${session.feeling}", "爱：${session.craving}", "取：${session.grasping}", session.response).filter { it.isNotBlank() }.joinToString(" · ")
                    )
                )
            }
        }.sortedByDescending { it.date }.take(20)
        val medToday = repository.countMeditationForDate(today)
        val walkingToday = repository.countWalkingForDate(today)
        val walkingWeek = repository.countWalkingBetween(weekStartLabel, weekEndLabel)
        val rootToday = repository.countRootProtectionForDate(today)
        val coverToday = repository.countFiveCoversForDate(today)
        val preceptToday = repository.countPreceptsForDate(today)
        val eventToday = repository.countMindfulnessForDate(today)
        val todayCalendar = Calendar.getInstance().apply {
            time = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val dayStartMillis = todayCalendar.timeInMillis
        val dayEndMillis = dayStartMillis + 24L * 60L * 60L * 1000L
        val actualOriginToday = repository.countDependentOriginationsBetween(dayStartMillis, dayEndMillis)
        val actualAggregateToday = repository.countFiveAggregatesBetween(dayStartMillis, dayEndMillis)
        val observationToday = eventToday + actualOriginToday + actualAggregateToday
        val sevenDayMeditations = repository.countMeditationBetween(weekStartLabel, weekEndLabel)
        val hindranceCounts = repository.countHindrancesBetween(weekStartLabel, weekEndLabel)
        val strongestCover = hindranceCounts.firstOrNull()?.first
        val coverGuidance = when (strongestCover) {
            "昏沉" -> "近七日昏沉较常出现：可加强念、择法、精进、喜；坐中明显昏沉时可起身经行。"
            "掉举" -> "近七日掉举较常出现：可加强轻安、定、舍，减少继续追逐刺激。"
            "贪欲" -> "近七日贪欲较常出现：白天护根时重点观察乐受之后的‘再来一点’，看到受与爱之间的间隙。"
            "瞋恚" -> "近七日瞋恚较常出现：先辨认苦受与排斥冲动，再观察回应前身体和心的变化。"
            "疑" -> "近七日疑较常出现：回到已经学过的四圣谛、安般念与当前直接经验，避免在概念中反复打转。"
            else -> "暂时没有足够的五盖记录形成明显趋势；继续如实记录，不需要人为寻找问题。"
        }
        val nextAction = when {
            medToday == 0 -> "今天先做一座安般念。以自然呼吸为所缘，先把前四步练清楚。"
            rootToday == 0 -> "今天可以做一次护根：从一个真实接触开始，看触 → 受 → 爱 → 取。"
            observationToday == 0 -> "今天选一个明显的受或心状态，观察它的生起、变化与灭去。"
            walkingToday == 0 -> "若心昏沉或坐后需要转换，可以经行几分钟，再回到安般。"
            else -> "今天已有完整练习痕迹，继续保持稳定；不必为了数字强行增加时长。"
        }
        return PracticeSummary(
            meditationCount, totalMinutes, recentPractices, medToday, walkingToday, rootToday,
            observationToday, coverToday, preceptToday, sevenDayMeditations, walkingWeek,
            strongestCover, coverGuidance, nextAction
        )
    }
}

data class PracticeSummary(
    val meditationCount: Int = 0, val totalMinutes: Int = 0,
    val recentPractices: List<RecentPracticeSummary> = emptyList(),
    val todayMeditationCount: Int = 0, val todayWalkingCount: Int = 0, val todayRootCount: Int = 0,
    val todayObservationCount: Int = 0, val todayHindranceCount: Int = 0, val todayPreceptCount: Int = 0,
    val sevenDayMeditationCount: Int = 0, val sevenDayWalkingCount: Int = 0,
    val strongestHindrance: String? = null,
    val hindranceGuidance: String = "继续如实观察五盖。",
    val nextAction: String = "从安般念开始。"
)

data class RecentPracticeSummary(
    val key: String,
    val date: String,
    val title: String,
    val durationSeconds: Int?,
    val detail: String
)
