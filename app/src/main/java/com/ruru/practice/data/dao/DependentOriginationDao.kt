package com.ruru.practice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ruru.practice.data.entity.DependentOriginationEntity

@Dao
interface DependentOriginationDao {
    @Insert
    suspend fun insert(entity: DependentOriginationEntity)

    @Query("SELECT * FROM dependent_origination ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 10): List<DependentOriginationEntity>

    @Query("SELECT COUNT(*) FROM dependent_origination WHERE createdAt >= :start AND createdAt < :end")
    suspend fun countBetween(start: Long, end: Long): Int
}
