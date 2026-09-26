package com.ruru.practice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ruru.practice.data.entity.PreceptEntity

@Dao
interface PreceptDao {
    @Insert
    suspend fun insert(entity: PreceptEntity)

    @Query("SELECT * FROM precept ORDER BY id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 10): List<PreceptEntity>

    @Query("SELECT COUNT(*) FROM precept WHERE date LIKE :datePrefix || '%'")
    suspend fun countForDate(datePrefix: String): Int
}
