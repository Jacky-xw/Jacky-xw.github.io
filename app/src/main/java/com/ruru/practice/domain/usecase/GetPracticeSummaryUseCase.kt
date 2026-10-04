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
        val dateOnly = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val today = dateOnly.format(now)
        val calendar = Calendar.getInstance().apply {
            time = now
            // Keep the weekly boundary independent of the device locale. The
            // retention pass uses the same Monday calculation.
            val daysSinceMonday = (get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
            add(Calendar.DAY_OF_YEAR, -daysSinceMonday)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val weekStart = calendar.time
        val weekStartLabel = dateOnly.format(weekStart)
        val weekEnd = (calendar.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 7) }
        val weekEndLabel = dateOnly.format(weekEnd.time)

        // Home totals are the current Monday-Sunday cycle, matching the weekly
        // retention model and the "本周" labels shown to the user.
        val meditationWeek = repository.countMeditationBetween(weekStartLabel, weekEndLabel)
        val walkingWeekTotal = repository.countWalkingBetween(weekStartLabel, weekEndLabel)
        val rootWeekTotal = repository.countRootProtectionBetween(weekStartLabel, weekEndLabel)
        val totals = PracticeTotals(
            count = meditationWeek + walkingWeekTotal + rootWeekTotal,
            durationSeconds = repository.sumMeditationSecondsBetween(weekStartLabel, weekEndLabel) +
                repository.sumWalkingSecondsBetween(weekStartLabel, weekEndLabel)
        )
        // The home timeline is not a "latest N" list. Fetch every row from each
        // source, then merge and sort it so one practice type cannot hide older
        // entries from another type. The DAO queries still use a LIMIT parameter;
        // Int.MAX_VALUE is SQLite's practical unlimited value for these queries.
        val recentPracticeLimit = Int.MAX_VALUE
        val recentPractices = buildList {
            repository.getRecentMeditations(recentPracticeLimit).forEach { session ->
                add(
                    RecentPracticeSummary(
                        key = "meditation-${session.id}",
                        recordType = PracticeRecordType.MEDITATION,
                        sourceId = session.id,
                        date = session.date,
                        title = "安般念 · 第${session.practiceStep}步",
                        durationSeconds = session.durationSeconds,
                        detail = noteDetail(
                            "本步练习中的观察（可选）" to session.observation,
                            "练习后的身心状态（可选）" to session.afterState
                        )
                    )
                )
            }
            repository.getRecentWalking(recentPracticeLimit).forEach { session ->
                add(
                    RecentPracticeSummary(
                        key = "walking-${session.id}",
                        recordType = PracticeRecordType.WALKING,
                        sourceId = session.id,
                        date = session.date,
                        title = "经行 · ${session.method}",
                        durationSeconds = session.durationSeconds,
                        detail = noteDetail("经行后的观察" to session.observation)
                    )
                )
            }
            repository.getRecentRootProtections(recentPracticeLimit).forEach { session ->
                add(
                    RecentPracticeSummary(
                        key = "root-${session.id}",
                        recordType = PracticeRecordType.ROOT_PROTECTION,
                        sourceId = session.id,
                        date = session.date,
                        title = "护根 · ${session.sense}",
                        durationSeconds = null,
                        detail = noteDetail(
                            "接触" to session.contact,
                            "受" to session.feeling,
                            "爱" to session.craving,
                            "取" to session.grasping,
                            "回应" to session.response
                        )
                    )
                )
            }
            repository.getRecentFiveCovers(recentPracticeLimit).forEach { record ->
                add(
                    RecentPracticeSummary(
                        key = "five-cover-${record.id}",
                        recordType = PracticeRecordType.FIVE_COVER,
                        sourceId = record.id,
                        date = record.date,
                        title = "五盖 · ${record.type}",
                        durationSeconds = null,
                        detail = noteDetail(
                            "强度" to record.intensity.toString(),
                            "触发条件" to record.trigger,
                            "我的回应" to record.response,
                            "回看" to record.observation
                        )
                    )
                )
            }
            repository.getRecentPrecepts(recentPracticeLimit).forEach { record ->
                add(
                    RecentPracticeSummary(
                        key = "precept-${record.id}",
                        recordType = PracticeRecordType.PRECEPT,
                        sourceId = record.id,
                        date = record.date,
                        title = "戒行 · ${record.type}",
                        durationSeconds = null,
                        detail = noteDetail(
                            "发生的事情" to record.event,
                            "回看与下一步" to record.reflection
                        )
                    )
                )
            }
            repository.getRecentMindfulnessEvents(recentPracticeLimit).forEach { record ->
                add(
                    RecentPracticeSummary(
                        key = "mindfulness-${record.id}",
                        recordType = PracticeRecordType.MINDFULNESS_EVENT,
                        sourceId = record.id,
                        date = record.date,
                        title = "观察 · ${record.scene}",
                        durationSeconds = null,
                        detail = noteDetail(
                            "事件经过" to record.description,
                            "身体与感受" to record.feeling,
                            "即时反应" to record.reaction,
                            "回看后的觉察" to record.awareness
                        )
                    )
                )
            }
            repository.getRecentFiveAggregates(recentPracticeLimit).forEach { record ->
                add(
                    RecentPracticeSummary(
                        key = "five-aggregate-${record.id}",
                        recordType = PracticeRecordType.FIVE_AGGREGATE,
                        sourceId = record.id,
                        date = format.format(Date(record.createdAt)),
                        title = "五蕴观察 · ${record.event}",
                        durationSeconds = null,
                        detail = noteDetail(
                            "色 · 身体" to record.rupa,
                            "受 · 感受" to record.vedana,
                            "想 · 识别" to record.sanna,
                            "行 · 反应" to record.sankhara,
                            "识 · 知道" to record.vinnana,
                            "回看" to record.reflection
                        )
                    )
                )
            }
            repository.getRecentDependentOriginations(recentPracticeLimit).forEach { record ->
                add(
                    RecentPracticeSummary(
                        key = "dependent-origination-${record.id}",
                        recordType = PracticeRecordType.DEPENDENT_ORIGINATION,
                        sourceId = record.id,
                        date = format.format(Date(record.createdAt)),
                        title = "缘起观察 · ${record.trigger}",
                        durationSeconds = null,
                        detail = noteDetail(
                            "受 · 感受" to record.feeling,
                            "爱 · 想要" to record.craving,
                            "取 · 抓住" to record.grasping,
                            "回看" to record.reflection
                        )
                    )
                )
            }
            repository.getRecentEightPreceptSessions(recentPracticeLimit).forEach { record ->
                add(
                    RecentPracticeSummary(
                        key = "eight-precept-${record.id}",
                        recordType = PracticeRecordType.EIGHT_PRECEPT,
                        sourceId = record.id,
                        date = record.date,
                        title = "八戒自检",
                        durationSeconds = null,
                        detail = noteDetail("今日自检备注" to record.note)
                    )
                )
            }
            repository.getRecentReflections(recentPracticeLimit).forEach { record ->
                add(
                    RecentPracticeSummary(
                        key = "reflection-${record.id}",
                        recordType = PracticeRecordType.REFLECTION,
                        sourceId = record.id,
                        date = format.format(Date(record.createdAt)),
                        title = record.linkedPractice?.let { "复盘 · $it" } ?: "修行复盘",
                        durationSeconds = null,
                        detail = noteDetail("今天想记下什么？" to record.content)
                    )
                )
            }
            repository.getRecentDailyPractices(recentPracticeLimit).forEach { record ->
                add(
                    RecentPracticeSummary(
                        key = "daily-practice-${record.id}",
                        recordType = PracticeRecordType.DAILY_PRACTICE,
                        sourceId = record.id,
                        date = record.date,
                        title = record.focus.takeIf { it.isNotBlank() }?.let { "每日修行 · $it" } ?: "每日修行",
                        durationSeconds = null,
                        detail = noteDetail("笔记" to record.note)
                    )
                )
            }
        }.sortedByDescending { it.date }
        val medToday = repository.countMeditationForDate(today)
        val walkingToday = repository.countWalkingForDate(today)
        val walkingWeek = repository.countWalkingBetween(weekStartLabel, weekEndLabel)
        val rootToday = repository.countRootProtectionForDate(today)
        val rootWeek = repository.countRootProtectionBetween(weekStartLabel, weekEndLabel)
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
        val sevenDayPracticeCount = sevenDayMeditations + walkingWeek + rootWeek
        val hindranceCounts = repository.countHindrancesBetween(weekStartLabel, weekEndLabel)
        val strongestCover = hindranceCounts.firstOrNull()?.first
        val coverGuidance = when (strongestCover) {
            "昏沉" -> "本周昏沉较常出现：可加强念、择法、精进、喜；坐中明显昏沉时可起身经行。"
            "掉举" -> "本周掉举较常出现：可加强轻安、定、舍，减少继续追逐刺激。"
            "贪欲" -> "本周贪欲较常出现：白天护根时重点观察乐受之后的‘再来一点’，看到受与爱之间的间隙。"
            "瞋恚" -> "本周瞋恚较常出现：先辨认苦受与排斥冲动，再观察回应前身体和心的变化。"
            "疑" -> "本周疑较常出现：回到已经学过的四圣谛、安般念与当前直接经验，避免在概念中反复打转。"
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
            totals = totals,
            recentPractices = recentPractices,
            todayMeditationCount = medToday,
            todayWalkingCount = walkingToday,
            todayRootCount = rootToday,
            todayObservationCount = observationToday,
            todayHindranceCount = coverToday,
            todayPreceptCount = preceptToday,
            sevenDayPracticeCount = sevenDayPracticeCount,
            sevenDayWalkingCount = walkingWeek,
            strongestHindrance = strongestCover,
            hindranceGuidance = coverGuidance,
            nextAction = nextAction
        )
    }
}


