package com.ruru.practice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ruru.practice.data.entity.RootProtectionEntity

@Dao
interface RootProtectionDao {
    @Insert suspend fun insert(entity: RootProtectionEntity)
    @Query("SELECT * FROM root_protection ORDER BY id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 10): List<RootProtectionEntity>
    @Query("SELECT COUNT(*) FROM root_protection WHERE date LIKE :datePrefix || '%'")
    suspend fun countForDate(datePrefix: String): Int
}
