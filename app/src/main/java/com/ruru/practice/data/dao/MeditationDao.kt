package com.ruru.practice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ruru.practice.data.entity.MeditationSessionEntity

@Dao
interface MeditationDao {
    @Insert
    suspend fun insert(item: MeditationSessionEntity): Long

    @Query("SELECT * FROM meditation_session ORDER BY id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 10): List<MeditationSessionEntity>

    @Query("SELECT SUM(durationSeconds) FROM meditation_session")
    suspend fun getTotalSeconds(): Long?

    @Query("SELECT COUNT(*) FROM meditation_session")
    suspend fun getCount(): Int

    @Query("SELECT * FROM meditation_session ORDER BY id DESC LIMIT 1")
    suspend fun getLatest(): MeditationSessionEntity?

    @Query("SELECT COUNT(*) FROM meditation_session WHERE date LIKE :datePrefix || '%'")
    suspend fun countForDate(datePrefix: String): Int

    @Query("SELECT COUNT(*) FROM meditation_session WHERE date >= :start AND date <= :end")
    suspend fun countBetween(start: String, end: String): Int

    @Query("UPDATE meditation_session SET observation = :observation, afterState = :afterState WHERE id = :id")
    suspend fun updateNotes(id: Long, observation: String, afterState: String)

    @Query("DELETE FROM meditation_session WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