private fun noteDetail(vararg fields: Pair<String, String>): String = fields
    .filter { (_, value) -> value.isNotBlank() }
    .joinToString("\n") { (label, value) -> "$label：\n${value.trim()}" }

data class PracticeSummary(
    val totals: PracticeTotals = PracticeTotals(),
    val recentPractices: List<RecentPracticeSummary> = emptyList(),
    val todayMeditationCount: Int = 0, val todayWalkingCount: Int = 0, val todayRootCount: Int = 0,
    val todayObservationCount: Int = 0, val todayHindranceCount: Int = 0, val todayPreceptCount: Int = 0,
    val sevenDayPracticeCount: Int = 0, val sevenDayWalkingCount: Int = 0,
    val strongestHindrance: String? = null,
    val hindranceGuidance: String = "继续如实观察五盖。",
    val nextAction: String = "从安般念开始。"
)

data class PracticeTotals(
    val count: Int = 0,
    val durationSeconds: Long = 0L
)

enum class PracticeRecordType {
    MEDITATION,
    WALKING,
    ROOT_PROTECTION,
    FIVE_COVER,
    PRECEPT,
    MINDFULNESS_EVENT,
    FIVE_AGGREGATE,
    DEPENDENT_ORIGINATION,
    EIGHT_PRECEPT,
    REFLECTION,
    DAILY_PRACTICE
}

data class RecentPracticeSummary(
    val key: String,
    val recordType: PracticeRecordType,
    val sourceId: Long,
    val date: String,
    val title: String,
    val durationSeconds: Int?,
    val detail: String
)
