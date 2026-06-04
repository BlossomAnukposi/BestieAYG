package com.bayg.services.storage.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bayg.services.storage.entities.Streak
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {
    @Query("SELECT * FROM streaks WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: String): Streak?

    @Query("SELECT * FROM streaks WHERE userId = :userId LIMIT 1")
    fun getAllStreaks(userId: String): Flow<Streak?>

    /**
     * IMPORTANT! Make sure you call SyncWorker.runOnce everytime you have made an insert or update
     * call from the Room Dao. This reduces the chances of abusers taking advantage of
     * sync time
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(streak: Streak): Long

    /**
     * IMPORTANT! Make sure you call SyncWorker.runOnce everytime you have made an insert or update
     * call from the Room Dao. This reduces the chances of abusers taking advantage of
     * sync time
     */
    @Update
    suspend fun update(streak: Streak)

    /**
     * IMPORTANT! Make sure you call SyncWorker.runOnce everytime you have made an insert or update
     * call from the Room Dao. This reduces the chances of abusers taking advantage of
     * sync time
     */
    @Query("UPDATE streaks SET currentStreak = 0, lastStreakDate = NULL WHERE id = :id")
    suspend fun resetStreak(id: Long)
}
