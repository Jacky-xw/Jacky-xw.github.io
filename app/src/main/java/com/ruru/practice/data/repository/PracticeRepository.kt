package com.ruru.practice.data.repository

import com.ruru.practice.data.entity.*

interface PracticeRepository {
    suspend fun saveMeditation(session: MeditationSessionEntity)
    suspend fun getRecentMeditations(limit: Int = 10): List<MeditationSessionEntity>
    suspend fun getMeditationCount(): Int
    suspend fun getTotalMeditationSeconds(): Long
    suspend fun countMeditationForDate(date: String): Int
    suspend fun countMeditationBetween(start: String, end: String): Int

    suspend fun saveMindfulnessEvent(entity: MindfulnessEventEntity)
    suspend fun getRecentMindfulnessEvents(limit: Int = 10): List<MindfulnessEventEntity>
    suspend fun countMindfulnessForDate(date: String): Int

    suspend fun saveFiveAggregate(entity: FiveAggregateEntity)
    suspend fun getRecentFiveAggregates(limit: Int = 10): List<FiveAggregateEntity>
    suspend fun countFiveAggregatesBetween(start: Long, end: Long): Int

    suspend fun saveReflection(entity: ReflectionEntity)
    suspend fun getRecentReflections(limit: Int = 10): List<ReflectionEntity>

    suspend fun saveFiveCover(entity: FiveCoverEntity)
    suspend fun getRecentFiveCovers(limit: Int = 10): List<FiveCoverEntity>
    suspend fun countFiveCoversForDate(date: String): Int
    suspend fun countHindrancesBetween(start: String, end: String): List<Pair<String, Int>>

    suspend fun savePrecept(entity: PreceptEntity)
    suspend fun getRecentPrecepts(limit: Int = 10): List<PreceptEntity>
    suspend fun countPreceptsForDate(date: String): Int

    suspend fun saveDependentOrigination(entity: DependentOriginationEntity)
    suspend fun getRecentDependentOriginations(limit: Int = 10): List<DependentOriginationEntity>
    suspend fun countDependentOriginationsBetween(start: Long, end: Long): Int

    suspend fun saveWalking(entity: WalkingSessionEntity)
    suspend fun getRecentWalking(limit: Int = 10): List<WalkingSessionEntity>
    suspend fun countWalkingForDate(date: String): Int
    suspend fun countWalkingBetween(start: String, end: String): Int

    suspend fun saveRootProtection(entity: RootProtectionEntity)
    suspend fun getRecentRootProtections(limit: Int = 10): List<RootProtectionEntity>
    suspend fun countRootProtectionForDate(date: String): Int

    suspend fun saveEightPreceptSession(entity: EightPreceptSessionEntity)
    suspend fun getEightPreceptForDate(date: String): EightPreceptSessionEntity?
    suspend fun countEightPreceptsForDate(date: String): Int
}
