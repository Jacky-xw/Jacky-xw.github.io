package com.ruru.practice.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "dependent_origination", indices = [Index(value = ["createdAt"])])
data class DependentOriginationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val trigger: String,
    val feeling: String,
    val craving: String,
    val grasping: String,
    val reflection: String,
    val createdAt: Long = System.currentTimeMillis()
)
