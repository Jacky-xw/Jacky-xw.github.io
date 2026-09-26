package com.ruru.practice.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "five_aggregate")
data class FiveAggregateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val event: String,
    val rupa: String,
    val vedana: String,
    val sanna: String,
    val sankhara: String,
    val vinnana: String,
    val reflection: String,
    val createdAt: Long = System.currentTimeMillis()
)
