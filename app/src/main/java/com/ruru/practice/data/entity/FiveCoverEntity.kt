package com.ruru.practice.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "five_cover")
data class FiveCoverEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val type: String,
    val intensity: Int,
    val trigger: String,
    val response: String,
    val observation: String
)
