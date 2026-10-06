package com.ruru.practice.feature.home

import com.ruru.practice.domain.usecase.PracticeTotals
import com.ruru.practice.domain.usecase.RecentPracticeSummary

data class HomeDashboardState(
    val isLoading: Boolean = true,
    val totals: PracticeTotals = PracticeTotals(),
    val recentPractices: List<RecentPracticeSummary> = emptyList(),
    val todayMeditationCount: Int = 0,
    val todayWalkingCount: Int = 0,
    val todayRootCount: Int = 0,
    val todayObservationCount: Int = 0,
    val todayHindranceCount: Int = 0,
    val todayPreceptCount: Int = 0,
    val sevenDayPracticeCount: Int = 0,
    val sevenDayWalkingCount: Int = 0,
    val strongestHindrance: String? = null,
    val hindranceGuidance: String = "继续如实观察五盖。",
    val nextAction: String = "从安般念开始。",
    val errorMessage: String? = null
)
