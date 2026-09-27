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

    @Query("SELECT * FROM eight_precept_session ORDER BY date DESC, id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 30): List<EightPreceptSessionEntity>

    @Query("UPDATE eight_precept_session SET checkedMask = :checkedMask, note = :note WHERE id = :id")
    suspend fun updateForId(id: Long, checkedMask: Int, note: String)

    @Query("DELETE FROM eight_precept_session WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
}
