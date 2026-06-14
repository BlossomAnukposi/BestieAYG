package com.bayg.services.storage.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * One row per user per calendar day, storing total Instagram usage
 * for that day plus how many blocks were triggered.
 *
 * `date` is stored as "yyyy-MM-dd" (local time) so it sorts and
 * groups correctly without needing a date library.
 */
@Entity(
    tableName = "daily_usage",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index(value = ["userId", "date"], unique = true)]
)
data class DailyUsage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firebaseId: String = "",
    val syncedAt: Long? = null,
    val userId: Long,
    val date: String, // "yyyy-MM-dd"
    val usageMinutes: Int = 0,
    val blockCount: Int = 0
)
