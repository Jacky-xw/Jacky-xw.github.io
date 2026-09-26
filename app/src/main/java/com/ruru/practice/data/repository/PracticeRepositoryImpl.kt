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
    override suspend fun updateMeditationNotes(id: Long, observation: String, afterState: String) = meditationDao.updateNotes(id, observation, afterState)
    override suspend fun deleteMeditations(ids: List<Long>) = meditationDao.deleteByIds(ids)

    override suspend fun saveMindfulnessEvent(entity: MindfulnessEventEntity) = mindfulnessEventDao.insert(entity)
    override suspend fun getRecentMindfulnessEvents(limit: Int) = mindfulnessEventDao.getRecent(limit)
    override suspend fun countMindfulnessForDate(date: String) = mindfulnessEventDao.countForDate(date)
    override suspend fun deleteMindfulnessEvents(ids: List<Long>) = mindfulnessEventDao.deleteByIds(ids)

    override suspend fun saveFiveAggregate(entity: FiveAggregateEntity) = fiveAggregateDao.insert(entity)
    override suspend fun getRecentFiveAggregates(limit: Int) = fiveAggregateDao.getRecent(limit)
    override suspend fun countFiveAggregatesBetween(start: Long, end: Long) = fiveAggregateDao.countBetween(start, end)
    override suspend fun deleteFiveAggregates(ids: List<Long>) = fiveAggregateDao.deleteByIds(ids)

    override suspend fun saveReflection(entity: ReflectionEntity) = reflectionDao.insert(entity)
    override suspend fun getRecentReflections(limit: Int) = reflectionDao.getRecent(limit)
    override suspend fun deleteReflections(ids: List<Long>) = reflectionDao.deleteByIds(ids)

    override suspend fun saveFiveCover(entity: FiveCoverEntity) = fiveCoverDao.insert(entity)
    override suspend fun getRecentFiveCovers(limit: Int) = fiveCoverDao.getRecent(limit)
    override suspend fun countFiveCoversForDate(date: String) = fiveCoverDao.countForDate(date)
    override suspend fun countHindrancesBetween(start: String, end: String) = fiveCoverDao.countTypesBetween(start, end).map { it.type to it.count }
    override suspend fun deleteFiveCovers(ids: List<Long>) = fiveCoverDao.deleteByIds(ids)

    override suspend fun savePrecept(entity: PreceptEntity) = preceptDao.insert(entity)
    override suspend fun getRecentPrecepts(limit: Int) = preceptDao.getRecent(limit)
    override suspend fun countPreceptsForDate(date: String) = preceptDao.countForDate(date)
    override suspend fun deletePrecepts(ids: List<Long>) = preceptDao.deleteByIds(ids)

    override suspend fun saveDependentOrigination(entity: DependentOriginationEntity) = dependentOriginationDao.insert(entity)
    override suspend fun getRecentDependentOriginations(limit: Int) = dependentOriginationDao.getRecent(limit)
    override suspend fun countDependentOriginationsBetween(start: Long, end: Long) = dependentOriginationDao.countBetween(start, end)
    override suspend fun deleteDependentOriginations(ids: List<Long>) = dependentOriginationDao.deleteByIds(ids)

    override suspend fun saveWalking(entity: WalkingSessionEntity) = walkingSessionDao.insert(entity)
    override suspend fun getRecentWalking(limit: Int) = walkingSessionDao.getRecent(limit)
    override suspend fun countWalkingForDate(date: String) = walkingSessionDao.countForDate(date)
    override suspend fun countWalkingBetween(start: String, end: String) = walkingSessionDao.countBetween(start, end)
    override suspend fun getTotalWalkingSeconds() = walkingSessionDao.getTotalSeconds() ?: 0L
    override suspend fun getWalkingCount() = walkingSessionDao.countAll()
    override suspend fun deleteWalking(ids: List<Long>) = walkingSessionDao.deleteByIds(ids)

    override suspend fun saveRootProtection(entity: RootProtectionEntity) = rootProtectionDao.insert(entity)
    override suspend fun getRecentRootProtections(limit: Int) = rootProtectionDao.getRecent(limit)
    override suspend fun countRootProtectionForDate(date: String) = rootProtectionDao.countForDate(date)
    override suspend fun countRootProtectionBetween(start: String, end: String) = rootProtectionDao.countBetween(start, end)
    override suspend fun getRootProtectionCount() = rootProtectionDao.countAll()
    override suspend fun deleteRootProtections(ids: List<Long>) = rootProtectionDao.deleteByIds(ids)

    override suspend fun saveEightPreceptSession(entity: EightPreceptSessionEntity) = eightPreceptSessionDao.insert(entity)
    override suspend fun getEightPreceptForDate(date: String) = eightPreceptSessionDao.getLatestForDate(date)
    override suspend fun countEightPreceptsForDate(date: String) = eightPreceptSessionDao.countForDate(date)
    override suspend fun getRecentEightPreceptSessions(limit: Int) = eightPreceptSessionDao.getRecent(limit)
    override suspend fun updateEightPreceptSession(id: Long, checkedMask: Int, note: String) = eightPreceptSessionDao.updateForId(id, checkedMask, note)
    override suspend fun deleteEightPreceptSessions(ids: List<Long>) = eightPreceptSessionDao.deleteByIds(ids)
}
