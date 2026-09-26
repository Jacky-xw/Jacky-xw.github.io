package com.ruru.practice.data.repository

import com.ruru.practice.data.dao.*
import com.ruru.practice.data.entity.*
import javax.inject.Inject

class PracticeRepositoryImpl @Inject constructor(
    private val meditationDao: MeditationDao,
    private val mindfulnessEventDao: MindfulnessEventDao,
    private val fiveAggregateDao: FiveAggregateDao,
    private val reflectionDao: ReflectionDao,
    private val fiveCoverDao: FiveCoverDao,
    private val preceptDao: PreceptDao,
    private val dependentOriginationDao: DependentOriginationDao,
    private val walkingSessionDao: WalkingSessionDao,
    private val rootProtectionDao: RootProtectionDao,
    private val eightPreceptSessionDao: EightPreceptSessionDao
) : PracticeRepository {
    override suspend fun saveMeditation(session: MeditationSessionEntity) = meditationDao.insert(session)
    override suspend fun getRecentMeditations(limit: Int) = meditationDao.getRecent(limit)
    override suspend fun getMeditationCount() = meditationDao.getCount()
    override suspend fun getTotalMeditationSeconds() = meditationDao.getTotalSeconds() ?: 0L
    override suspend fun countMeditationForDate(date: String) = meditationDao.countForDate(date)
    override suspend fun countMeditationBetween(start: String, end: String) = meditationDao.countBetween(start, end)

    override suspend fun saveMindfulnessEvent(entity: MindfulnessEventEntity) = mindfulnessEventDao.insert(entity)
    override suspend fun getRecentMindfulnessEvents(limit: Int) = mindfulnessEventDao.getRecent(limit)
    override suspend fun countMindfulnessForDate(date: String) = mindfulnessEventDao.countForDate(date)

    override suspend fun saveFiveAggregate(entity: FiveAggregateEntity) = fiveAggregateDao.insert(entity)
    override suspend fun getRecentFiveAggregates(limit: Int) = fiveAggregateDao.getRecent(limit)
    override suspend fun countFiveAggregatesBetween(start: Long, end: Long) = fiveAggregateDao.countBetween(start, end)

    override suspend fun saveReflection(entity: ReflectionEntity) = reflectionDao.insert(entity)
    override suspend fun getRecentReflections(limit: Int) = reflectionDao.getRecent(limit)

    override suspend fun saveFiveCover(entity: FiveCoverEntity) = fiveCoverDao.insert(entity)
    override suspend fun getRecentFiveCovers(limit: Int) = fiveCoverDao.getRecent(limit)
    override suspend fun countFiveCoversForDate(date: String) = fiveCoverDao.countForDate(date)
    override suspend fun countHindrancesBetween(start: String, end: String) = fiveCoverDao.countTypesBetween(start, end).map { it.type to it.count }

    override suspend fun savePrecept(entity: PreceptEntity) = preceptDao.insert(entity)
    override suspend fun getRecentPrecepts(limit: Int) = preceptDao.getRecent(limit)
    override suspend fun countPreceptsForDate(date: String) = preceptDao.countForDate(date)

    override suspend fun saveDependentOrigination(entity: DependentOriginationEntity) = dependentOriginationDao.insert(entity)
    override suspend fun getRecentDependentOriginations(limit: Int) = dependentOriginationDao.getRecent(limit)
    override suspend fun countDependentOriginationsBetween(start: Long, end: Long) = dependentOriginationDao.countBetween(start, end)

    override suspend fun saveWalking(entity: WalkingSessionEntity) = walkingSessionDao.insert(entity)
    override suspend fun getRecentWalking(limit: Int) = walkingSessionDao.getRecent(limit)
    override suspend fun countWalkingForDate(date: String) = walkingSessionDao.countForDate(date)
    override suspend fun countWalkingBetween(start: String, end: String) = walkingSessionDao.countBetween(start, end)

    override suspend fun saveRootProtection(entity: RootProtectionEntity) = rootProtectionDao.insert(entity)
    override suspend fun getRecentRootProtections(limit: Int) = rootProtectionDao.getRecent(limit)
    override suspend fun countRootProtectionForDate(date: String) = rootProtectionDao.countForDate(date)

    override suspend fun saveEightPreceptSession(entity: EightPreceptSessionEntity) = eightPreceptSessionDao.insert(entity)
    override suspend fun getEightPreceptForDate(date: String) = eightPreceptSessionDao.getLatestForDate(date)
    override suspend fun countEightPreceptsForDate(date: String) = eightPreceptSessionDao.countForDate(date)
}
