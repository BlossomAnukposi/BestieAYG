package com.bayg.services.storage.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bayg.services.storage.entities.DailyUsage

@Dao
interface DailyUsageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dailyUsage: DailyUsage)

    @Update
    suspend fun update(dailyUsage: DailyUsage)

    @Query("SELECT * FROM daily_usage WHERE userId = :userId AND date = :date LIMIT 1")
    suspend fun getByDate(userId: Long, date: String): DailyUsage?

    /**
     * Returns rows between two date strings (inclusive), ordered oldest first.
     * Dates are "yyyy-MM-dd" so lexicographic ordering == chronological ordering.
     */
    @Query(
        "SELECT * FROM daily_usage WHERE userId = :userId " +
            "AND date BETWEEN :startDate AND :endDate ORDER BY date ASC"
    )
    suspend fun getBetween(userId: Long, startDate: String, endDate: String): List<DailyUsage>

    @Query("SELECT * FROM daily_usage WHERE userId = :userId ORDER BY date ASC")
    suspend fun getAll(userId: Long): List<DailyUsage>
}
