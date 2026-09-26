package com.ruru.practice.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName="daily_practice")
data class DailyPracticeEntity(
    @PrimaryKey(autoGenerate=true) val id:Long=0,
    val date:String,
    val focus:String,
    val note:String
)
