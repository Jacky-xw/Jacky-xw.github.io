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
}
