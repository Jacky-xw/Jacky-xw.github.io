package com.ruru.practice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ruru.practice.data.entity.MindfulnessEventEntity

@Dao
interface MindfulnessEventDao {
    @Insert
    suspend fun insert(entity: MindfulnessEventEntity)

    @Query("SELECT * FROM mindfulness_event ORDER BY id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 10): List<MindfulnessEventEntity>

    @Query("SELECT COUNT(*) FROM mindfulness_event WHERE date LIKE :datePrefix || '%'")
    suspend fun countForDate(datePrefix: String): Int
    @Query("DELETE FROM mindfulness_event WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
