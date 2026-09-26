package com.ruru.practice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ruru.practice.data.entity.FiveAggregateEntity

@Dao
interface FiveAggregateDao {
    @Insert
    suspend fun insert(entity: FiveAggregateEntity)

    @Query("SELECT * FROM five_aggregate ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 10): List<FiveAggregateEntity>

    @Query("SELECT COUNT(*) FROM five_aggregate WHERE createdAt >= :start AND createdAt < :end")
    suspend fun countBetween(start: Long, end: Long): Int
}
