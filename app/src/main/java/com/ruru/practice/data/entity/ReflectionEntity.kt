package com.ruru.practice.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reflection")
data class ReflectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
