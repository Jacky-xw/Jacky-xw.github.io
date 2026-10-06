package com.ruru.practice.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "walking_session", indices = [Index(value = ["date"])])
data class WalkingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val durationSeconds: Int,
    val method: String,
    val observation: String
)
