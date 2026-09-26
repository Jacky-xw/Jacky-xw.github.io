package com.ruru.practice.data.repository

import com.ruru.practice.data.entity.*

interface PracticeRepository {
    suspend fun saveMeditation(session: MeditationSessionEntity): Long
    suspend fun getRecentMeditations(limit: Int = 10): List<MeditationSessionEntity>
    suspend fun getMeditationCount(): Int
    suspend fun getTotalMeditationSeconds(): Long
    suspend fun countMeditationForDate(date: String): Int
    suspend fun countMeditationBetween(start: String, end: String): Int
    suspend fun updateMeditationNotes(id: Long, observation: String, afterState: String)
    suspend fun deleteMeditations(ids: List<Long>)

    suspend fun saveMindfulnessEvent(entity: MindfulnessEventEntity)
    suspend fun getRecentMindfulnessEvents(limit: Int = 10): List<MindfulnessEventEntity>
    suspend fun countMindfulnessForDate(date: String): Int
    suspend fun deleteMindfulnessEvents(ids: List<Long>)

    suspend fun saveFiveAggregate(entity: FiveAggregateEntity)
    suspend fun getRecentFiveAggregates(limit: Int = 10): List<FiveAggregateEntity>
    suspend fun countFiveAggregatesBetween(start: Long, end: Long): Int
    suspend fun deleteFiveAggregates(ids: List<Long>)

    suspend fun saveReflection(entity: ReflectionEntity)
    suspend fun getRecentReflections(limit: Int = 50): List<ReflectionEntity>
    suspend fun deleteReflections(ids: List<Long>)

    suspend fun saveFiveCover(entity: FiveCoverEntity)
    suspend fun getRecentFiveCovers(limit: Int = 10): List<FiveCoverEntity>
    suspend fun countFiveCoversForDate(date: String): Int
    suspend fun countHindrancesBetween(start: String, end: String): List<Pair<String, Int>>
    suspend fun deleteFiveCovers(ids: List<Long>)

    suspend fun savePrecept(entity: PreceptEntity)
    suspend fun getRecentPrecepts(limit: Int = 10): List<PreceptEntity>
    suspend fun countPreceptsForDate(date: String): Int
    suspend fun deletePrecepts(ids: List<Long>)

    suspend fun saveDependentOrigination(entity: DependentOriginationEntity)
    suspend fun getRecentDependentOriginations(limit: Int = 10): List<DependentOriginationEntity>
    suspend fun countDependentOriginationsBetween(start: Long, end: Long): Int
    suspend fun deleteDependentOriginations(ids: List<Long>)

    suspend fun saveWalking(entity: WalkingSessionEntity)
    suspend fun getRecentWalking(limit: Int = 10): List<WalkingSessionEntity>
    suspend fun countWalkingForDate(date: String): Int
    suspend fun countWalkingBetween(start: String, end: String): Int
    suspend fun getTotalWalkingSeconds(): Long
    suspend fun getWalkingCount(): Int
    suspend fun deleteWalking(ids: List<Long>)

    suspend fun saveRootProtection(entity: RootProtectionEntity)
    suspend fun getRecentRootProtections(limit: Int = 10): List<RootProtectionEntity>
    suspend fun countRootProtectionForDate(date: String): Int
    suspend fun countRootProtectionBetween(start: String, end: String): Int
    suspend fun getRootProtectionCount(): Int
    suspend fun deleteRootProtections(ids: List<Long>)

    suspend fun saveEightPreceptSession(entity: EightPreceptSessionEntity)
    suspend fun getEightPreceptForDate(date: String): EightPreceptSessionEntity?
    suspend fun countEightPreceptsForDate(date: String): Int
    suspend fun getRecentEightPreceptSessions(limit: Int = 30): List<EightPreceptSessionEntity>
    suspend fun updateEightPreceptSession(id: Long, checkedMask: Int, note: String)
    suspend fun deleteEightPreceptSessions(ids: List<Long>)
}
