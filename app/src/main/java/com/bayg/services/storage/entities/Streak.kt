package com.bayg.services.storage.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "streaks",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId", unique = true)]
)
data class Streak(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firebaseId: String = "",
    val userId: String,
    val currentStreak: Int = 0,
    val lastStreakDate: Long? = null
)