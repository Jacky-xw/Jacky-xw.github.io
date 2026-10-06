package com.ruru.practice.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ruru.practice.data.entity.WeeklyReportEntity

@Dao
interface WeeklyReportDao {
    @Query("SELECT * FROM weekly_report WHERE id = 1")
    suspend fun getCurrent(): WeeklyReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun replace(report: WeeklyReportEntity)

    @Query("DELETE FROM weekly_report")
    suspend fun clear()
}
