package com.bayg.services.storage.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "block_events",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("triggeredAt")]
)
data class BlockEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firebaseId: String = "",
    val userId: String,
    val triggeredAt: Long = System.currentTimeMillis(),
    val blockDurationMinutes: Int,
)