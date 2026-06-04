package com.bayg.services.storage.daos

import androidx.room.*
import com.bayg.services.storage.entities.Streak
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {
    @Query("SELECT * FROM streaks WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: String): Streak?

    @Query("SELECT * FROM streaks WHERE userId = :userId LIMIT 1")
    fun getAllStreaks(userId: String): Flow<Streak?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(streak: Streak): Long

    @Update
    suspend fun update(streak: Streak)

    @Query("UPDATE streaks SET currentStreak = 0, lastStreakDate = NULL WHERE id = :id")
    suspend fun resetStreak(id: Long)
}
