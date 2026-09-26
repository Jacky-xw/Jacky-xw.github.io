package com.ruru.practice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ruru.practice.data.entity.WalkingSessionEntity

@Dao
interface WalkingSessionDao {
    @Insert suspend fun insert(entity: WalkingSessionEntity)
    @Query("SELECT * FROM walking_session ORDER BY id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 10): List<WalkingSessionEntity>
    @Query("SELECT COUNT(*) FROM walking_session WHERE date LIKE :datePrefix || '%'")
    suspend fun countForDate(datePrefix: String): Int
    @Query("SELECT COUNT(*) FROM walking_session WHERE date >= :start AND date <= :end")
    suspend fun countBetween(start: String, end: String): Int
    @Query("SELECT SUM(durationSeconds) FROM walking_session")
    suspend fun getTotalSeconds(): Long?

    @Query("DELETE FROM walking_session WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
    @Query("SELECT COUNT(*) FROM walking_session")
    suspend fun countAll(): Int
}
