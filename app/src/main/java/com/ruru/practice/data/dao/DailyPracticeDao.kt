package com.ruru.practice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ruru.practice.data.entity.DailyPracticeEntity

@Dao
interface DailyPracticeDao {
    @Insert
    suspend fun insert(entity: DailyPracticeEntity)

    @Query("SELECT * FROM daily_practice ORDER BY id DESC")
    suspend fun getAll(): List<DailyPracticeEntity>

    @Query("SELECT * FROM daily_practice ORDER BY date DESC, id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<DailyPracticeEntity>

    @Query("DELETE FROM daily_practice WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT COUNT(*) FROM daily_practice WHERE date >= :start AND date < :end")
    suspend fun countBetween(start: String, end: String): Int

    @Query("DELETE FROM daily_practice WHERE date < :cutoff")
    suspend fun deleteBefore(cutoff: String)
}
