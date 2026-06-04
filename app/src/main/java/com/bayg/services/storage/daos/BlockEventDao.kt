package com.bayg.services.storage.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.bayg.services.storage.entities.BlockEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockEventDao {
    @Query("SELECT * FROM block_events WHERE userId = :userId ORDER BY triggeredAt DESC")
    fun getAllBlockEvents(userId: String): Flow<List<BlockEvent>>

    @Query("""
        SELECT * FROM block_events
        WHERE userId = :userId
          AND triggeredAt BETWEEN :fromMs AND :toMs
        ORDER BY triggeredAt DESC
    """)
    suspend fun getBlockEventsInRange(userId: String, fromMs: Long, toMs: Long): List<BlockEvent>

    @Insert
    suspend fun insert(event: BlockEvent): Long

    @Query("SELECT COUNT(*) FROM block_events WHERE userId = :userId")
    suspend fun countAll(userId: String): Int
}