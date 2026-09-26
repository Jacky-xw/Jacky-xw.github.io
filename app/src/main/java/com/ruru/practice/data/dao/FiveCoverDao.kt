package com.ruru.practice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ruru.practice.data.entity.FiveCoverEntity

data class HindranceCount(val type: String, val count: Int)

@Dao
interface FiveCoverDao {
    @Insert
    suspend fun insert(entity: FiveCoverEntity)

    @Query("SELECT * FROM five_cover ORDER BY id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 10): List<FiveCoverEntity>

    @Query("SELECT COUNT(*) FROM five_cover WHERE date LIKE :datePrefix || '%'")
    suspend fun countForDate(datePrefix: String): Int

    @Query("SELECT type, COUNT(*) AS count FROM five_cover WHERE date >= :start AND date <= :end GROUP BY type ORDER BY count DESC")
    suspend fun countTypesBetween(start: String, end: String): List<HindranceCount>

    @Query("DELETE FROM five_cover WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
