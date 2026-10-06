package com.ruru.practice.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "mindfulness_event", indices = [Index(value = ["date"])])
data class MindfulnessEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val scene: String,
    val description: String,
    val feeling: String,
    val reaction: String,
    val awareness: String
)
