package com.bayg.services.storage.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.bayg.services.storage.entities.User

/**
 * Only stored locally with Room
 * userId references the User table's primary key (Room ID)
 */
@Entity(
    tableName = "user_settings",
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
data class UserSettings(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,  // Foreign key to User.id (Room ID)

    val dailyLimitMinutes: Int = 45,
    val blockDurationMinutes: Int = 30,

    val touchGrassModeEnabled: Boolean = true,
    val locationEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true
)