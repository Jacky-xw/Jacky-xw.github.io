package com.ruru.practice.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "root_protection")
data class RootProtectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val sense: String,
    val contact: String,
    val feeling: String,
    val craving: String,
    val grasping: String,
    val response: String
)
