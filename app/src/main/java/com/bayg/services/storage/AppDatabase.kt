package com.bayg.services.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bayg.services.storage.daos.BlockEventDao
import com.bayg.services.storage.daos.StreakDao
import com.bayg.services.storage.daos.UserDao
import com.bayg.services.storage.daos.UserSettingsDao
import com.bayg.services.storage.entities.BlockEvent
import com.bayg.services.storage.entities.Streak
import com.bayg.services.storage.entities.User
import com.bayg.services.storage.entities.UserSettings

@Database(
    entities = [
        User::class,
        UserSettings::class,
        BlockEvent::class,
        Streak::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun blockEventDao(): BlockEventDao
    abstract fun streakDao(): StreakDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bayg.db"
                )
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
