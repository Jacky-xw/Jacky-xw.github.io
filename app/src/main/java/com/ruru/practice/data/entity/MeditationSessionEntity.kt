package com.ruru.practice.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName="meditation_session", indices = [Index(value = ["date"])])
data class MeditationSessionEntity(
    @PrimaryKey(autoGenerate=true)
    val id:Long=0,
    val date:String,
    val durationSeconds:Int,
    val observation:String,
    val afterState:String,
    val practiceStep:Int = 1,
    val stepTitle:String = "入息、出息"
)
