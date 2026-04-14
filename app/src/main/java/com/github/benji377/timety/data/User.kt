package com.github.benji377.timety.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val xp: Int = 0,
    val level: Int = 1,
    val currentStreak: Int = 0,
    val highestStreak: Int = 0,
    val dailyFocusTarget: Long, // Target focus time in milliseconds
    val lastActiveDate: Long // Unix epoch milliseconds
)
