package com.ruru.practice.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "precept", indices = [Index(value = ["date"])])
data class PreceptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String,
    val type: String,
    val event: String,
    val reflection: String
)
