package com.ruru.practice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ruru.practice.data.entity.ReflectionEntity

@Dao
interface ReflectionDao {
    @Insert
    suspend fun insert(entity: ReflectionEntity)

    @Query("SELECT * FROM reflection ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<ReflectionEntity>

    @Query("DELETE FROM reflection WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
