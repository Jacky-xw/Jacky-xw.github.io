package com.ruru.practice.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "eight_precept_session", indices = [Index(value = ["date"])])
data class EightPreceptSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val checkedMask: Int,
    val note: String
)
