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
    suspend fun getRecent(limit: Int = 10): List<ReflectionEntity>
}
