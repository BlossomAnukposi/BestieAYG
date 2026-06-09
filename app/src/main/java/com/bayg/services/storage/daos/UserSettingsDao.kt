package com.bayg.services.storage.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bayg.services.storage.entities.UserSettings

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    suspend fun getByUserId(userId: Long): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: UserSettings): Long

    @Update
    suspend fun update(settings: UserSettings)
}