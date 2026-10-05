package com.ruru.practice.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "weekly_report")
data class WeeklyReportEntity(
    @PrimaryKey val id: Int = 1,
    val weekStart: String,
    val weekEnd: String,
    val content: String,
    val generatedAt: Long = System.currentTimeMillis()
)
