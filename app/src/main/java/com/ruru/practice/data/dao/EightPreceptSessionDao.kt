package com.ruru.practice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ruru.practice.data.entity.EightPreceptSessionEntity

@Dao
interface EightPreceptSessionDao {
    @Insert
    suspend fun insert(entity: EightPreceptSessionEntity)

    @Query("SELECT * FROM eight_precept_session WHERE date = :date ORDER BY id DESC LIMIT 1")
    suspend fun getLatestForDate(date: String): EightPreceptSessionEntity?

    @Query("SELECT COUNT(*) FROM eight_precept_session WHERE date = :date")
    suspend fun countForDate(date: String): Int
}
