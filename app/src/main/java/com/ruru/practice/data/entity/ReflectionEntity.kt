package com.ruru.practice.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reflection")
data class ReflectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    /** A snapshot label of the practice this review was written after. */
    val linkedPractice: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
