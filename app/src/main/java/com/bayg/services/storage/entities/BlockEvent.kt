package com.bayg.services.storage.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "block_events",
    indices = [
        Index("userId"),
        Index("triggeredAt"),
        Index("firebaseId", unique = true)
    ]
)
data class BlockEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firebaseId: String? = null,
    val syncedAt: Long? = null,
    val userId: String,
    val triggeredAt: Long = System.currentTimeMillis(),
    val blockDurationMinutes: Int,
    val label: String = "Daily limit exceeded",
    val severity: BlockEventSeverity = BlockEventSeverity.RED,
    val detail: String? = null,
)