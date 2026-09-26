package com.ruru.practice.feature.home

import com.ruru.practice.data.entity.MeditationSessionEntity

data class HomeDashboardState(
    val isLoading: Boolean = true,
    val meditationCount: Int = 0,
    val totalMinutes: Int = 0,
    val recentSessions: List<MeditationSessionEntity> = emptyList(),
    val todayMeditationCount: Int = 0,
    val todayWalkingCount: Int = 0,
    val todayRootCount: Int = 0,
    val todayObservationCount: Int = 0,
    val todayHindranceCount: Int = 0,
    val todayPreceptCount: Int = 0,
    val todayEightPreceptCount: Int = 0,
    val sevenDayMeditationCount: Int = 0,
    val sevenDayWalkingCount: Int = 0,
    val strongestHindrance: String? = null,
    val hindranceGuidance: String = "继续如实观察五盖。",
    val nextAction: String = "从安般念开始。",
    val errorMessage: String? = null
)
