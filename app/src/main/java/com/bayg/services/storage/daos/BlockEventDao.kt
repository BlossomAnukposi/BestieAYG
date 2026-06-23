package com.bayg.services.storage.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.bayg.services.storage.entities.BlockEvent

@Dao
interface BlockEventDao {
    @Query("SELECT * FROM block_events WHERE userId = :userId ORDER BY triggeredAt DESC")
    fun getAllBlockEvents(userId: String): List<BlockEvent>

    @Query("""
        SELECT * FROM block_events
        WHERE userId = :userId
          AND triggeredAt BETWEEN :fromMs AND :toMs
        ORDER BY triggeredAt DESC
    """)
    suspend fun getBlockEventsInRange(userId: String, fromMs: Long, toMs: Long): List<BlockEvent>

    @Query("""
        SELECT * FROM block_events
        WHERE userId = :userId
          AND syncedAt IS NULL
        ORDER BY triggeredAt ASC
    """)
    suspend fun getUnsyncedBlockEvents(userId: String): List<BlockEvent>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicates(event: BlockEvent): Long

    @Query("""
        UPDATE block_events
        SET syncedAt = :syncedAt, firebaseId = :firebaseId
        WHERE id = :eventId
    """)
    suspend fun markSynced(eventId: Long, syncedAt: Long, firebaseId: String)

    @Insert
    suspend fun insert(event: BlockEvent): Long

    @Query("SELECT COUNT(*) FROM block_events WHERE userId = :userId")
    suspend fun countAll(userId: String): Int

    @Query(
        "SELECT * FROM block_events WHERE userId = :userId " +
                "AND triggeredAt BETWEEN :startMillis AND :endMillis ORDER BY triggeredAt DESC"
    )
    suspend fun getBetween(userId: String, startMillis: Long, endMillis: Long): List<BlockEvent>

    @Query(
        "SELECT COUNT(*) FROM block_events WHERE userId = :userId " +
                "AND triggeredAt BETWEEN :startMillis AND :endMillis"
    )
    suspend fun countBetween(userId: String, startMillis: Long, endMillis: Long): Int

    @Query("SELECT triggeredAt FROM block_events WHERE userId = :userId ORDER BY triggeredAt ASC")
    suspend fun getAllTriggeredTimestamps(userId: String): List<Long>
}